package org.sireum.aadl.osate.util;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.EList;
import org.osate.aadl2.Classifier;
import org.sireum.aadl.osate.util.IOUtils.SearchType;

public class AadlProjectUtil {

	final static boolean debugging = System.getenv().containsKey("DEBUGGING_CLI");

	public static Classifier getResourceByName(String name, EList<Classifier> l) {
		boolean isQualified = name.contains("::");
		for (Classifier oc : l) {
			if (isQualified && oc.getQualifiedName().equals(name)) {
				return oc;
			} else if (oc.getName().equals(name)) {
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

	public static String relativize(File root, File other) {
		return Paths.get(root.toURI()).relativize(Paths.get(other.toURI())).toString();
	}

	public static List<AadlSystem> findSystems(File f) {
		List<AadlSystem> ret = new ArrayList<AadlSystem>();
		if (f.isDirectory()) {

			List<File> systems = IOUtils.collectFiles(f, ".system", false, SearchType.STARTS_WITH);
			if (!systems.isEmpty()) {
				// found user provided .system file so use that

				assert (systems.size() == 1);
				ret.add(AadlProjectUtil.createAadlSystem(systems.get(0)));
				return ret;
			}

			for (File file : f.listFiles()) {
				ret.addAll(findSystems(file));
			}
		} else {
			if (f.getName().equals(".project")) {
				// assumes there is only one system implementation in the
				// directory rooted at f

				AadlProject project = createAadlProject(f.getParentFile());
				if (project == null) {
					return ret;
				}
				String systemImplName = null;
				Optional<File> systemImplFile = Optional.empty();
				for (File a : project.aadlFiles) {
					for (String line : IOUtils.readFile(a).split("\n")) {
						String SYS_IMPL = "system implementation";
						if (line.contains(SYS_IMPL)) {
							if (systemImplFile.isPresent()) {
								addError("Found multiple system implementations in " + f.getParent());
								return ret;
							}
							systemImplFile = Optional.of(a);
							systemImplName = line.substring(line.indexOf(SYS_IMPL) + SYS_IMPL.length()).trim();
							break;
						}
					}
				}

				ret.add(AadlSystem.makeAadlSystem(systemImplName, systemImplFile, Arrays.asList(project), null));
			} else if (f.getName().startsWith(".system")) {
				ret.add(AadlProjectUtil.createAadlSystem(f));
			}
		}
		return ret;
	}

	public static AadlProject createAadlProject(File projectRoot) {
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

	public static AadlSystem createAadlSystem(File systemFile) {
		try {

			String[] _projects = AadlSystem.getProjectsProperty(systemFile);
			Optional<File> systemImplFile = AadlSystem.getSystemImplementationFile(systemFile);
			String systemImpl = AadlSystem.getSystemImplementationClassifierName(systemFile);

			List<AadlProject> projects = new ArrayList<AadlProject>();
			for (String p : _projects) {
				File projectRoot = new File(systemFile.getParentFile(), p);

				if (!projectRoot.exists() || !projectRoot.isDirectory()) {
					addError(projectRoot + " doesn't exist or isn't a directory");
				} else {
					projects.add(createAadlProject(projectRoot));
				}
			}
			return AadlSystem.makeAadlSystem(systemImpl, systemImplFile, projects, null);
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

	public static class SystemFileContainer {
		public AadlProject proj; // project system file is in
		public String projectRelativePath;
		public File systemImplementationFile;

		public SystemFileContainer(AadlProject _proj, String _projectRelativePath, File _systemImplementationFile) {
			this.proj = _proj;
			this.projectRelativePath = _projectRelativePath;
			this.systemImplementationFile = _systemImplementationFile;
		}

		public String getProjectRelativeURI() {
			return projectRelativePath.replace("\\", "/");
		}
	}

	public static class AadlSystem {

		public String systemImplementationName;
		public Optional<SystemFileContainer> systemFileContainer;
		public File slangOutputFile;
		public List<AadlProject> projects;

		public AadlSystem(String _systemImplementationName, Optional<SystemFileContainer> _systemFileContainer,
				List<AadlProject> _projects, File _slangOutputFile) {

			assert _systemImplementationName != null;
			assert _projects.size() > 0;
			assert !_systemFileContainer.isPresent() || (_systemFileContainer.get().systemImplementationFile.exists()
					&& _systemFileContainer.get().systemImplementationFile.isFile());
			this.systemImplementationName = _systemImplementationName;
			this.systemFileContainer = _systemFileContainer;
			this.projects = _projects;
			this.slangOutputFile = _slangOutputFile;
		}

		public AadlSystem(String _systemImplementationName, Optional<SystemFileContainer> _systemFileContainer,
				List<AadlProject> _projects) {
			this(_systemImplementationName, _systemFileContainer, _projects, null);
		}

		public boolean isSystemNameQualified() {
			return systemImplementationName.contains("::");
		}

		/*
		 * Example .system properties file
		 *
		 * projects=pca;BLESS_Resources;ice-device;physical
		 * system_impl_file=pca/aadl/packages/PCA_System.aadl
		 * system_impl=wrap_pca.imp
		 *
		 */
		public static String KEY_PROJECT = "projects";
		public static String KEY_SYSTEM_IMPL_FILE = "system_impl_file";
		public static String KEY_SYSTEM_IMPL = "system_impl";

		static String getCanonPath(File p) {
			try {
				return p.getCanonicalPath();
			} catch (Exception e) {
				return p.getAbsolutePath();
			}
		}

		public static AadlSystem makeAadlSystem(String _systemImplementationName,
				Optional<File> _systemImplementationFile, List<AadlProject> _projects, File _slangOutputFile) {

			if (_systemImplementationName == null) {
				addError(AadlSystem.KEY_SYSTEM_IMPL + " property must be present");
				return null;
			}
			if (_systemImplementationFile.isPresent()
					&& (!_systemImplementationFile.get().exists() || !_systemImplementationFile.get().isFile())) {
				addError(_systemImplementationFile.get() + " must be a file");
				return null;
			}
			if (_projects.isEmpty()) {
				addError("There must be at least one project");
				return null;
			}

			Optional<SystemFileContainer> _sysFileContainer = Optional.empty();
			if (_systemImplementationFile.isPresent()) {
				String _projRelSysImplFilename = null;
				AadlProject _p = null;

				// assume there are not nested .projects??
				String implP = getCanonPath(_systemImplementationFile.get());
				for (AadlProject p : _projects) {
					String canP = getCanonPath(p.rootDirectory);
					if (implP.startsWith(canP)) {
						_projRelSysImplFilename = relativize(p.rootDirectory, _systemImplementationFile.get());
						_p = p;
						break;
					}
				}

				if (_p != null) {
					_sysFileContainer = Optional
							.of(new SystemFileContainer(_p, _projRelSysImplFilename, _systemImplementationFile.get()));
				} else {
					addError("Could not find " + _systemImplementationFile + " in any AADL project");
					return null;
				}
			}

			return new AadlSystem(_systemImplementationName, _sysFileContainer, _projects, _slangOutputFile);
		}

		public static String getSystemImplementationClassifierName(File systemFile) {
			return IOUtils.getPropertiesFile(systemFile).getProperty(KEY_SYSTEM_IMPL);
		}

		static String[] getProjectsProperty(File systemFile) {
			String x = IOUtils.getPropertiesFile(systemFile).getProperty(KEY_PROJECT);
			if (x == null) {
				return new String[] { "." };
			} else {
				return x.split(";");
			}
		}

		public static Optional<String> getSystemImplementationFilename(File systemFile) {
			String systemImplFilename = IOUtils.getPropertiesFile(systemFile).getProperty(KEY_SYSTEM_IMPL_FILE);
			if (systemImplFilename != null) {
				return Optional.of(systemImplFilename);
			} else {
				return Optional.empty();
			}
		}

		public static Optional<File> getSystemImplementationFile(File systemFile) {
			Optional<String> systemImplFilename = getSystemImplementationFilename(systemFile);
			if (systemImplFilename.isPresent()) {
				return Optional.of(new File(systemFile.getParentFile(), systemImplFilename.get()));
			} else {
				return Optional.empty();
			}
		}

	}

	static void addInfo(String msg) {
		System.err.println(msg);
	}

	static void addError(String msg) {
		System.err.println("Error: " + msg);
	}
}
