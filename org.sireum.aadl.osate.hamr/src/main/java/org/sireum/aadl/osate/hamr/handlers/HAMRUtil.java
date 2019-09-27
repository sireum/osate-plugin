package org.sireum.aadl.osate.hamr.handlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osate.aadl2.ComponentCategory;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.ConnectionKind;
import org.osate.aadl2.instance.SystemInstance;
import org.sireum.IS;
import org.sireum.Z;
import org.sireum.aadl.arsit.ArsitBridge.Platform;
import org.sireum.aadl.osate.hamr.PreferenceValues;

public class HAMRUtil {

	public static List<Report> checkModel(SystemInstance root, HAMRPrompt prompt) {

		boolean targetSel4 = prompt.getOptionPlatform() == Platform.SeL4;
		boolean trustedBuild = prompt.getOptionTrustedBuildProfile();
		boolean treatDevicesAsThreads = PreferenceValues.getHAMR_DEVICES_AS_THREADS_OPT();

		List<Report> ret = new ArrayList<>();
		// List<ComponentInstance> allThreads = root.getAllComponentInstances().stream()
		// .filter(f -> isThread(f)).collect(Collectors.toList());
		// List<ComponentInstance> allDevices = root.getAllComponentInstances().stream()
		// .filter(f -> isDevice(f)).collect(Collectors.toList());
		List<ConnectionInstance> allConnections = root.getAllComponentInstances().stream()
				.flatMap(f -> f.getConnectionInstances().stream()).collect(Collectors.toList());

		if (!trustedBuild) {
			List<ConnectionInstance> threadDeviceConnections = allConnections.stream().filter(conn -> {
				ComponentInstance src = conn.getSource().getComponentInstance();
				ComponentInstance dst = conn.getDestination().getComponentInstance();
				return treatDevicesAsThreads ? isThreadOrDevice(src) && isThreadOrDevice(dst)
						: isThread(src) && isThread(dst);
			}).collect(Collectors.toList());

			for (ConnectionInstance conn : threadDeviceConnections) {
				if (conn.getKind() != ConnectionKind.PORT_CONNECTION) {
					String msg = "HAMR integration does not currently support " + conn.getKind();
					if (targetSel4) {
						msg += ". Use the '" + prompt.OPTION_TRUSTED_BUILD_PROFILE.displayText
								+ "' option if only targeting CAmkES.";
					}
					ret.add(inst.new ErrorReport(conn, msg));
				}
			}
		}

		return ret;
	}

	static boolean isThreadOrDevice(ComponentInstance c) {
		return isDevice(c) || isThread(c);
	}

	static boolean isDevice(ComponentInstance c) {
		return (c != null) && (c.getCategory() == ComponentCategory.DEVICE);
	}

	static boolean isThread(ComponentInstance c) {
		return (c != null) && (c.getCategory() == ComponentCategory.THREAD);
	}

	public static boolean shouldTranspile(HAMRPrompt p) {
		switch (p.getOptionPlatform()) {
		case JVM:
			return false;
		case SeL4:
			return !p.getOptionTrustedBuildProfile();
		case Linux:
		case MacOS:
		case Cygwin:
			return true;
		default:
			throw new RuntimeException("Unexpected platform: " + p.getOptionPlatform());
		}
	}

	public static String cleanupPackageName(String p) {
		return p.replaceAll("-", "_");
	}

	@SafeVarargs
	public static <T> IS<Z, T> toISZ(T... args) {
		scala.collection.Seq<T> seq = scala.collection.JavaConverters.asScalaBuffer(java.util.Arrays.asList(args));
		IS<Z, T> ret = org.sireum.IS$.MODULE$.apply(seq, org.sireum.Z$.MODULE$);
		return ret;
	}

	public static int invoke(MessageConsole console, String[] commands) {
		MessageConsoleStream mcs = console.newMessageStream();
		try {
			Runtime rt = Runtime.getRuntime();
			String[] envp = new String[] { "SIREUM_HOME=" + PreferenceValues.getHAMR_SIREUM_HOME() };
			Process p = rt.exec(commands, envp);

			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line;
			while ((line = input.readLine()) != null) {
				mcs.write(line + "\n");
			}

			BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = error.readLine()) != null) {
				mcs.write(line + "\n");
			}

			p.waitFor();

			return p.exitValue();

		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
	}

	private final static HAMRUtil inst = new HAMRUtil();

	abstract class Report {
		String kind;
		NamedElement component;
		String message;

		Report(String kind, NamedElement component, String message) {
			this.kind = kind;
			this.component = component;
			this.message = message;
		}

		@Override
		public String toString() {
			return kind + ": " + component.getFullName() + ": " + message;
		}
	}

	class InfoReport extends Report {
		InfoReport(NamedElement component, String message) {
			super("Info", component, message);
		}
	}

	class WarningReport extends Report {
		WarningReport(NamedElement component, String message) {
			super("Warning", component, message);
		}
	}

	class ErrorReport extends Report {
		ErrorReport(NamedElement component, String message) {
			super("Error", component, message);
		}
	}
}
