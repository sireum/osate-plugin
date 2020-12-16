package org.sireum.aadl.osate.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.osate.aadl2.Classifier;

public class UpdaterUtil {

	static Classifier getResourceByName(String name, EList<Classifier> l) {
		for (Classifier oc : l) {
			if (oc.getName().equals(name)) {
				return oc;
			}
		}
		return null;
	}

	static String getProjectName(File projectFile) {
		assert (projectFile.getName().equals(".project"));

		String marker = "<name>";
		String line = IOUtils.readFile(projectFile).split("\n")[2];
		return line.substring(line.indexOf(marker) + marker.length(), line.indexOf("</name>"));
	}

	static List<TestAadlSystem> findSystems(File f) {
		List<TestAadlSystem> ret = new ArrayList<TestAadlSystem>();
		if (f.isDirectory()) {
			List<File> systems = IOUtils.collectFiles(f, ".system", false);
			if (!systems.isEmpty()) {
				// found user provided .system file so use that

				assert (systems.size() == 1);
				ret.add(UpdaterUtil.createTestAadlSystem(systems.get(0)));
				return ret;
			}
			for (File file : f.listFiles()) {
				ret.addAll(findSystems(file));
			}
		} else {
			if (f.getName().equals(".project")) {
				// assumes there is only one system implementation in the
				// directory rooted at f

				String projectName = UpdaterUtil.getProjectName(f);
				File rootDirectory = f.getParentFile();
				List<File> aadlFiles = IOUtils.collectFiles(rootDirectory, ".aadl", true);
				TestAadlProject project = new TestAadlProject(projectName, rootDirectory, aadlFiles);

				String systemImpl = null;
				File systemFile = null;
				for (File a : aadlFiles) {
					for (String line : IOUtils.readFile(a).split("\n")) {
						String SYS_IMPL = "system implementation";
						if (line.contains(SYS_IMPL)) {
							assert systemFile == null : "Found multiple system implementations in " + f.getParent();
							systemFile = a;
							systemImpl = line.substring(line.indexOf(SYS_IMPL) + SYS_IMPL.length()).trim();
							break;
						}
					}
				}
				ret.add(new TestAadlSystem(systemImpl, systemFile, List.of(project)));
			}
		}
		return ret;
	}

	static TestAadlSystem createTestAadlSystem(File systemFile) {
		try {

			String[] _projects = TestAadlSystem.getProjectsProperty(systemFile);
			File systemImplFile = TestAadlSystem.getSystemImplementationFile(systemFile);
			String systemImpl = TestAadlSystem.getSystemImplementationClassifierName(systemFile);

			List<TestAadlProject> projects = new ArrayList<TestAadlProject>();
			for (String p : _projects) {
				File projectFile = new File(systemFile.getParentFile(), p);
				assert (projectFile.exists() && projectFile.isFile() && projectFile.getName().equals(".project"));

				String projectName = getProjectName(projectFile);
				List<File> aadlFiles = IOUtils.collectFiles(projectFile.getParentFile(), ".aadl", true);

				projects.add(new TestAadlProject(projectName, projectFile.getParentFile(), aadlFiles));
			}
			return new TestAadlSystem(systemImpl, systemImplFile, projects);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	static class TestAadlProject {
		String projectName;
		File rootDirectory;
		List<File> aadlFiles;

		TestAadlProject(String _projectName, File _rootDirectory, List<File> _aadlFiles) {
			assert (_projectName != null);
			assert _rootDirectory != null && _rootDirectory.exists() && _rootDirectory.isDirectory();
			assert !_aadlFiles.isEmpty() : "no aadl files for " + projectName + " in root dir " + rootDirectory;

			this.projectName = _projectName;
			this.rootDirectory = _rootDirectory;
			this.aadlFiles = _aadlFiles;
		}
	}

	static class TestAadlSystem {

		String systemImplementationName;
		File systemImplementationFile;
		File slangOutputDir;
		List<TestAadlProject> projects;

		TestAadlSystem(String _systemImpl, File _systemFile, List<TestAadlProject> _projects, File _slangOutputDir) {
			assert _systemImpl != null;
			assert _systemFile != null && _systemFile.exists() && _systemFile.isFile();
			assert _projects.size() > 0;
			assert _slangOutputDir.exists() && _slangOutputDir.isDirectory();

			this.systemImplementationName = _systemImpl;
			this.systemImplementationFile = _systemFile;
			this.projects = _projects;
			this.slangOutputDir = _slangOutputDir;
		}

		TestAadlSystem(String _systemImpl, File _systemFile, List<TestAadlProject> _projects) {
			// assume first project contains a .slang subdirectory
			this(_systemImpl, _systemFile, _projects, new File(_projects.get(0).rootDirectory, ".slang"));
		}

		/*
		 * Example .system properties file
		 *
		 * projects=pca/.project;BLESS_Resources/.project;ice-device/.project;physical/.project
		 * system_impl_file=pca/aadl/packages/PCA_System.aadl
		 * system_impl=wrap_pca.imp
		 *
		 */
		static String KEY_PROJECT = "projects";
		static String KEY_SYSTEM_IMPL_FILE = "system_impl_file";
		static String KEY_SYSTEM_IMPL = "system_impl";

		public static String getSystemImplementationClassifierName(File systemFile) {
			return IOUtils.getPropertiesFile(systemFile).getProperty(KEY_SYSTEM_IMPL);
		}

		static String[] getProjectsProperty(File systemFile) {
			return IOUtils.getPropertiesFile(systemFile).getProperty(KEY_PROJECT).split(";");
		}

		public static File getSystemImplementationFile(File systemFile) {
			String systemImplFilename = IOUtils.getPropertiesFile(systemFile).getProperty(KEY_SYSTEM_IMPL_FILE);
			return new File(systemFile.getParentFile(), systemImplFilename);
		}

	}
}
