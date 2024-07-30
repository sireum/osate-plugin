package org.sireum.aadl.osate.tests.extras;

import java.io.File;
import java.util.List;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osate.aadl2.errormodel.tests.ErrorModelInjectorProvider;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.testsupport.TestResourceSetHelper;
import org.sireum.aadl.osate.tests.SireumTest;
import org.sireum.aadl.osate.util.AadlProjectUtil;
import org.sireum.aadl.osate.util.AadlProjectUtil.AadlSystem;
import org.sireum.aadl.osate.util.IOUtil;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SerializerType;
import org.sireum.aadl.osate.util.VisitorUtil;
import org.sireum.hamr.ir.Aadl;
import org.sireum.message.Reporter;

import com.google.inject.Inject;

@RunWith(XtextRunner.class)
@InjectWith(ErrorModelInjectorProvider.class)
public class AirUpdater extends SireumTest {
	@Inject
	TestResourceSetHelper rsHelper;

	String SIREUM_HOME() {
		String SIREUM_HOME = System.getenv("SIREUM_HOME") != null ? System.getenv("SIREUM_HOME")
				: System.getenv("HOME") != null ? System.getenv("HOME") + "/devel/sireum/kekinian" : null;

		assert SIREUM_HOME != null && new File(SIREUM_HOME).exists()
				: SIREUM_HOME + " does not exist of isn't a directory";

		return SIREUM_HOME;
	}

	@Test
	public void updateAirHamr() {

		List<File> hamrModelsDirs = VisitorUtil.toIList(
				new File(SIREUM_HOME() + "/hamr/codegen/jvm/src/test/resources/models"), //
				new File(SIREUM_HOME() + "/hamr/codegen/arsit/jvm/src/test/scala/models"), //
				new File(SIREUM_HOME()
						+ "/hamr/codegen/jvm/src/test-ext/gumbo/resources/models/sirfur_omnibus/gumbo/git_models/temp_control/simple_temp_aadl"), //
				new File(SIREUM_HOME()
						+ "/hamr/codegen/jvm/src/test-ext/gumbo/resources/models/sirfur_omnibus/gumbo/git_models/temp_control/unit_temp_aadl"), //
				new File(SIREUM_HOME()
						+ "/hamr/codegen/jvm/src/test-ext/gumbo/resources/models/GumboAdventiumTest/simple_temp_aadl/aadl"));

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

	/*
	 * @Test
	 * public void syncGumbo() throws IOException {
	 * Path srcPath = Os.path("./projects/org/sireum/aadl/osate/tests/gumbo");
	 * Path destPath = Os.path(SIREUM_HOME() + "/hamr/codegen/jvm/src/test/resources/models/GumboTest");
	 *
	 * srcPath.copyOverTo(destPath);
	 *
	 * for (AadlSystem system : AadlProjectUtil.findSystems(new File(destPath.canon().value()))) {
	 * System.out.println("Processing: " + system.projects.get(0).projectName);
	 * regen(system);
	 * }
	 * }
	 */

	void regen(AadlSystem system) {

		Reporter reporter = Util.createReporter();

		SystemInstance instance = getSystemInstance(system, reporter);

		if (reporter.hasError()) {
			reporter.printMessages();
			assert false : "Reporter has errors";
		}

		assert instance != null : "System is null " + system.systemImplementationName;

		Aadl model = Util.getAir(instance, true, reporter);

		if (reporter.hasError()) {
			reporter.printMessages();
			assert false : "Reporter has errors";
		} else {
			String air = Util.serialize(model, SerializerType.JSON);

			String instanceFilename = Util.toIFile(instance.eResource().getURI()).getName();
			String fname = instanceFilename.substring(0, instanceFilename.lastIndexOf(".")) + ".json";

			File slangDir = new File(system.projects.get(0).rootDirectory, File.separator + ".slang");
			slangDir.mkdir();

			assert slangDir.exists() && slangDir.isDirectory() : slangDir + " does not exist";

			File outFile = new File(slangDir, fname);
			IOUtil.writeFile(outFile, air);
		}
	}
}
