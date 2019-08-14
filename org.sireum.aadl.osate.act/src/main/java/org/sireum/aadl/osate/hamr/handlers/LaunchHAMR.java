package org.sireum.aadl.osate.hamr.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.ui.dialogs.Dialog;
import org.sireum.IS;
import org.sireum.Z;
import org.sireum.aadl.arsit.ArsitBridge;
import org.sireum.aadl.arsit.ArsitBridge.IPCMechanismJava;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.hamr.PreferenceValues;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;
import org.sireum.aadl.osate.util.Util;

public class LaunchHAMR extends AbstractSireumHandler {

	private HAMRPrompt prompt = null;

	@Override
	public String getToolName() {
		return "HAMR";
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

			if (PreferenceValues.getHAMR_SERIALIZE_OPT()) {
				File f = serializeToFile(model, PreferenceValues.getHAMR_OUTPUT_FOLDER_OPT(), si);
				writeToConsole("Wrote: " + f.getAbsolutePath());
			}

			Display.getDefault().syncExec(() -> {
				prompt = new HAMRPrompt(getProject(si), getShell());
				prompt.open();
			});

			if (prompt.getReturnCode() == Window.OK) {
				try {
					File workspaceRoot = getProjectPath(si).toFile();

					String slangOutputDir = prompt.getSlangOptionOutputDirectory().equals("") ? null
							: prompt.getSlangOptionOutputDirectory();

					// always run Arsit
					int toolRet = Util.callWrapper(getToolName(), console, () -> {

						writeToConsole("Generating ART artifacts...");

						// always gen shared mem for HAMR
						ArsitBridge.IPCMechanismJava ipc = IPCMechanismJava.SharedMemory;

						String base = null;

						return org.sireum.aadl.arsit.Arsit.run( //
								model, //
								ArsitBridge.sireumOption(slangOutputDir), //
								ArsitBridge.sireumOption(base), //
								true, // always embed ART
								false, // never gen bless entrypoints
								false, // no verbose
								true, // always gen transpiler artifacts
								ipc, //
								prompt.getOptionExcludesSlangImplementations(),
								prompt.getOptionOutputProfile() == OutputProfile.seL4 // removes ipc.c if true
						);
					});

					if (toolRet == 0) {
						String transpilerScript = slangOutputDir + "/bin/transpile.sh";

						if (prompt.getOptionOutputProfile() == OutputProfile.seL4) {
							writeToConsole("Generating CAmkES artifacts...");

							transpilerScript = slangOutputDir + "/bin/transpile-camkes.sh";
							{
								BufferedWriter writer = new BufferedWriter(new FileWriter(transpilerScript, true));
								writer.write("\n\nFILE=$OUTPUT_DIR/CMakeLists.txt\n");
								writer.write("echo -e \"\\n\\nadd_definitions(-DCAMKES)\" >> $FILE");
								writer.close();
							}

							// run ACT
							toolRet = Util.callWrapper(getToolName(), console, () -> {
								File outDir = new File(prompt.getCamkesOptionOutputDirectory());
								IS<Z, String> auxDirs = toISZ(prompt.getOptionCSourceDirectory());
								org.sireum.Option<File> aadlRootDir = new org.sireum.Some<>(workspaceRoot);

								return org.sireum.aadl.act.Act.run(outDir, model, auxDirs, aadlRootDir);
							});
						}

						if (toolRet == 0) {
							// run the transpiler

							writeToConsole("Running " + transpilerScript);

							ProcessBuilder pb = new ProcessBuilder("/bin/bash", "--login", "-c", transpilerScript);

							pb.redirectErrorStream(true);
							pb.environment().put("HOME", "/home/sireum");
							pb.environment().put("PATH",
									"/home/sireum/devel/sireum/kekinian/bin:/home/sireum/devel/sireum/kekinian/bin/linux/java/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:.");
							pb.environment().put("SIREUM_HOME", "/home/sireum/devel/sireum/kekinian");

							InputStream is = pb.start().getInputStream();

							MessageConsoleStream mcs = console.newMessageStream();

							int c;
							while ((c = is.read()) != -1) {
								mcs.write(c);
							}
						}
					}

					displayPopup(
							"HAMR code " + (toolRet == 0 ? "successfully generated" : "generation was unsuccessful"));

					refreshWorkspace();

				} catch (Throwable ex) {
					ex.printStackTrace();
					displayPopup("Could not generate CAmkES.  Please make sure HAMR is present.\n\n"
							+ ex.getLocalizedMessage());
					return Status.CANCEL_STATUS;
				}
			}
		}

		return Status.OK_STATUS;
	}

	private void displayPopup(String msg) {
		Display.getDefault().syncExec(() -> {
			AbstractNotificationPopup notification = new EclipseNotification(Display.getCurrent(), "HAMR Message", msg);
			notification.open();
		});
	}

	private <T> IS<Z, T> toISZ(T... args) {
		scala.collection.Seq<T> seq = scala.collection.JavaConverters.asScalaBuffer(java.util.Arrays.asList(args));
		IS<Z, T> ret = org.sireum.IS$.MODULE$.apply(seq, org.sireum.Z$.MODULE$);
		return ret;
	}
}
