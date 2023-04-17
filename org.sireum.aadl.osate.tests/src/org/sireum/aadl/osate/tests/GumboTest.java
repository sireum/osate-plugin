package org.sireum.aadl.osate.tests;

import java.io.File;

import org.junit.Test;

public class GumboTest extends SireumTest {

	{
		generateExpected = false;
		writeResults = true;
	}

	static File ROOT_DIR = new File("./projects/org/sireum/aadl/osate/tests/gumbo/");

	@Test
	public void data_invariants() {
		lexecute("data-invariants", "Data_Invariants.aadl", "s.impl");
	}

	@Test
	public void integration_contracts() {
		lexecute("integration-contracts", "Integration_Contracts.aadl", "s.impl");
	}

	@Test
	public void initialize_entrypoint() {
		lexecute("initialize-entrypoint", "Initialize_Entrypoint.aadl", "s.impl");
	}

	@Test
	public void compute_entrypoint() {
		lexecute("compute-entrypoint", "Compute_Entrypoint.aadl", "s.impl");
	}

	@Test
	public void enum_test() {
		lexecute("enum-test", "Enum_Test.aadl", "s.impl");
	}

	void lexecute(String dirName, String sysFilename, String sysImplName) {
		execute(new File(ROOT_DIR, dirName), sysFilename, sysImplName);
	}
}
