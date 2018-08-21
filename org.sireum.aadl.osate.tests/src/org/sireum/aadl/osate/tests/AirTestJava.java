package org.sireum.aadl.osate.tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.testsupport.Aadl2InjectorProvider;
import org.osate.testsupport.TestHelper;
import org.sireum.aadl.osate.util.TestUtil;

import com.google.inject.Inject;
import com.itemis.xtext.testing.XtextTest;

@RunWith(XtextRunner.class)
@InjectWith(Aadl2InjectorProvider.class)
public class AirTestJava extends XtextTest {

	@Inject
	TestHelper<AadlPackage> testHelper;

	boolean generateExpected = false;

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

//	@Test
//	public void feature_Grpup_Tests_Concrete_Sys() {
//		execute("feature-group-tests", "Feature_Group_TestCase.aadl", "Concrete_Sys.impl");
//	}

	void execute(String dirName, String sysFilename, String sysImplName) {
		try {
			File r = new File(ROOT_DIR, dirName);
			String sys = null;
			ArrayList<String> l = new ArrayList<>();

			for (File f : r.listFiles()) {
				if(f.getName().endsWith(".aadl")) {
					if(f.getName().equals(sysFilename)){
						sys = readFile(f);
					} else {
						l.add(readFile(f));
					}
				}
			}

			AadlPackage pkg = testHelper.parseString(sys, l.toArray(new String[l.size()]));

			assertAllCrossReferencesResolvable(pkg);

			// instantiate
			SystemImplementation sysImpl = (SystemImplementation) getResourceByName(sysImplName,
					pkg.getOwnedPublicSection().getOwnedClassifiers());
			SystemInstance instance = InstantiateModel.buildInstanceModelFile(sysImpl);

			String ir = TestUtil.getAir(instance);

			Optional<File> expectedFile = Arrays.stream(r.listFiles())
					.filter(x -> x.getName().endsWith(sysImplName + ".json")).findFirst();
			Assert.assertTrue("Expected results not found", expectedFile.isPresent());
			String expected = readFile(expectedFile.get());
			if (generateExpected) {
				Files.write(Paths.get(expectedFile.get().toURI()), ir.getBytes(StandardCharsets.UTF_8));
				System.out.println("Wrote: " + expectedFile.get().getAbsolutePath());
				expected = ir;
			}

			Assert.assertEquals(ir, expected);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.assertFalse(true);
		}
	}

	String readFile(File f) throws IOException {
		return new String(Files.readAllBytes(Paths.get(f.toURI())));
	}

	Classifier getResourceByName(String name, EList<Classifier> l) {
		for (Classifier oc : l) {
			if (oc.getName().equals(name)) {
				return oc;
			}
		}
		return null;
	}
}
