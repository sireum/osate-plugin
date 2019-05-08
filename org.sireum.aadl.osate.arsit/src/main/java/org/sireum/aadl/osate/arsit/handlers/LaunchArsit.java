package org.sireum.aadl.osate.arsit.handlers;

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
import org.sireum.aadl.arsit.ArsitBridge;
import org.sireum.aadl.arsit.ArsitBridge.IPCMechanismJava;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.arsit.PreferenceValues;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;
import org.sireum.aadl.osate.util.Util;

public class LaunchArsit extends AbstractSireumHandler {

	private ArsitPrompt p = null;

	@Override
	public String getToolName() {
		return "Arsit";
	}

	@Override
	public IStatus runJob(Element elem, IProgressMonitor monitor) {
		p = null;

		MessageConsole console = displayConsole();
		console.clearConsole();

		SystemInstance si = getSystemInstance(elem);
		if (si == null) {
			Dialog.showError("Sireum", "Please select a system implementation or a system instance");
			return Status.CANCEL_STATUS;
		}

		writeToConsole("Generating AIR ...");

		Aadl model = Util.getAir(si, true, console);

		if (model != null) {

			if (PreferenceValues.getARSIT_SERIALIZE_OPT()) {
				File f = serializeToFile(model, PreferenceValues.getARSIT_OUTPUT_FOLDER_OPT(), si);
				writeToConsole(console, "Wrote: " + f.getAbsolutePath());
			}

			Display.getDefault().syncExec(() -> {
				p = new ArsitPrompt(getProject(si), getShell());
				p.open();
			});

			if (p.getReturnCode() == Window.OK) {
				try {
					// Eclipse doesn't seem to like accessing nested scala classes
					// (e.g. org.sireum.cli.Cli$ArsitOption$) so invoke Arsit from scala instead

					// int ret = ArsitUtil.launchArsit(p, model, console);

					int ret = Util.callWrapper(getToolName(), console, () -> {

						ArsitBridge.IPCMechanismJava ipc = IPCMechanismJava.MessageQueue;

						if (p.getOptionIPCMechanism().equals("Shared Memory")) {
							ipc = IPCMechanismJava.SharedMemory;
						}

						String outputDir = p.getOptionOutputDirectory().equals("") ? null
								: p.getOptionOutputDirectory();
						String base = p.getOptionBasePackageName().equals("") ? null : p.getOptionBasePackageName();

						return org.sireum.aadl.arsit.Arsit.run(
								model,
								ArsitBridge.sireumOption(outputDir),
								ArsitBridge.sireumOption(base),
								p.getOptionEmbedArt(),
								p.getOptionGenerateBlessEntryPoints(),
								p.getOptionGenerateTranspilerArtifacts(),
								ipc
								);
						});

					refreshWorkspace();

					Dialog.showInfo(getToolName(), "Slang-Embedded code "
							+ (ret == 0 ? "successfully generated" : "generation was unsuccessful"));

				} catch (Throwable ex) {
					ex.printStackTrace();
					String m = "Could not generate Slang-Embedded code.  Please make sure Arsit is present.\n\n"
							+ ex.getLocalizedMessage();
					Dialog.showError(getToolName(), m);
					return Status.CANCEL_STATUS;
				}
			}
		}

		return Status.OK_STATUS;
	}
}
