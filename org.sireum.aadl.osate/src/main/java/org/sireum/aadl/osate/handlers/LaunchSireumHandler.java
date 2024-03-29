package org.sireum.aadl.osate.handlers;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.console.MessageConsole;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.ui.dialogs.Dialog;
import org.sireum.aadl.osate.PreferenceValues;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SerializerType;
import org.sireum.aadl.osate.util.VisitorUtil;
import org.sireum.hamr.ir.Aadl;
import org.sireum.message.Reporter;

public class LaunchSireumHandler extends AbstractSireumHandler {

	@Override
	public IStatus runJob(Element elem, IProgressMonitor monitor) {

		MessageConsole console = displayConsole();
		console.clearConsole();

		if (!Util.emitSireumVersion(console)) {
			displayPopup("AIR generation was unsuccessful");
			return Status.CANCEL_STATUS;
		}

		SystemInstance si = getSystemInstance(elem);
		if (si == null) {
			Dialog.showError(getToolName(), "Please select a system implementation or a system instance");
			return Status.CANCEL_STATUS;
		}

		writeToConsole("Generating AIR ...");

		Reporter reporter = Util.createReporter();

		Aadl model = Util.getAir(si, true, console, reporter);

		if (model != null && !reporter.hasError()) {
			SerializerType ser = PreferenceValues.getSERIALIZATION_METHOD_OPT();

			writeToConsole("Serializing AIR to " + ser.name() + " ...");

			String s = Util.serialize(model, ser);

			Display.getDefault().syncExec(() -> {
				FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
				fd.setFileName("aadl." + (ser == SerializerType.MSG_PACK ? "msgpack" : "json"));
				fd.setText("Specify filename");
				fd.setFilterPath(getProjectPath(si).toString());
				String fname = fd.open();

				if (fname != null) {
					File fout = new File(fname);
					writeFile(fout, s, false);
					writeToConsole("Wrote: " + fout.getAbsolutePath());
				}
			});

			Util.addMarkers(PreferenceValues.SIREUM_MARKER_ID, VisitorUtil.toIList(), si, reporter);

			refreshWorkspace();

			return Status.OK_STATUS;
		} else {
			Dialog.showError(getToolName(), "AIR generation failed");
			writeToConsole("AIR generation failed");

			Util.addMarkers(PreferenceValues.SIREUM_MARKER_ID, VisitorUtil.toIList(), si, reporter);

			return Status.CANCEL_STATUS;
		}
	}
}
