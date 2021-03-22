package org.sireum.aadl.osate.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.instance.InstancePackage;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.util.Aadl2ResourceFactoryImpl;
import org.osate.pluginsupport.PluginSupportUtil;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;
import org.osate.xtext.aadl2.errormodel.ErrorModelStandaloneSetup;
import org.sireum.Cli.HamrCodeGenOption;
import org.sireum.Cli.HelpOption;
import org.sireum.Cli.PhantomOption;
import org.sireum.Cli.SireumTopOption;
import org.sireum.Option;
import org.sireum.aadl.osate.architecture.VisitorUtil;
import org.sireum.aadl.osate.util.AadlProjectUtil;
import org.sireum.aadl.osate.util.AadlProjectUtil.AadlProject;
import org.sireum.aadl.osate.util.AadlProjectUtil.AadlSystem;
import org.sireum.aadl.osate.util.IOUtils;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SerializerType;
import org.sireum.hamr.ir.Aadl;

import com.google.inject.Injector;

@SuppressWarnings("restriction")
public class Phantom implements IApplication {

	/**
	 * based on instructions from
	 *
	 * https://github.com/osate/osate2/wiki/Using-annex-extensions-in-stand-alone-applications
	 *
	 * and examples contained in
	 *
	 * https://github.com/osate/osate2/tree/1388_stand_alone_property_sets/standalone_tests
	 *
	 */
	@Override
	public Object start(IApplicationContext context) throws Exception {
		context.applicationRunning();

		// Read the meta information about the plug-ins to get the annex information.
		// may be slow
		EcorePlugin.ExtensionProcessor.process(null);

		// or load the annexes individually

		// Add the EMV2 annex handling
		// AnnexRegistry.registerAnnex("EMV2", new EMV2AnnexParser(), new EMV2AnnexUnparser(),
		// new EMV2AnnexLinkingService(), null, null, null, null, null);

		final Injector injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();

		// important that this comes next, otherwise emv2 libraries won't resolve
		// see https://github.com/osate/osate2/issues/1387#issuecomment-483739761
		ErrorModelStandaloneSetup.doSetup();

		if (Platform.getBundle("com.rockwellcollins.atc.resolute") != null) {
			//addInfo("Setting up Resolute");
			// ResoluteStandaloneSetup.doSetup();
			reflectDoSetup("com.rockwellcollins.atc.resolute.ResoluteStandaloneSetup");
		}

		if (Platform.getBundle("com.rockwellcollins.atc.agree") != null) {
			//addInfo("Setting up AGREE");
			// AgreeStandaloneSetup.doSetup();
			reflectDoSetup("com.rockwellcollins.atc.agree.AgreeStandaloneSetup");
		}

		// Init Instance model -- need both these lines
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("aaxl2", new Aadl2ResourceFactoryImpl());
		InstancePackage.eINSTANCE.eClass();

		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);

