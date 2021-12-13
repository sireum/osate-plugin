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

	void lexecute(String dirName, String sysFilename, String sysImplName) {
		execute(new File(ROOT_DIR, dirName), sysFilename, sysImplName);
	}
}
