package org.sireum.aadl.ostate.tests;

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
import org.eclipse.xtext.xbase.lib.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.testsupport.Aadl2UiInjectorProvider;
import org.osate.testsupport.OsateTest;
import org.sireum.aadl.osate.util.TestUtil;

import com.itemis.xtext.testing.FluentIssueCollection;

@RunWith(XtextRunner.class)
@InjectWith(Aadl2UiInjectorProvider.class)
public class AirTestJava extends OsateTest {

	boolean generateExpected = false;

	File root = new File("./projects/org/sireum/aadl/ostate/tests/");

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

	@SuppressWarnings("unchecked")
	void execute(String dirName, String sysFilename, String sysImplName) {
		try {
			File r = new File(root, dirName);
			ArrayList<Pair<String, String>> l = new ArrayList<>();
			String expected = null;

			for (File _f : r.listFiles()) {
				if (_f.getName().endsWith(".aadl")) {
					l.add(new Pair<>(_f.getName(), readFile(_f)));
				}
			}

			Optional<File> expectedFile = Arrays.stream(r.listFiles())
					.filter(x -> x.getName().endsWith(sysImplName + ".json")).findFirst();

			if (expectedFile.isPresent()) {
				expected = readFile(expectedFile.get());
			}

			Assert.assertTrue("Expected results not found", expected != null);

			createFiles(l.toArray(new Pair[l.size()]));

			suppressSerialization();
			FluentIssueCollection result = testFile(sysFilename);

			AadlPackage pkg = (AadlPackage) result.getResource().getContents().get(0);
			assertAllCrossReferencesResolvable(pkg);

			// instantiate
			SystemImplementation sysImpl = (SystemImplementation) getResourceByName(sysImplName,
					pkg.getOwnedPublicSection().getOwnedClassifiers());
			SystemInstance instance = InstantiateModel.buildInstanceModelFile(sysImpl);

			String ir = TestUtil.getAir(instance);

			if(generateExpected && expectedFile.isPresent()) {
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
