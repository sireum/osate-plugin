package org.sireum.aadl.osate.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.runner.RunWith;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.errormodel.tests.ErrorModelInjectorProvider;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.testsupport.TestResourceSetHelper;
import org.sireum.aadl.osate.tests.util.UpdaterUtil;
import org.sireum.aadl.osate.tests.util.UpdaterUtil.TestAadlProject;
import org.sireum.aadl.osate.tests.util.UpdaterUtil.TestAadlSystem;

import com.google.inject.Inject;
import com.itemis.xtext.testing.XtextTest;

@RunWith(XtextRunner.class)
//@InjectWith(Aadl2InjectorProvider.class)
@InjectWith(ErrorModelInjectorProvider.class)
public abstract class SireumTest extends XtextTest {
	@Inject
	TestResourceSetHelper rsHelper;

	protected SystemInstance getSystemInstance(TestAadlSystem system) {
		try {
			ResourceSet rset = createResourceSet(system.projects);

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

	ResourceSet createResourceSet(List<TestAadlProject> projects) {

		for (TestAadlProject project : projects) {
			// pretend the files are organized in a project called <projectName> so that URIs in
			// AIR Positions are relative to the project, not the file system
			// https://github.com/osate/osate2/wiki/Using-contributed-resources-in-stand-alone-applications/d5e28542e20b531b8688ab962aa08606d5e619a8
			String wsRoot = project.rootDirectory.getAbsolutePath();
			EcorePlugin.getPlatformResourceMap().put(project.projectName, URI.createFileURI(wsRoot));
		}

		ResourceSet rs = rsHelper.getResourceSet();
		for (TestAadlProject project : projects) {
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
	Resource loadFile(TestAadlProject project, File file, ResourceSet rs) {
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
