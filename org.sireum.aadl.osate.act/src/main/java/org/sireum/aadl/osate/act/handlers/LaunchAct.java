package org.sireum.aadl.osate.act.handlers;

import java.io.File;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.act.ActUtil;
import org.sireum.aadl.osate.act.PreferenceValues;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;

public class LaunchAct extends AbstractSireumHandler {
	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		ComponentInstance root = getComponentInstance(e);
		if (root == null) {
			MessageDialog.openError(shell, "Sireum", "Please select a system implementation or a system instance");
			return null;
		}

		Aadl model = getAir(root, true);

		if (model != null) {

			MessageConsole console = displayConsole("ACT Console");

			if (PreferenceValues.getACT_SERIALIZE_OPT()) {
				File f = serializeToFile(model, PreferenceValues.getACT_OUTPUT_FOLDER_OPT(), root);
				writeToConsole(console, "Wrote: " + f.getAbsolutePath());
			}

			ActPrompt p = new ActPrompt(getProject(root), shell);
			if (p.open() == Window.OK) {
				try {
					File out = new File(p.getOptionOutputDirectory());
					if (!out.exists()) {
						if (MessageDialog.openQuestion(shell, "Create Directory?",
								"Directory '" + out.getAbsolutePath() + "' does not exist.  Should it be created?")) {
							if (!out.mkdirs()) {
								MessageDialog.openError(shell, "Error",
										"Could not create directory " + out.getAbsolutePath());
								return null;
							}
						}
					}
					File workspaceRoot = getProjectPath(root).toFile();
					int ret = ActUtil.launchAct(p, model, console, workspaceRoot);

					MessageDialog.openInformation(shell, "Sireum",
							"CAmkES code " + (ret == 0 ? "successfully generated" : "generation was unsuccessful"));

				} catch (Exception ex) {
					String m = "Could not generate CAmkES.  Please make sure ACT is present.\n\n"
							+ ex.getLocalizedMessage();
					MessageDialog.openError(shell, "Sireum", m);
				}
			}
		}

		return null;
	}
}
