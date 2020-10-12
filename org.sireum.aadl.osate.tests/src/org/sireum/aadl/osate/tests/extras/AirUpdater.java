package org.sireum.aadl.osate.tests.extras;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.errormodel.tests.ErrorModelInjectorProvider;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.testsupport.TestResourceSetHelper;
import org.sireum.aadl.osate.tests.extras.UpdaterUtil.TestAadlProject;
import org.sireum.aadl.osate.tests.extras.UpdaterUtil.TestAadlSystem;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SerializerType;

import com.google.inject.Inject;
import com.itemis.xtext.testing.XtextTest;

@RunWith(XtextRunner.class)
@InjectWith(ErrorModelInjectorProvider.class)
public class AirUpdater extends XtextTest {
	@Inject
	TestResourceSetHelper rsHelper;

	@Test
	public void updateAirHamr() {
		File hamrModelsDir = new File(System.getenv("SIREUM_HOME") + "/hamr/codegen/jvm/src/test/scala/models");

		if (hamrModelsDir.exists()) {
			for (TestAadlSystem system : UpdaterUtil.findSystems(hamrModelsDir)) {
				regen(system);
			}
		} else {
			System.out.println("Directory does not exist: " + hamrModelsDir);
		}
	}

	@Test
	public void updateAirHamr2() {

		File hamrModelsDir = new File("/home/vagrant/devel/case/CASE-loonwerks/TA5/tool-evaluation-4/HAMR/examples");
		if (hamrModelsDir.exists()) {
			for(TestAadlSystem system : UpdaterUtil.findSystems(hamrModelsDir)) {
				regen(system);
			}
		} else {
			System.out.println("Directory does not exist: " + hamrModelsDir);
		}

	}

	void regen(TestAadlSystem system) {

		SystemInstance instance = getSystemInstance(system);
		assert instance != null : "System is null " + system.systemImplementationName;

		String air = Util.serialize(Util.getAir(instance, true), SerializerType.JSON);

		String instanceFilename = Util.toIFile(instance.eResource().getURI()).getName();
		String fname = instanceFilename.substring(0, instanceFilename.lastIndexOf(".")) + ".json";

		File outFile = new File(system.slangOutputDir, fname);
		IOUtils.writeFile(outFile, air);
		IOUtils.zipFile(outFile);
	}

	SystemInstance getSystemInstance(TestAadlSystem system) {
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
				final AadlPackage pkg = (AadlPackage) (sysImplResource.getContents().isEmpty() ? null : sysImplResource.getContents().get(0));

				SystemImplementation sysImpl = (SystemImplementation) UpdaterUtil.getResourceByName(system.systemImplementationName,
						pkg.getOwnedPublicSection().getOwnedClassifiers());

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
				loadFile(project.projectName, f, rs);
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
	public Resource loadFile(String projectName, File file, ResourceSet rs) {
		try {
			URL url = new URL("file:" + file.getAbsolutePath());
			InputStream stream = url.openConnection().getInputStream();
			Resource res = rs.createResource(URI.createURI(projectName + "/" + file.getName()));
			if (res != null) {
				res.load(stream, Collections.EMPTY_MAP);
			}
			return res;
		} catch (IOException e) {
			return null;
		}
	}
}
