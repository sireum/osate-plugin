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
import org.sireum.Cli.HelpOption;
//import org.sireum.Cli.PhantomMode.Type;
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

import com.google.inject.Injector;

@SuppressWarnings("restriction")
public class GenerateAir implements IApplication {

	org.sireum.Z z(int i) {
		return org.sireum.Z.apply(i);
	}
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

			Option<PhantomOption> _phantomOptions = getOptions(appArgs);

			if (_phantomOptions.isEmpty() || _phantomOptions.get().getArgs().size().toInt() > 1) {
				printUsage();
				return IApplication.EXIT_OK;
			}

			PhantomOption po = _phantomOptions.get();

			if(po.args().nonEmpty() == (po.getProjects().nonEmpty() && po.getMain().nonEmpty() && po.getImpl().nonEmpty())) {
				addError("Either point to a directory or supply the required options\n");
				printUsage();
				return IApplication.EXIT_OK;
			}

			boolean userProvided = po.getArgs().isEmpty();

			AadlSystem system = null;

			if (userProvided) {
				File mainPackageFile = new File(po.getMain().get().string());

				List<File> projRoots = VisitorUtil.isz2IList(po.getProjects()).stream().map(m -> new File(m.string()))
						.collect(Collectors.toList());
				for (File projectRoot : projRoots) {
					if(!projectRoot.exists() || !projectRoot.isDirectory()) {
						addError(projectRoot + " does not exist or is not a directory");
						return IApplication.EXIT_OK;
					}
				}

				List<AadlProject> projects = projRoots.stream().map(m -> AadlProjectUtil.createTestAadlProject(m))
						.collect(Collectors.toList());

				String sysImplName = po.getImpl().get().string();

				system = new AadlSystem(sysImplName, mainPackageFile, projects);

			} else {

				File root = new File(po.getArgs().apply(z(0)).string());

				if (!root.exists() || !root.isDirectory()) {
					addError(root + " is not a directory\n");
					printUsage();
					return IApplication.EXIT_OK;
				}

				// use air hamr test utils to load the system
				List<AadlSystem> systems = AadlProjectUtil.findSystems(root);

				if (systems.size() != 1) {
					addError("Found " + systems.size() + " AADL projects. "
							+ "Point to a directory that contains a single .project file " + "or a .system file\n");
					printUsage();
					return IApplication.EXIT_OK;
				}
				system = systems.get(0);
			}

			// Slang enum's type def nested inside a Slang object don't seem to be
			// accessible via Eclipse jdt
			boolean toJson = po.getMode().name().equals("Json");

			File outputFile = po.getOutput().nonEmpty() ? new File(po.getOutput().get().string()) : null;
			if (outputFile != null) {
				system.slangOutputFile = outputFile;
			}

			populateResourceSet(system.projects, resourceSet);

			org.sireum.aadl.osate.PreferenceValues.setPROCESS_BA_OPT(true);

			genAIR(system, resourceSet, toJson);

		} else {
			addError("Failed! Unable to create resource set");
		}
		return IApplication.EXIT_OK;
	}

	void genAIR(AadlSystem system, ResourceSet resourceSet, boolean toJson) {

		addInfo("Processing: " + system.systemImplementationName);

		SystemInstance instance = getSystemInstance(system, resourceSet);
		if (instance == null) {
			return;
		}

		SerializerType st = toJson ? SerializerType.JSON : SerializerType.MSG_PACK;
		String ext = toJson ? ".json" : ".msgpack";

		String air = Util.serialize(Util.getAir(instance, true), st);

		File outputFile = system.slangOutputFile;
		if (outputFile == null) {
			String instanceFilename = Util.toIFile(instance.eResource().getURI()).getName();
			String fname = instanceFilename.substring(0, instanceFilename.lastIndexOf(".")) + ext;

			File slangDir = new File(system.projects.get(0).rootDirectory, ".slang");
			outputFile = new File(slangDir, fname);
		}

		IOUtils.writeFile(outputFile, air);

		// IOUtils.zipFile(outFile);
	}

	SystemInstance getSystemInstance(AadlSystem system, ResourceSet rset) {
		try {
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
					System.err.println(issue.getMessage());
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
				res.load(stream, Collections.EMPTY_MAP);
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

	private Option<PhantomOption> getOptions(String... appArgs) {
		// convert java strings to sireum strings
		List<org.sireum.String> sStrings = Arrays.asList(appArgs).stream().map(s -> new org.sireum.String(s))
				.collect(Collectors.toList());

		Option<SireumTopOption> opts = org.sireum.Cli$.MODULE$.apply(File.pathSeparatorChar)
				.parsePhantom(VisitorUtil.toISZ(sStrings), z(0));

		if (opts.isEmpty() || opts.get() instanceof HelpOption) {
			return org.sireum.None$.MODULE$.apply();
		} else if (opts.get() instanceof PhantomOption) {
			PhantomOption po = (PhantomOption) opts.get();

			if (po.getOsate().nonEmpty()) {
				throw new RuntimeException("Not expecting Phantom 'osate' option (ie. you're already in OSATE)");
			}

			return org.sireum.Some$.MODULE$.apply(po);
		} else {
			throw new RuntimeException("Unexpected: received" + opts.get());
		}
	}

	void printUsage() {
		// This will cause sireum to print the help text for phantom
		getOptions("-h");
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
