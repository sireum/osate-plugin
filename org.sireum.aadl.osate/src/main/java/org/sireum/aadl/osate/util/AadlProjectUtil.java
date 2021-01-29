package org.sireum.aadl.osate.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.osate.aadl2.Classifier;

public class AadlProjectUtil {

	public static Classifier getResourceByName(String name, EList<Classifier> l) {
		for (Classifier oc : l) {
			if (oc.getName().equals(name)) {
				return oc;
			}
		}
		return null;
	}

	public static String getProjectName(File projectFile) {
		assert (projectFile.getName().equals(".project"));

		String marker = "<name>";
		String line = IOUtils.readFile(projectFile).split("\n")[2];
		return line.substring(line.indexOf(marker) + marker.length(), line.indexOf("</name>"));
	}

	public static List<AadlSystem> findSystems(File f) {
		List<AadlSystem> ret = new ArrayList<AadlSystem>();
		if (f.isDirectory()) {
			List<File> systems = IOUtils.collectFiles(f, ".system", false);
			if (!systems.isEmpty()) {
				// found user provided .system file so use that

				assert (systems.size() == 1);
				ret.add(AadlProjectUtil.createTestAadlSystem(systems.get(0)));
				return ret;
			}
			for (File file : f.listFiles()) {
				ret.addAll(findSystems(file));
			}
		} else {
			if (f.getName().equals(".project")) {
				// assumes there is only one system implementation in the
				// directory rooted at f

				AadlProject project = createTestAadlProject(f.getParentFile());
				if (project == null) {
					return ret;
				}
				String systemImpl = null;
				File systemFile = null;
				for (File a : project.aadlFiles) {
					for (String line : IOUtils.readFile(a).split("\n")) {
						String SYS_IMPL = "system implementation";
						if (line.contains(SYS_IMPL)) {
							if (systemFile != null) {
								addError("Found multiple system implementations in " + f.getParent());
								return ret;
							}
							systemFile = a;
							systemImpl = line.substring(line.indexOf(SYS_IMPL) + SYS_IMPL.length()).trim();
							break;
						}
					}
				}
				ret.add(new AadlSystem(systemImpl, systemFile, Arrays.asList(project)));
			}
		}
		return ret;
	}

	public static AadlProject createTestAadlProject(File projectRoot) {
		File projectFile = new File(projectRoot, ".project");

		if (!projectFile.exists() || !projectFile.isFile()) {
			addError(projectFile + " does not exist or isn't a file");
			return null;
		}

		String projectName = AadlProjectUtil.getProjectName(projectFile);
		List<File> aadlFiles = IOUtils.collectFiles(projectRoot, ".aadl", true);
		AadlProject project = new AadlProject(projectName, projectRoot, aadlFiles);

		return project;
	}

	public static AadlSystem createTestAadlSystem(File systemFile) {
		try {

			String[] _projects = AadlSystem.getProjectsProperty(systemFile);
			File systemImplFile = AadlSystem.getSystemImplementationFile(systemFile);
			String systemImpl = AadlSystem.getSystemImplementationClassifierName(systemFile);

			List<AadlProject> projects = new ArrayList<AadlProject>();
			for (String p : _projects) {
				File projectRoot = new File(systemFile.getParentFile(), p);

				if (!projectRoot.exists() || !projectRoot.isDirectory()) {
					addError(projectRoot + " doesn't exist or isn't a directory");
				} else {
					projects.add(createTestAadlProject(projectRoot));
				}
			}
			return new AadlSystem(systemImpl, systemImplFile, projects);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static class AadlProject {
		public String projectName;
		public File rootDirectory;
		public List<File> aadlFiles;

		public AadlProject(String _projectName, File _rootDirectory, List<File> _aadlFiles) {
			assert (_projectName != null);
			assert _rootDirectory != null && _rootDirectory.exists() && _rootDirectory.isDirectory();
			assert !_aadlFiles.isEmpty() : "no aadl files for " + projectName + " in root dir " + rootDirectory;

			this.projectName = _projectName;
			this.rootDirectory = _rootDirectory;
			this.aadlFiles = _aadlFiles;
		}
	}

	public static class AadlSystem {

		public String systemImplementationName;
		public File systemImplementationFile;
		public File slangOutputFile;
		public List<AadlProject> projects;

		public AadlSystem(String _systemImpl, File _systemFile, List<AadlProject> _projects, File _slangOutputFile) {
			assert _systemImpl != null;
			assert _systemFile != null && _systemFile.exists() && _systemFile.isFile();
			assert _projects.size() > 0;

			this.systemImplementationName = _systemImpl;
			this.systemImplementationFile = _systemFile;
			this.projects = _projects;
			this.slangOutputFile = _slangOutputFile;
		}

		public AadlSystem(String _systemImpl, File _systemFile, List<AadlProject> _projects) {
			// assume first project contains a .slang subdirectory
			this(_systemImpl, _systemFile, _projects, null);
		}

		/*
		 * Example .system properties file
		 *
		 * projects=pca;BLESS_Resources;ice-device;physical
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

	static void addError(String msg) {
		System.err.println("Error: " + msg);
	}
}
