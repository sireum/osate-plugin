package org.sireum.aadl.osate.hamr.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.osate.aadl2.ComponentCategory;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.ConnectionKind;
import org.osate.aadl2.instance.SystemInstance;
import org.sireum.aadl.osate.hamr.PreferenceValues;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.Platform;

public class HAMRUtil {

	public static List<Report> checkModel(SystemInstance root, HAMRPrompt prompt) {

		boolean targetSel4 = prompt.getOptionPlatform() == Platform.seL4
				|| prompt.getOptionPlatform() == Platform.seL4_Only || prompt.getOptionPlatform() == Platform.seL4_TB;
		boolean trustedBuild = prompt.getOptionPlatform() == Platform.seL4_Only
				|| prompt.getOptionPlatform() == Platform.seL4_TB;
		boolean treatDevicesAsThreads = PreferenceValues.HAMR_DEVICES_AS_THREADS_OPT.getValue();

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
						msg += ". Use the '" + Platform.seL4_Only + "' or " + Platform.seL4_TB
								+ "' option if only targeting CAmkES.";
					}

					// ret.add(inst.new ErrorReport(conn, msg));
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

	public static String cleanupPackageName(String p) {
		return p.replaceAll("-", "_");
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
