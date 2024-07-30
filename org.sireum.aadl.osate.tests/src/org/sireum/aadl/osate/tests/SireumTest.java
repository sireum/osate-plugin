package org.sireum.aadl.osate.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.errormodel.tests.ErrorModelInjectorProvider;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.testsupport.TestResourceSetHelper;
import org.sireum.aadl.osate.util.AadlProjectUtil;
import org.sireum.aadl.osate.util.AadlProjectUtil.AadlProject;
import org.sireum.aadl.osate.util.AadlProjectUtil.AadlSystem;
import org.sireum.aadl.osate.util.IOUtil;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SerializerType;
import org.sireum.aadl.osate.util.VisitorUtil;
import org.sireum.hamr.ir.Aadl;
import org.sireum.message.Reporter;

import com.google.inject.Inject;
import com.itemis.xtext.testing.XtextTest;

@RunWith(XtextRunner.class)
//@InjectWith(Aadl2InjectorProvider.class)
@InjectWith(ErrorModelInjectorProvider.class)
public abstract class SireumTest extends XtextTest {
	@Inject
	TestResourceSetHelper rsHelper;

	public boolean writeResults = true;
	public boolean generateExpected = false;

	public void execute(File modelDir, String sysFilename, String sysImplName) {
		try {
			File root = modelDir;

			List<File> aadlFiles = IOUtil.collectFiles(root, ".aadl", true);
			File sysImplFile = new File(root, sysFilename);
			assert sysImplFile.exists() : sysImplFile.getAbsolutePath() + "doesn't exist";

			AadlProject project = new AadlProject(root.getName(), root, aadlFiles);

			AadlSystem system = AadlSystem.makeAadlSystem(sysImplName, Optional.of(sysImplFile), Arrays.asList(project),
					null);

			Reporter reporter = Util.createReporter();

			SystemInstance instance = getSystemInstance(system, reporter);

			if (reporter.hasError()) {
				reporter.printMessages();
				Assert.assertTrue("Reporter has errors", !reporter.hasError());
			}

			Aadl model = Util.getAir(instance, true, reporter, System.out);

			if (reporter.hasError()) {
				reporter.printMessages();
				Assert.assertTrue("Reporter has errors", !reporter.hasError());
			}

			String ir = Util.serialize(model, SerializerType.JSON);

			if (writeResults) {
				File results = new File(root, sysImplName + "_results.json");
				IOUtil.writeFile(results, ir);
			}

			File expectedFile = new File(root, sysImplName + ".json");
			String expected = null;
			if (generateExpected) {
				IOUtil.writeFile(expectedFile, ir);
				expected = ir;
			} else {
				Assert.assertTrue("Expected results not found: " + expectedFile.getCanonicalPath(),
						expectedFile.exists());
				expected = IOUtil.readFile(expectedFile);
			}

			Assert.assertEquals(expected, ir);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.assertFalse(true);
		}
	}

	protected SystemInstance getSystemInstance(AadlSystem system, Reporter reporter) {
		try {
			ResourceSet rset = createResourceSet(system.projects);

			Resource sysImplResource = null;
			String candURI = "platform:/resource/" + system.systemFileContainer.get().proj.projectName + "/"
					+ system.systemFileContainer.get().projectRelativePath;

			VisitorUtil.translateMessages(rset, "SireumTest", reporter);

			if (reporter.hasError()) {
				return null;
			}

			for (Resource rs : rset.getResources()) {
				if (rs.getURI().toString().equals(candURI)) {
					sysImplResource = rs;
					break;
				}
			}

			if (sysImplResource != null) {
				final AadlPackage pkg = (AadlPackage) (sysImplResource.getContents().isEmpty() ? null
						: sysImplResource.getContents().get(0));

				SystemImplementation sysImpl = (SystemImplementation) AadlProjectUtil.getResourceByName(
						system.systemImplementationName, pkg.getOwnedPublicSection().getOwnedClassifiers());

				return InstantiateModel.instantiate(sysImpl);
			} else {
				throw new RuntimeException(
						"Couldn't find resource " + system.systemFileContainer.get().systemImplementationFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(1);
		return null;
	}

	ResourceSet createResourceSet(List<AadlProject> projects) {

		for (AadlProject project : projects) {
			// pretend the files are organized in a project called <projectName> so that URIs in
			// AIR Positions are relative to the project, not the file system
			// https://github.com/osate/osate2/wiki/Using-contributed-resources-in-stand-alone-applications/d5e28542e20b531b8688ab962aa08606d5e619a8
			String wsRoot = project.rootDirectory.getAbsolutePath();
			EcorePlugin.getPlatformResourceMap().put(project.projectName, URI.createFileURI(wsRoot));
		}

		ResourceSet rs = rsHelper.getResourceSet();
		for (AadlProject project : projects) {
			for (File f : project.aadlFiles) {
				loadFile(project, f, rs);
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
	Resource loadFile(AadlProject project, File file, ResourceSet rs) {
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
}
