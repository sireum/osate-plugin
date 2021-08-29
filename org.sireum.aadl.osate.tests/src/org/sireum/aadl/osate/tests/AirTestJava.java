package org.sireum.aadl.osate.tests;

import java.io.File;

import org.junit.Test;

public class AirTestJava extends SireumTest {

	{
		generateExpected = false;
		writeResults = true;
	}

	static File ROOT_DIR = new File("./projects/org/sireum/aadl/osate/tests/");

	@Test
	public void pca_pump_chassis() {
		lexecute("pca-pump-chasis-gen", "Chassis.aadl", "Chassis_System.i");
	}

	@Test
	public void connection_Test_one_reference() {
		lexecute("connection-translation-tests", "Connection_Translation.aadl", "Root.one_reference");
	}

	@Test
	public void connection_Test_two_references() {
		lexecute("connection-translation-tests", "Connection_Translation.aadl", "Root.two_references");
	}

	@Test
	public void connection_Test_three_references() {
		lexecute("connection-translation-tests", "Connection_Translation.aadl", "Root.three_references");
	}

	@Test
	public void bus_Access_Test_Dual_Processor_PowerPC() {
		lexecute("bus-access-tests", "Bus_Access.aadl", "Dual_Processor.PowerPC");
	}

	@Test
	public void pca_pulseox_spiral15_insecure() {
		// FIXME
		assert false : "FIXME - doesn't work via OSATE either";
		// execute("PCA_PulseOX_spiral15", "PCA_Example.aadl", "PCA_PulseOx.insecure");
	}

	@Test
	public void feature_Grpup_Tests_Concrete_Sys() {
		lexecute("feature-group-tests", "Feature_Group_TestCase.aadl", "Concrete_Sys.impl");
	}

	void lexecute(String dirName, String sysFilename, String sysImplName) {
		execute(new File(ROOT_DIR, dirName), sysFilename, sysImplName);
	}
}
