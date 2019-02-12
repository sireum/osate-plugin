package org.sireum.aadl.osate.act.handlers;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.ui.dialogs.Dialog;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.act.ActUtil;
import org.sireum.aadl.osate.act.PreferenceValues;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;

public class LaunchAct extends AbstractSireumHandler {

	private ActPrompt p = null;

	@Override
	public String getToolName() {
		return "ACT";
	}

	@Override
	public IStatus runJob(Element elem, IProgressMonitor monitor) {

		p = null;

		MessageConsole console = displayConsole();
		console.clearConsole();

		SystemInstance si = getSystemInstance(elem);
		if (si == null) {
			Dialog.showError(getToolName(), "Please select a system implementation or a system instance");
			return Status.CANCEL_STATUS;
		}

		writeToConsole("Generating AIR ...");

		Aadl model = getAir(si, true);

		if (model != null) {

			if (PreferenceValues.getACT_SERIALIZE_OPT()) {
				File f = serializeToFile(model, PreferenceValues.getACT_OUTPUT_FOLDER_OPT(), si);
				writeToConsole("Wrote: " + f.getAbsolutePath());
			}

			Display.getDefault().syncExec(() -> {
				p = new ActPrompt(getProject(si), getShell());
				p.open();
			});

			if (p.getReturnCode() == Window.OK) {
				try {
					File out = new File(p.getOptionOutputDirectory());
					if (!out.exists()) {
						if (Dialog.askQuestion("Create Directory?",
								"Directory '" + out.getAbsolutePath() + "' does not exist.  Should it be created?")) {
							if (!out.mkdirs()) {
								Dialog.showError(getToolName(), "Could not create directory " + out.getAbsolutePath());
							}
						}
					}
					File workspaceRoot = getProjectPath(si).toFile();
					int ret = ActUtil.launchAct(p, model, console, workspaceRoot);

					refreshWorkspace();

					Dialog.showInfo(getToolName(),
							"CAmkES code " + (ret == 0 ? "successfully generated" : "generation was unsuccessful"));

				} catch (Throwable ex) {
					ex.printStackTrace();
					String m = "Could not generate CAmkES.  Please make sure ACT is present.\n\n"
							+ ex.getLocalizedMessage();
					Dialog.showError(getToolName(), m);
					return Status.CANCEL_STATUS;
				}
			}
		}

		return Status.OK_STATUS;
	}
}
