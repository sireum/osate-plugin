package org.sireum.aadl.osate.tests.extras;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osate.aadl2.errormodel.tests.ErrorModelInjectorProvider;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.testsupport.TestResourceSetHelper;
import org.sireum.Os;
import org.sireum.Os.Path;
import org.sireum.aadl.osate.architecture.VisitorUtil;
import org.sireum.aadl.osate.tests.SireumTest;
import org.sireum.aadl.osate.util.AadlProjectUtil;
import org.sireum.aadl.osate.util.AadlProjectUtil.AadlSystem;
import org.sireum.aadl.osate.util.IOUtils;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SerializerType;

import com.google.inject.Inject;

@RunWith(XtextRunner.class)
@InjectWith(ErrorModelInjectorProvider.class)
public class AirUpdater extends SireumTest {
	@Inject
	TestResourceSetHelper rsHelper;

	@Test
	public void updateAirHamr() {
		List<File> hamrModelsDirs = VisitorUtil.toIList(
				new File(System.getenv("SIREUM_HOME") + "/hamr/codegen/jvm/src/test/scala/models"),
				new File(System.getenv("SIREUM_HOME") + "/hamr/codegen/arsit/jvm/src/test/scala/models"));

		for (File hamrModelsDir : hamrModelsDirs) {
			if (hamrModelsDir.exists()) {
				for (AadlSystem system : AadlProjectUtil.findSystems(hamrModelsDir)) {
					regen(system);
				}
			} else {
				System.out.println("Directory does not exist: " + hamrModelsDir);
			}
		}
	}

	@Test
	public void updateAirHamr2() {

		File hamrModelsDir = new File("/home/vagrant/devel/case/case-loonwerks/TA5/tool-evaluation-4/HAMR/examples");
		if (hamrModelsDir.exists()) {
			for (AadlSystem system : AadlProjectUtil.findSystems(hamrModelsDir)) {
				regen(system);
			}
		} else {
			System.out.println("Directory does not exist: " + hamrModelsDir);
		}
	}

	@Test
	public void syncGumbo() throws IOException {
		Path srcPath = Os.path("./projects/org/sireum/aadl/osate/tests/gumbo");
		Path destPath = Os.path(System.getenv("SIREUM_HOME") + "/hamr/codegen/jvm/src/test/scala/models/GumboTest");

		srcPath.copyOverTo(destPath);

		for (AadlSystem system : AadlProjectUtil.findSystems(new File(destPath.canon().value()))) {
			regen(system);
		}
	}

	void regen(AadlSystem system) {

		SystemInstance instance = getSystemInstance(system);
		assert instance != null : "System is null " + system.systemImplementationName;

		String air = Util.serialize(Util.getAir(instance, true), SerializerType.JSON);

		String instanceFilename = Util.toIFile(instance.eResource().getURI()).getName();
		String fname = instanceFilename.substring(0, instanceFilename.lastIndexOf(".")) + ".json";

		File slangDir = new File(system.projects.get(0).rootDirectory, File.separator + ".slang");
		assert slangDir.exists() && slangDir.isDirectory() : slangDir + " does not exist";

		File outFile = new File(slangDir, fname);
		IOUtils.writeFile(outFile, air);
		IOUtils.zipFile(outFile);
	}
}
