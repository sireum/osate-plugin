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
import org.sireum.IS;
import org.sireum.Option;
import org.sireum.Z;
import org.sireum.aadl.osate.act.PreferenceValues;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;
import org.sireum.aadl.osate.util.Util;
import org.sireum.hamr.ir.Aadl;

public class LaunchAct extends AbstractSireumHandler {

	private ActPrompt prompt = null;

	@Override
	public String getToolName() {
		return "ACT";
	}

	@Override
	public IStatus runJob(Element elem, IProgressMonitor monitor) {

		prompt = null;

		MessageConsole console = displayConsole();
		console.clearConsole();

		SystemInstance si = getSystemInstance(elem);
		if (si == null) {
			Dialog.showError(getToolName(), "Please select a system implementation or a system instance");
			return Status.CANCEL_STATUS;
		}

		writeToConsole("Generating AIR ...");

		Aadl model = Util.getAir(si, true, console);

		if (model != null) {

			if (PreferenceValues.getACT_SERIALIZE_OPT()) {
				File f = serializeToFile(model, PreferenceValues.getACT_OUTPUT_FOLDER_OPT(), si);
				writeToConsole("Wrote: " + f.getAbsolutePath());
			}

			Display.getDefault().syncExec(() -> {
				prompt = new ActPrompt(getProject(si), getShell());
				prompt.open();
			});

			if (prompt.getReturnCode() == Window.OK) {
				try {

					File out = new File(prompt.getOptionOutputDirectory());
					if (!out.exists()) {
						if (Dialog.askQuestion("Create Directory?",
								"Directory '" + out.getAbsolutePath() + "' does not exist.  Should it be created?")) {
							if (!out.mkdirs()) {
								Dialog.showError(getToolName(), "Could not create directory " + out.getAbsolutePath());
								return Status.CANCEL_STATUS;
							}
						} else {
							return Status.CANCEL_STATUS;
						}
					}
					File workspaceRoot = getProjectPath(si).toFile();

					int toolRet = Util.callWrapper(getToolName(), console, () -> {
						File outDirFile = new File(prompt.getOptionOutputDirectory());
						Option<String> outDir = new org.sireum.Some<String>(outDirFile.getAbsolutePath());
						IS<Z, String> auxDirs = toISZ(prompt.getOptionCSourceDirectory());
						Option<String> aadlRootDir = new org.sireum.Some<String>(workspaceRoot.getAbsolutePath());

						return org.sireum.hamr.act.Act.run(outDir, model, auxDirs, aadlRootDir);
					});

					refreshWorkspace();

					Dialog.showInfo(getToolName(),
							"CAmkES code " + (toolRet == 0 ? "successfully generated" : "generation was unsuccessful"));

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

	private <T> IS<Z, T> toISZ(T... args) {
		scala.collection.Seq<T> seq = scala.collection.JavaConverters.asScalaBuffer(java.util.Arrays.asList(args));
		IS<Z, T> ret = org.sireum.IS$.MODULE$.apply(seq, org.sireum.Z$.MODULE$);
		return ret;
	}
}
