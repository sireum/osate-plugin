package org.sireum.aadl.osate.arsit.handlers;

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
import org.sireum.aadl.osate.arsit.ArsitUtil;
import org.sireum.aadl.osate.arsit.PreferenceValues;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;

public class LaunchArsit extends AbstractSireumHandler {
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

			MessageConsole console = displayConsole("Arsit Console");

			if (PreferenceValues.getARSIT_SERIALIZE_OPT()) {
				File f = serializeToFile(model, PreferenceValues.getARSIT_OUTPUT_FOLDER_OPT(), root);
				writeToConsole(console, "Wrote: " + f.getAbsolutePath());
			}

			ArsitPrompt p = new ArsitPrompt(getProject(root), shell);
			if (p.open() == Window.OK) {
				try {
					// Eclipse doesn't seem to like accessing nested scala classes
					// (e.g. org.sireum.cli.Cli$ArsitOption$) so invoke Arsit from scala instead

					int ret = ArsitUtil.launchArsit(p, model, console);

					MessageDialog.openInformation(shell, "Sireum", "Slang-Embedded code "
							+ (ret == 0 ? "successfully generated" : "generation was unsuccessful"));
				} catch (Exception ex) {
					ex.printStackTrace();
					String m = "Could not generate Slang-Embedded code.  Please make sure Arsit is present.\n\n"
							+ ex.getLocalizedMessage();
					MessageDialog.openError(shell, "Sireum", m);
				}
			}
		}

		return null;
	}
}
