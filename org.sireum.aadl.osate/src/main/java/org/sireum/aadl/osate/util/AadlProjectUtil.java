package org.sireum.aadl.osate.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.EList;
import org.osate.aadl2.Classifier;
import org.sireum.aadl.osate.util.IOUtil.SearchType;

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
		assertHalt(projectFile.getName().equals(".project"),
				"Expecting project filename to be .project but it's " + projectFile.getName());

		String marker = "<name>";
		String line = IOUtil.readFile(projectFile).split("\n")[2];
		return line.substring(line.indexOf(marker) + marker.length(), line.indexOf("</name>"));
	}

	public static String relativize(File root, File other) {
		return Paths.get(root.toURI()).relativize(Paths.get(other.toURI())).toString();
	}

	public static List<AadlSystem> findSystems(File f) {
		return findSystems(f, new ArrayList<>());
	}

	// @param ignorePaths f will be skipped/ignored if its canonical path contains any of the strings in ignorePaths
	public static List<AadlSystem> findSystems(File f, List<String> ignorePaths) {
		List<AadlSystem> ret = new ArrayList<>();
		if (f.isDirectory()) {
			try {
				if (ignorePaths.stream().noneMatch(f.getCanonicalPath()::contains)) {
					List<File> systems = IOUtil.collectFiles(f, ".system", false, SearchType.STARTS_WITH);
					if (!systems.isEmpty()) {
						// found user provided .system file so use that

						for (File systemFile : systems) {
							ret.add(AadlProjectUtil.createAadlSystem(systemFile));
						}
						return ret;
					}

					for (File file : f.listFiles()) {
						ret.addAll(findSystems(file, ignorePaths));
					}
				}
			} catch (IOException e) {

			}
		} else {
			if (f.getName().equals(".project") && !IOUtil.collectFiles(f.getParentFile(), ".aadl", true).isEmpty()) {

				AadlProject project = createAadlProject(f.getParentFile());

				boolean add = true;

				String systemImplName = null;
				Optional<File> systemImplFile = Optional.empty();
				for (File a : project.aadlFiles) {
					for (String line : IOUtil.readFile(a).split("\n")) {
						String SYS_IMPL = "system implementation";
						if (line.toLowerCase().contains(SYS_IMPL)) {
							if (systemImplFile.isPresent()) {
								addWarning("Found multiple system implementations, please provide a .system file for: " + f.getParent());
								add = false;
							} else {
								systemImplFile = Optional.of(a);
								systemImplName = line.substring(line.indexOf(SYS_IMPL) + SYS_IMPL.length()).trim();
							}
						}
					}
				}

				if (add) {
					ret.add(AadlSystem.makeAadlSystem(systemImplName, systemImplFile, Arrays.asList(project), null));
				}
			} else if (f.getName().startsWith(".system")) {
				ret.add(AadlProjectUtil.createAadlSystem(f));
			}
		}
		return ret;
	}

	public static AadlProject createAadlProject(File projectRoot) {
		File projectFile = new File(projectRoot, ".project");

		String projectName = "aadl_project";
		if (projectFile.exists() && projectFile.isFile()) {
			projectName = AadlProjectUtil.getProjectName(projectFile);
		} else {
			addWarning("Using '" + projectName
					+ "' as the project name since the following directory does not contain a .project file: "
					+ projectRoot.getPath());
		}

		List<File> aadlFiles = IOUtil.collectFiles(projectRoot, ".aadl", true);
		AadlProject project = new AadlProject(projectName, projectRoot, aadlFiles);

		return project;
	}

	public static AadlSystem createAadlSystem(File systemFile) {
		try {

			String[] _projects = AadlSystem.getProjectsProperty(systemFile);
			Optional<File> systemImplFile = AadlSystem.getSystemImplementationFile(systemFile);
			String systemImpl = AadlSystem.getSystemImplementationClassifierName(systemFile);

			List<AadlProject> projects = new ArrayList<>();
			for (String p : _projects) {
				File projectRoot = new File(systemFile.getParentFile(), p);

				if (!projectRoot.exists() || !projectRoot.isDirectory()) {
					assertHalt(false, projectRoot + " doesn't exist or isn't a directory");
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
			assertHalt(_projectName != null, "_projectName cannot be null");
			assertHalt(_rootDirectory != null && _rootDirectory.exists() && _rootDirectory.isDirectory(),
					"_rootDirectory is not a directory: " + _rootDirectory);
			assertHalt(!_aadlFiles.isEmpty(), "No aadl files for " + projectName + " in root dir " + rootDirectory);

			projectName = _projectName;
			rootDirectory = _rootDirectory;
			aadlFiles = _aadlFiles;
		}
	}

	public static class SystemFileContainer {
		public AadlProject proj; // project system file is in
		public String projectRelativePath;
		public File systemImplementationFile;

		public SystemFileContainer(AadlProject _proj, String _projectRelativePath, File _systemImplementationFile) {
			proj = _proj;
			projectRelativePath = _projectRelativePath;
			systemImplementationFile = _systemImplementationFile;
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

			assertHalt(_systemImplementationName != null, "_systemImplementationName cannot be null");
			assertHalt(_projects.size() > 0, "There must be at least one project");
			assertHalt(
					!_systemFileContainer.isPresent() || (_systemFileContainer.get().systemImplementationFile.exists()
							&& _systemFileContainer.get().systemImplementationFile.isFile()),
					"_systemFileContainer should either be empty or be a valid file: " + _systemFileContainer);

			systemImplementationName = _systemImplementationName;
			systemFileContainer = _systemFileContainer;
			projects = _projects;
			slangOutputFile = _slangOutputFile;
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

			assertHalt(_systemImplementationName != null, AadlSystem.KEY_SYSTEM_IMPL + " property must be present");

			assertHalt(
					!_systemImplementationFile.isPresent()
							|| (_systemImplementationFile.get().exists() && _systemImplementationFile.get().isFile()),
					_systemImplementationFile.get() + " must be a file");

			assertHalt(!_projects.isEmpty(), "There must be at least one project");

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
					assertHalt(false, "Could not find " + _systemImplementationFile + " in any AADL project");
					return null;
				}
			}

			return new AadlSystem(_systemImplementationName, _sysFileContainer, _projects, _slangOutputFile);
		}

		public static String getSystemImplementationClassifierName(File systemFile) {
			return IOUtil.getPropertiesFile(systemFile).getProperty(KEY_SYSTEM_IMPL);
		}

		static String[] getProjectsProperty(File systemFile) {
			String x = IOUtil.getPropertiesFile(systemFile).getProperty(KEY_PROJECT);
			if (x == null) {
				return new String[] { "." };
			} else {
				return x.split(";");
			}
		}

		public static Optional<String> getSystemImplementationFilename(File systemFile) {
			String systemImplFilename = IOUtil.getPropertiesFile(systemFile).getProperty(KEY_SYSTEM_IMPL_FILE);
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

	static void assertHalt(boolean cond, String mesg) {
		if (!cond) {
			throw new RuntimeException(mesg);
		}
	}

	static void addInfo(String msg) {
		System.out.println(msg);
	}

	static void addWarning(String msg) {
		System.out.println("Warning: " + msg);
	}
}
