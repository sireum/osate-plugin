package org.sireum.aadl.osate.tests;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.osate.aadl2.instance.SystemInstance;
import org.sireum.aadl.osate.tests.util.IOUtils;
import org.sireum.aadl.osate.tests.util.UpdaterUtil.TestAadlProject;
import org.sireum.aadl.osate.tests.util.UpdaterUtil.TestAadlSystem;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SerializerType;
import org.sireum.hamr.ir.Aadl;

public class AirTestJava extends SireumTest {

	boolean generateExpected = false;
	boolean writeResults = true;

	static File ROOT_DIR = new File("./projects/org/sireum/aadl/osate/tests/");

	@Test
	public void pca_pump_chassis() {
		execute("pca-pump-chasis-gen", "Chassis.aadl", "Chassis_System.i");
	}

	@Test
	public void connection_Test_one_reference() {
		execute("connection-translation-tests", "Connection_Translation.aadl", "Root.one_reference");
	}

	@Test
	public void connection_Test_two_references() {
		execute("connection-translation-tests", "Connection_Translation.aadl", "Root.two_references");
	}

	@Test
	public void connection_Test_three_references() {
		execute("connection-translation-tests", "Connection_Translation.aadl", "Root.three_references");
	}

	@Test
	public void bus_Access_Test_Dual_Processor_PowerPC() {
		execute("bus-access-tests", "Bus_Access.aadl", "Dual_Processor.PowerPC");
	}

	@Test
	public void pca_pulseox_spiral15_insecure() {
		// FIXME
		assert false : "FIXME - doesn't work via OSATE either";
		// execute("PCA_PulseOX_spiral15", "PCA_Example.aadl", "PCA_PulseOx.insecure");
	}

	@Test
	public void feature_Grpup_Tests_Concrete_Sys() {
		execute("feature-group-tests", "Feature_Group_TestCase.aadl", "Concrete_Sys.impl");
	}

	void execute(String dirName, String sysFilename, String sysImplName) {
		try {
			File root = new File(ROOT_DIR, dirName);

			List<File> aadlFiles = IOUtils.collectFiles(root, ".aadl", true);
			File sysImplFile = new File(root, sysFilename);
			assert sysImplFile.exists() : sysImplFile.getAbsolutePath() + "doesn't exist";

			TestAadlProject project = new TestAadlProject(root.getName(), root, aadlFiles);
			TestAadlSystem system = new TestAadlSystem(sysImplName, sysImplFile, Arrays.asList(project), root);

			SystemInstance instance = getSystemInstance(system);

			Aadl model = Util.getAir(instance, true, System.out);
			String ir = Util.serialize(model, SerializerType.JSON);

			Optional<File> expectedFile = Arrays.stream(root.listFiles())
					.filter(x -> x.getName().endsWith(sysImplName + ".json")).findFirst();
			if (writeResults) {
				File results = new File(root, sysImplName + "_results.json");
				IOUtils.writeFile(results, ir);
			}
			Assert.assertTrue("Expected results not found", expectedFile.isPresent());
			String expected = IOUtils.readFile(expectedFile.get());
			if (generateExpected) {
				IOUtils.writeFile(expectedFile.get(), ir);
				expected = ir;
			}

			Assert.assertEquals(ir, expected);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.assertFalse(true);
		}
	}
}
