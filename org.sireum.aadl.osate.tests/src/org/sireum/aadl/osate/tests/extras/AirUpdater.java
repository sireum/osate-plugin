package org.sireum.aadl.osate.tests.extras;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
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
import org.osate.testsupport.TestResourceSetHelper;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SerializerType;

import com.google.inject.Inject;
import com.itemis.xtext.testing.XtextTest;

@RunWith(XtextRunner.class)
@InjectWith(Aadl2InjectorProvider.class)
public class AirUpdater extends XtextTest {
	@Inject
	TestResourceSetHelper rsHelper;

	@Test
	public void updateAirHamr() {
		File hamrModelsDir = new File(
				System.getenv("SIREUM_HOME") + "/hamr/codegen/jvm/src/test/scala/models");
		if (hamrModelsDir.exists()) {
			regen(hamrModelsDir, ".slang");
		} else {
			System.out.println("Directory does not exist: " + hamrModelsDir);
		}
	}

	@Test
	public void updateAirHamr2() {
		File hamrModelsDir = new File(
				"/home/vagrant/devel/case/CASE-loonwerks/TA5/tool-evaluation-4/HAMR/examples");
		if (hamrModelsDir.exists()) {
			regen(hamrModelsDir, ".slang");
		} else {
			System.out.println("Directory does not exist: " + hamrModelsDir);
		}
	}

	void regen(File rootDir, String outputDir) {

		List<File> projectDirs = new ArrayList<>();
		findProjects(rootDir, projectDirs);

		for (File f : projectDirs) {
			String projectName = getProjectName(f);

			List<File> aadlFiles = collectFiles(f, ".aadl");
			List<File> systemDescriptions = collectFiles(f, ".system");

			File sysFile = null;
			String implName = null;
			if (systemDescriptions.size() > 0) {
				// .system files should have the name of the file that contains
				// the system instance on the first line and the simple name
				// of the system instance on the second line. e.g.
				// UAV.aadl
				// UAV.Impl
				String[] testParams = readFile(systemDescriptions.get(0)).split("\n");
				sysFile = new File(f, testParams[0]);
				implName = testParams[1];
			} else {
				for (File a : aadlFiles) {
					for (String line : readFile(a).split("\n")) {
						String SYS_IMPL = "system implementation";
						if (line.contains(SYS_IMPL)) {
							assert sysFile == null : "Found multiple system implementations in " + f;
							sysFile = a;
							implName = line.substring(line.indexOf(SYS_IMPL) + SYS_IMPL.length()).trim();
							break;
						}
					}
				}
			}
			assert sysFile != null && implName != null : String.format("Null things:\n%s\n%s\n%s", f, sysFile,
					implName);
			aadlFiles.remove(sysFile);

			SystemInstance instance = getSystemInstance(projectName, sysFile, implName, aadlFiles);
			String air = Util.serialize(Util.getAir(instance, true), SerializerType.JSON);

			String instanceFilename = Util.toIFile(instance.eResource().getURI()).getName();
			String fname = instanceFilename.substring(0, instanceFilename.lastIndexOf(".")) + ".json";

			writeFile(new File(f, outputDir + "/" + fname), air);
		}
	}

	SystemInstance getSystemInstance(String projectName, File sysImplFile, String sysImplName, List<File> aadlFiles) {
		try {

			// pretend the files are organized in a project called <projectName> so that URIs in
			// AIR Positions are relative to the project, not the file system
			// https://github.com/osate/osate2/wiki/Using-contributed-resources-in-stand-alone-applications/d5e28542e20b531b8688ab962aa08606d5e619a8
			String wsRoot = sysImplFile.getParentFile().getAbsolutePath();
			EcorePlugin.getPlatformResourceMap().put(projectName, URI.createFileURI(wsRoot));

			AadlPackage pkg = parseFile(projectName, sysImplFile, aadlFiles.toArray(new File[aadlFiles.size()]));

			// instantiate
			SystemImplementation sysImpl = (SystemImplementation) getResourceByName(sysImplName,
					pkg.getOwnedPublicSection().getOwnedClassifiers());
			return InstantiateModel.instantiate(sysImpl);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.assertFalse(true);
			return null;
		}
	}

	void findProjects(File f, List<File> l) {
		if (f.isDirectory()) {
			for (File file : f.listFiles()) {
				findProjects(file, l);
			}
		} else {
			if (f.getName().equals(".project")) {
				l.add(f.getParentFile());
			}
		}
	}

	private String getProjectName(File f) {
		String marker = "<name>";
		String line = readFile(new File(f, ".project")).split("\n")[2];
		return line.substring(line.indexOf(marker) + marker.length(), line.indexOf("</name>"));
	}

	List<File> collectFiles(File dir, String endsWith) {
		return Arrays.asList(dir.listFiles()).stream()
				.filter(p -> p.isFile() && p.getName().endsWith(endsWith)).collect(Collectors.toList());
	}

	String readFile(File f) {
		try {
			return new String(Files.readAllBytes(Paths.get(f.toURI())));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	void writeFile(File f, String str) {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(f.toURI()))) {
			writer.write(str);
			System.out.println("Wrote: " + f);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	Classifier getResourceByName(String name, EList<Classifier> l) {
		for (Classifier oc : l) {
			if (oc.getName().equals(name)) {
				return oc;
			}
		}
		return null;
	}

	/** Adapted from
	 * <a href="https://github.com/osate/osate2/blob/44af9ff8d6309410aeb134a0ae825aa7c916fabf/core/org.osate.testsupport/src/org/osate/testsupport/TestHelper.java#L126"/>here</a>.
	 *
	 * Parse a set of files into the test resource set.
	 * They can be of any extension supported by the InjectorProvider injected into the JUnit test class
	 * The first file is assumed to have a root object of type T
	 *
	 * @param projectName root of paths relative to the resource set
	 * @param filePath the main file to parse
	 * @param referenced other files that may be referenced by the main file
	 * @return the root object of the main file
	 */
	public AadlPackage parseFile(String projectName, File file, File... referencedPaths) {
		ResourceSet rs = rsHelper.getResourceSet();
		for (File name : referencedPaths) {
			loadFile(projectName, name, rs);
		}
		Resource res = loadFile(projectName, file, rs);
		if (res != null) {
			@SuppressWarnings("unchecked")
			final AadlPackage root = (AadlPackage) (res.getContents().isEmpty() ? null : res.getContents().get(0));
			return root;
		}
		return null;
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