		if (resourceSet != null) {

			// Add predeclared resources
			List<URI> contributed = PluginSupportUtil.getContributedAadl();
			for (URI uri : contributed) {
				resourceSet.getResource(uri, true);
			}

			final Map<?, ?> args = context.getArguments();
			final String[] appArgs = (String[]) args.get("application.args");

			Option<SireumTopOption> opts = getOptions(appArgs);

			if (opts.isEmpty() || opts.get() instanceof HelpOption) {
				// printUsage();
				return IApplication.EXIT_OK;
			}

			if (opts.get() instanceof PhantomOption) {
				return phantom((PhantomOption) opts.get(), resourceSet);
			} else if (opts.get() instanceof HamrCodeGenOption) {
				return hamrCodegen((HamrCodeGenOption) opts.get(), resourceSet);
			} else {
				org.sireum.Sireum$.MODULE$.main(appArgs);

				return IApplication.EXIT_OK;
			}

		} else {
			addError("Failed! Unable to create resource set");
		}
		return IApplication.EXIT_OK;
	}

	private void reflectDoSetup(String className) {
		try {
			Class.forName(className).getMethod("doSetup").invoke(null);
		} catch (Exception e) {
			addError("Issue invoking " + className + ".doSetup()");
		}
	}

	int phantom(PhantomOption po, ResourceSet rs) {

		if (po.args()
				.nonEmpty() == (po.getProjects().nonEmpty() && po.getMain().nonEmpty() && po.getImpl().nonEmpty())) {
			addError("Either point to a directory or supply the required options\n");
			// printUsage();
			return IApplication.EXIT_OK;
		}

		boolean userProvided = po.getArgs().isEmpty();

		AadlSystem system = null;

		if (userProvided) {
			File mainPackageFile = new File(po.getMain().get().string());

			List<File> projRoots = VisitorUtil.isz2IList(po.getProjects()).stream().map(m -> new File(m.string()))
					.collect(Collectors.toList());
			for (File projectRoot : projRoots) {
				if (!projectRoot.exists() || !projectRoot.isDirectory()) {
					addError(projectRoot + " does not exist or is not a directory");
					return IApplication.EXIT_OK;
				}
			}

			List<AadlProject> projects = projRoots.stream().map(m -> AadlProjectUtil.createTestAadlProject(m))
					.collect(Collectors.toList());

			String sysImplName = po.getImpl().get().string();

			system = new AadlSystem(sysImplName, mainPackageFile, projects);

		} else {

			File root = new File(po.getArgs().apply(z(0)).string()).getAbsoluteFile();

			if (!root.exists() || !root.isDirectory()) {
				addError(root + " is not a directory\n");
				// printUsage();
				return IApplication.EXIT_OK;
			}

			List<AadlSystem> systems = AadlProjectUtil.findSystems(root);

			if (systems.size() != 1) {
				addError("Found " + systems.size() + " AADL projects. "
						+ "Point to a directory that contains a single .project file " + "or a .system file\n");
				// printUsage();
				return IApplication.EXIT_OK;
			}
			system = systems.get(0);
		}

		// Slang enum's type def nested inside a Slang object don't seem to be
		// accessible via Eclipse jdt
		boolean toJson = po.getMode().name().equals("Json");

		File outputFile = po.getOutput().nonEmpty() ? new File(po.getOutput().get().string()) : null;

		SerializerType st = toJson ? SerializerType.JSON : SerializerType.MSG_PACK;
		String ext = toJson ? ".json" : ".msgpack";

		SystemInstance instance = getSystemInstance(system, rs);
		Aadl model = Util.getAir(instance, true);

		if (model != null) {
			String air = Util.serialize(model, st);

			if (outputFile == null) {
				String instanceFilename = Util.toIFile(instance.eResource().getURI()).getName();
				String fname = instanceFilename.substring(0, instanceFilename.lastIndexOf(".")) + ext;

				File slangDir = new File(system.projects.get(0).rootDirectory, ".slang");
				outputFile = new File(slangDir, fname);
			}

			IOUtils.writeFile(outputFile, air);

			// IOUtils.zipFile(outFile);

			return IApplication.EXIT_OK;
		} else {
			addError("Could not generate AIR");
			return IApplication.EXIT_OK;
		}
	}

	int hamrCodegen(HamrCodeGenOption ho, ResourceSet rs) {

		if (ho.args().size().toInt() != 1) {
			addError("Expecting exactly one argument");
			return IApplication.EXIT_OK;
		}

		String s = ho.args().apply(z(0)).string();

		File f = new File(s).getAbsoluteFile();
		if (!f.exists() || !f.isFile()) {
			addError("Either point to a serialized AIR file, or to a .project or .system file");
			return IApplication.EXIT_OK;
		}

		int ret = 0;
		if (f.getName().equals(".project") || f.getName().startsWith(".system")) {
			List<AadlSystem> systems = AadlProjectUtil.findSystems(f.getParentFile());

			if (systems.size() != 1) {
				addError("Found " + systems.size() + " AADL projects. " + "Point to a single .project file "
						+ "or a .system file\n");
				// printUsage();
				return IApplication.EXIT_OK;
			}

			AadlSystem system = systems.get(0);
			Aadl model = Util.getAir(getSystemInstance(system, rs), true);

			ret = org.sireum.cli.HAMR.codeGen(model, ho);
		} else {
			// assume it's a serialized AIR file
			ret = org.sireum.cli.HAMR.codeGen(ho);
		}

		addInfo("HAMR Codegen was " + ((ret != 0) ? "un" : "") + "succesful");

		return IApplication.EXIT_OK;
	}

	SystemInstance getSystemInstance(AadlSystem system, ResourceSet rset) {
		try {
			populateResourceSet(system.projects, rset);

			org.sireum.aadl.osate.PreferenceValues.setPROCESS_BA_OPT(true);

			addInfo("Processing: " + system.systemImplementationName);

			Resource sysImplResource = null;
			// TODO: determine correct way of getting the OSATE URI for the system impl file
			for (Resource rs : rset.getResources()) {
				String filename = rs.getURI().lastSegment();
				if (filename.equals(system.systemImplementationFile.getName())) {
					sysImplResource = rs;
				}
			}

			if (sysImplResource != null) {
				final AadlPackage pkg = (AadlPackage) (sysImplResource.getContents().isEmpty() ? null
						: sysImplResource.getContents().get(0));

				SystemImplementation sysImpl = (SystemImplementation) AadlProjectUtil.getResourceByName(
						system.systemImplementationName, pkg.getOwnedPublicSection().getOwnedClassifiers());

				if (sysImpl != null) {
					return InstantiateModel.instantiate(sysImpl);
				} else {
					addError("Unable to find system implementation " + system.systemImplementationName);
				}
			} else {
				addError("Unable to find resource " + system.systemImplementationFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	ResourceSet populateResourceSet(List<AadlProject> projects, ResourceSet rs) {

		for (AadlProject project : projects) {
			// pretend the files are organized in a project called <projectName> so that URIs in
			// AIR Positions are relative to the project, not the file system
			// https://github.com/osate/osate2/wiki/Using-contributed-resources-in-stand-alone-applications/d5e28542e20b531b8688ab962aa08606d5e619a8
			String wsRoot = project.rootDirectory.getAbsolutePath();
			EcorePlugin.getPlatformResourceMap().put(project.projectName, URI.createFileURI(wsRoot));
		}

		// ResourceSet rs = rsHelper.getResourceSet();
		for (AadlProject project : projects) {
			for (File f : project.aadlFiles) {
				loadFile(project, f, rs);
			}
		}

		for (Resource resource : rs.getResources()) {
			IResourceValidator validator = ((XtextResource) resource).getResourceServiceProvider()
					.getResourceValidator();
			List<Issue> issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);

			if (!issues.isEmpty()) {
				addError("Issues detected for: " + resource);
				for (Issue issue : issues) {
					String pos = "  [" + issue.getLineNumber() + "," + issue.getColumn() + "] ";
					System.err.println(pos + issue.getMessage());
				}
				System.err.println();
			}
		}

		return rs;
	}

	/** Adapted from
	 * <a href="https://github.com/osate/osate2/blob/44af9ff8d6309410aeb134a0ae825aa7c916fabf/core/org.osate.testsupport/src/org/osate/testsupport/TestHelper.java#L147">here</a>.
	 *
	 * load file as Xtext resource into resource set
	 * @param projectName root of paths relative to the resource set
	 * @param filePath String
	 * @param rs ResourceSet
	 * @return
	 */
	public Resource loadFile(AadlProject project, File file, ResourceSet rs) {
		try {
			URL url = new URL("file:" + file.getAbsolutePath());
			InputStream stream = url.openConnection().getInputStream();

			String prefix = "platform:/resource/";

			Path rootPath = Paths.get(project.rootDirectory.toURI());
			Path resourcePath = Paths.get(file.toURI());
			Path relativePath = rootPath.relativize(resourcePath);

			// came up with this uri by comparing what OSATE IDE serialized AIR produces
			URI resourceUri = URI.createURI(prefix + project.projectName + "/" + relativePath);

			/*
			 * System.out.println("root = " + rootPath);
			 * System.out.println("resourcePath = " + resourcePath);
			 * System.out.println("relative = " + relativePath);
			 * System.out.println("resourceUri = " + resourceUri);
			 * System.out.println();
			 */
			Resource res = rs.createResource(resourceUri);

			if (res != null) {
				// addInfo("Created resource: " + resourceUri);
				res.load(stream, Collections.EMPTY_MAP);
			} else {
				addError("Resource creation resulted in null for: " + resourceUri);
			}
			return res;
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}

	private Option<SireumTopOption> getOptions(String... appArgs) {
		// convert java strings to sireum strings
		List<org.sireum.String> sStrings = Arrays.asList(appArgs).stream().map(s -> new org.sireum.String(s))
				.collect(Collectors.toList());

		return org.sireum.Cli$.MODULE$.apply(File.pathSeparatorChar)
				.parseSireum(VisitorUtil.toISZ(sStrings), z(0));
	}

	org.sireum.Z z(int i) {
		return org.sireum.Z.apply(i);
	}

	void printUsage() {
		getOptions();
	}

	void addInfo(String msg) {
		System.out.println(msg);
	}

	void addError(String msg) {
		System.err.println("Error: " + msg);
	}

	void addWarning(String msg) {
		System.out.println("Warning: " + msg);
	}

}
