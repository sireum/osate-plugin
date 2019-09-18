package org.sireum.aadl.osate.hamr.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

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
import org.sireum.Option;
import org.sireum.Z;
import org.sireum.aadl.arsit.ArsitBridge;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.hamr.PreferenceValues;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.HW;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.Platform;
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

			final int bit_width = HAMRPropertyProvider.getDefaultBitWidthFromElement(si);
			if (!HAMRPropertyProvider.bitWidths.contains(bit_width)) {
				String options = HAMRPropertyProvider.bitWidths.stream().map(Object::toString)
						.collect(Collectors.joining(", "));
				displayPopup("Invalid bit width: " + bit_width + ".  Valid options are " + options);
				return Status.CANCEL_STATUS;
			}

			final int max_seq_size = HAMRPropertyProvider.getDefaultMaxSequenceSizeFromElement(si);
			if (max_seq_size < 0) {
				displayPopup("Max sequence size must be greater than or equal to 0");
				return Status.CANCEL_STATUS;
			}

			final int max_string_size = HAMRPropertyProvider.getDefaultMaxStringSizeFromElement(si);
			if (max_string_size < 0) {
				displayPopup("Max string size must be greater than or equal to 0");
				return Status.CANCEL_STATUS;
			}

			List<Platform> platforms = HAMRPropertyProvider.getPlatformsFromElement(si);
			List<HW> hardwares = HAMRPropertyProvider.getHWsFromElement(si);

			if (PreferenceValues.getHAMR_SERIALIZE_OPT()) {
				File f = serializeToFile(model, PreferenceValues.getHAMR_OUTPUT_FOLDER_OPT(), si);
				writeToConsole("Wrote: " + f.getAbsolutePath());
			}

			Display.getDefault().syncExec(() -> {
				prompt = new HAMRPrompt(getProject(si), getShell(), si.getComponentImplementation().getFullName(),
						platforms, hardwares, bit_width, max_seq_size, max_string_size);
				prompt.open();
			});

			if (prompt.getReturnCode() == Window.OK) {
				try {

					File workspaceRoot = getProjectPath(si).toFile();

					String slangOutputDir = prompt.getSlangOptionOutputDirectory().equals("")
							? workspaceRoot.getAbsolutePath()
							: prompt.getSlangOptionOutputDirectory();

					writeToConsole("Generating " + getToolName() + " artifacts...");

					int toolRet = 0;

					if (!prompt.getOptionTrustedBuildProfile()) {
						Util.callWrapper(getToolName(), console, () -> {

							assert (!prompt.getOptionTrustedBuildProfile()); // don't run arsit if TB profile choosen

							String _behaviorDir = prompt.getOptionCSourceDirectory().equals("") ? null
									: prompt.getOptionCSourceDirectory();

							String _base = prompt.getOptionBasePackageName().equals("") ? null
									: prompt.getOptionBasePackageName();

							String _cDir = new File(slangOutputDir,
									prompt.getOptionPlatform() == ArsitBridge.Platform.Sel4 ? "src/c/sel4"
											: "src/c/nix").getAbsolutePath();

							Option<String> optOutputDir = ArsitBridge.sireumOption(slangOutputDir);
							Option<String> optBasePackageName = ArsitBridge.sireumOption(_base);
							boolean embedArt = PreferenceValues.getHAMR_EMBED_ART_OPT();
							boolean genBlessEntryPoints = false;
							boolean verbose = PreferenceValues.getHAMR_VERBOSE_OPT();
							boolean devicesAsThreads = PreferenceValues.getHAMR_DEVICES_AS_THREADS_OPT();
							ArsitBridge.IPCMechanism ipcMechanism = ArsitBridge.IPCMechanism.SharedMemory;
							boolean excludeImpl = prompt.getOptionExcludesSlangImplementations();
							Option<String> behaviorDir = ArsitBridge.sireumOption(_behaviorDir);
							Option<String> outputCDir = ArsitBridge.sireumOption(_cDir);
							ArsitBridge.Platform platform = prompt.getOptionPlatform();
							int bitWidth = bit_width;
							int maxStringSize = max_string_size;
							int maxArraySize = max_seq_size;

							return org.sireum.aadl.arsit.Arsit.run( //
									model, optOutputDir, optBasePackageName, embedArt, genBlessEntryPoints, verbose,
									devicesAsThreads, ipcMechanism, excludeImpl, behaviorDir,
									outputCDir, platform, bitWidth, maxStringSize, maxArraySize);
						});
					}

					if (toolRet == 0) {

						String transpilerScript = slangOutputDir + "/bin/transpile.sh";

						if (prompt.getOptionPlatform() == ArsitBridge.Platform.Sel4) {
							transpilerScript = slangOutputDir + "/bin/transpile-camkes.sh";

							BufferedWriter writer = new BufferedWriter(new FileWriter(transpilerScript, true));
							writer.write("\n\nFILE=$OUTPUT_DIR/CMakeLists.txt\n");
							writer.write("echo -e \"\\n\\nadd_definitions(-DCAMKES)\" >> $FILE");
							writer.close();
						}

						// run the transpiler

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

						if (prompt.getOptionPlatform() == ArsitBridge.Platform.Sel4) {

							File camkesOutDir = new File(prompt.getOptionCamkesOptionOutputDirectory());

							camkesOutDir.mkdirs(); // FIXME should be done in ACT

							writeToConsole("\nGenerating CAmkES artifacts ...");

							// run ACT
							toolRet = Util.callWrapper(getToolName(), console, () -> {

								// FIXME: aux_code contains the ipc for camkes+slang along
								// with the data conversions. ipc.c could be placed in camkes'
								// includes dir and the converts won't be needed when we
								// switch to slang derived types
								IS<Z, String> auxDirs = toISZ(
										"/home/sireum/uav-project-extern/src/aadl/ACT_Demo_Dec2018/aux_code");

								org.sireum.Option<File> aadlRootDir = new org.sireum.Some<>(workspaceRoot);

								return org.sireum.aadl.act.Act.run(camkesOutDir, model, auxDirs, aadlRootDir);
							});
						}
					}

					String msg = "HAMR code "
							+ (toolRet == 0 ? "successfully generated" : "generation was unsuccessful");
					displayPopup(msg);

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
			writeToConsole(msg);
			AbstractNotificationPopup notification = new EclipseNotification(Display.getCurrent(),
					getToolName() + " Message", msg);
			notification.open();
		});
	}

	private <T> IS<Z, T> toISZ(T... args) {
		scala.collection.Seq<T> seq = scala.collection.JavaConverters.asScalaBuffer(java.util.Arrays.asList(args));
		IS<Z, T> ret = org.sireum.IS$.MODULE$.apply(seq, org.sireum.Z$.MODULE$);
		return ret;
	}
}
