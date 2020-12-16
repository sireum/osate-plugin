package org.sireum.aadl.osate.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import org.sireum.aadl.osate.cli.UpdaterUtil.TestAadlProject;
import org.sireum.aadl.osate.cli.UpdaterUtil.TestAadlSystem;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SerializerType;

import com.google.inject.Injector;

@SuppressWarnings("restriction")
public class GenerateAir implements IApplication {

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

			if (appArgs.length < 1 || appArgs.length > 2) {
				printUsage();
				return IApplication.EXIT_OK;
			}

			File root = new File(appArgs[0]);

			if (!root.exists() || !root.isDirectory()) {
				System.err.println(root + " is not a directory\n");
				printUsage();
				return IApplication.EXIT_OK;
			}

			File outputDir = appArgs.length == 2 ? new File(appArgs[1]) : null;

			// use air hamr test utils to load the system
			List<TestAadlSystem> systems = UpdaterUtil.findSystems(root);

			if (systems.size() != 1) {
				System.err.println("Found " + systems.size() + " AADL projects. "
						+ "Point to a directory that contains a single .project file "
						+ "or a .system file\n");
				printUsage();
				return IApplication.EXIT_OK;
			}

			TestAadlSystem system = systems.get(0);

			if (outputDir != null) {
				system.slangOutputDir = outputDir;
			}

			populateResourceSet(system.projects, resourceSet);

			genAIR(system, resourceSet);

		} else {
			System.out.println("Failed! Unable to create resource set");
		}
		return IApplication.EXIT_OK;
	}

	void printUsage() {
		System.out.println(//
				"AIR HAMR CLI\n\n" + //
						"Usage: <path> <output-directory>?");
	}

	void genAIR(org.sireum.aadl.osate.cli.UpdaterUtil.TestAadlSystem system, ResourceSet resourceSet) {

		System.out.println("Processing: " + system.systemImplementationName);

		SystemInstance instance = getSystemInstance(system, resourceSet);
		assert instance != null : "System is null " + system.systemImplementationName;

		String air = Util.serialize(Util.getAir(instance, true), SerializerType.JSON);

		String instanceFilename = Util.toIFile(instance.eResource().getURI()).getName();
		String fname = instanceFilename.substring(0, instanceFilename.lastIndexOf(".")) + ".json";

		File outFile = new File(system.slangOutputDir, fname);
		IOUtils.writeFile(outFile, air);
		IOUtils.zipFile(outFile);
	}

	SystemInstance getSystemInstance(TestAadlSystem system, ResourceSet rset) {
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

				SystemImplementation sysImpl = (SystemImplementation) UpdaterUtil.getResourceByName(
						system.systemImplementationName, pkg.getOwnedPublicSection().getOwnedClassifiers());

				return InstantiateModel.instantiate(sysImpl);
			} else {
				throw new RuntimeException("Couldn't find resource " + system.systemImplementationFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(1);
		return null;
	}

	ResourceSet populateResourceSet(List<TestAadlProject> projects, ResourceSet rs) {

		for (TestAadlProject project : projects) {
			// pretend the files are organized in a project called <projectName> so that URIs in
			// AIR Positions are relative to the project, not the file system
			// https://github.com/osate/osate2/wiki/Using-contributed-resources-in-stand-alone-applications/d5e28542e20b531b8688ab962aa08606d5e619a8
			String wsRoot = project.rootDirectory.getAbsolutePath();
			EcorePlugin.getPlatformResourceMap().put(project.projectName, URI.createFileURI(wsRoot));
		}

		// ResourceSet rs = rsHelper.getResourceSet();
		for (TestAadlProject project : projects) {
			for (File f : project.aadlFiles) {
				loadFile(project, f, rs);
			}
		}

		for (Resource resource : rs.getResources()) {
			IResourceValidator validator = ((XtextResource) resource).getResourceServiceProvider()
					.getResourceValidator();
			List<Issue> issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);

			if (!issues.isEmpty()) {
				System.out.println("Issues detected for: " + resource);
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
	public Resource loadFile(TestAadlProject project, File file, ResourceSet rs) {
		try {
			URL url = new URL("file:" + file.getAbsolutePath());
			InputStream stream = url.openConnection().getInputStream();

			String prefix = "platform:/resource/";

			Path rootPath = Path.of(project.rootDirectory.toURI());
			Path resourcePath = Path.of(file.toURI());
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
}
