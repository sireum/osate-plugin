package org.sireum.aadl.osate.hamr.handlers;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.ui.dialogs.Dialog;
import org.sireum.IS;
import org.sireum.Option;
import org.sireum.Z;
import org.sireum.aadl.osate.hamr.PreferenceValues;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.HW;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.Platform;
import org.sireum.aadl.osate.hamr.handlers.HAMRUtil.ErrorReport;
import org.sireum.aadl.osate.hamr.handlers.HAMRUtil.Report;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;
import org.sireum.aadl.osate.util.Util;
import org.sireum.hamr.arsit.ArsitBridge;
import org.sireum.hamr.ir.Aadl;

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
					int toolRet = 0;

					final boolean targetingSel4 = prompt.getOptionPlatform() == ArsitBridge.Platform.SeL4;

					List<Report> report = HAMRUtil.checkModel(si, prompt);

					for (Report r : report) {
						writeToConsole(r.toString());
						if (r instanceof ErrorReport) {
							toolRet = 1;
						}
					}

					final File workspaceRoot = getProjectPath(si).toFile();

					final String slangOutputDir = prompt.getSlangOptionOutputDirectory().equals("")
							? workspaceRoot.getAbsolutePath()
							: prompt.getSlangOptionOutputDirectory();

					final String _base = prompt.getOptionBasePackageName().equals("")
							? HAMRUtil.cleanupPackageName(new File(slangOutputDir).getName())
							: HAMRUtil.cleanupPackageName(prompt.getOptionBasePackageName());

					final String outputCDirectory = targetingSel4
							? new File(prompt.getOptionCamkesOptionOutputDirectory(), "hamr").getAbsolutePath()
							: new File(slangOutputDir, "src/c/nix").getAbsolutePath();

					if (toolRet == 0 && !prompt.getOptionTrustedBuildProfile()) {

						writeToConsole("Generating " + getToolName() + " artifacts...");

						Util.callWrapper(getToolName(), console, () -> {

							String _behaviorDir = prompt.getOptionCSourceDirectory().equals("") ? null
									: prompt.getOptionCSourceDirectory();

							Option<String> optOutputDir = ArsitBridge.sireumOption(slangOutputDir);
							Option<String> optBasePackageName = ArsitBridge.sireumOption(_base);
							boolean embedArt = PreferenceValues.getHAMR_EMBED_ART_OPT();
							boolean genBlessEntryPoints = false;
							boolean verbose = PreferenceValues.getHAMR_VERBOSE_OPT();
							boolean devicesAsThreads = PreferenceValues.getHAMR_DEVICES_AS_THREADS_OPT();
							ArsitBridge.IPCMechanism ipcMechanism = ArsitBridge.IPCMechanism.SharedMemory;
							boolean excludeImpl = prompt.getOptionExcludesSlangImplementations();
							Option<String> behaviorDir = ArsitBridge.sireumOption(_behaviorDir);
							Option<String> outputCDir = ArsitBridge.sireumOption(outputCDirectory);
							ArsitBridge.Platform platform = prompt.getOptionPlatform();
							int bitWidth = bit_width;
							int maxStringSize = max_string_size;
							int maxArraySize = max_seq_size;

							return org.sireum.hamr.arsit.Arsit.run( //
									model, optOutputDir, optBasePackageName, embedArt, genBlessEntryPoints, verbose,
									devicesAsThreads, ipcMechanism, excludeImpl, behaviorDir, outputCDir, platform,
									bitWidth, maxStringSize, maxArraySize);
						});
					}

					if (toolRet == 0 && HAMRUtil.shouldTranspile(prompt)) {

						String sireumHome = PreferenceValues.getHAMR_SIREUM_HOME();
						if (sireumHome.equals("") || !new File(sireumHome).exists()) {
							writeToConsole("SIREUM_HOME not set.");
							writeToConsole(
									"Install Sireum (https://github.com/sireum/kekinian#installing) and then add its install directory to \"Sireum HAMR >> Code Generation >> SIREUM_HOME\"");

							toolRet = 1;
						}

						String transpilerScript = slangOutputDir
								+ (prompt.getOptionPlatform() == ArsitBridge.Platform.SeL4 ? "/bin/transpile-sel4.sh"
										: "/bin/transpile.sh");

						String[] commands = new String[] { "chmod", "700", transpilerScript };

						if (toolRet == 0) {
							toolRet = HAMRUtil.invoke(console, commands);
						}

						if (toolRet == 0) {
							commands = new String[] { transpilerScript };
							toolRet = HAMRUtil.invoke(console, commands);
						}
					}

					if (toolRet == 0 && prompt.getOptionPlatform() == ArsitBridge.Platform.SeL4) {

						// run ACT
						toolRet = Util.callWrapper(getToolName(), console, () -> {

							File _camkesOutDir = new File(prompt.getOptionCamkesOptionOutputDirectory());
							_camkesOutDir.mkdirs();

							Option<String> camkesOutDir = ArsitBridge.sireumOption(_camkesOutDir.getAbsolutePath());

							writeToConsole("\nGenerating CAmkES artifacts ...");

							IS<Z, String> auxDirs = prompt.getOptionCamkesAuxSrcDir().equals("") ? HAMRUtil.toISZ()
									: HAMRUtil.toISZ(prompt.getOptionCamkesAuxSrcDir());

							Option<String> aadlRootDir = ArsitBridge.sireumOption(workspaceRoot.getAbsolutePath());

							boolean hamrIntegration = !prompt.getOptionTrustedBuildProfile();

							IS<Z, String> hamrIncludeDirs = HAMRUtil.toISZ(outputCDirectory);

							String hsl = new File(outputCDirectory, "sel4-build/libmain.a").getAbsolutePath();

							org.sireum.Option<String> hamrStaticLib = new org.sireum.Some<>(hsl);

							Option<String> hamrBasePackageName = ArsitBridge.sireumOption(_base);

							return org.sireum.hamr.act.Act.run(camkesOutDir, model, auxDirs, aadlRootDir,
									hamrIntegration, hamrIncludeDirs, hamrStaticLib, hamrBasePackageName);
						});

					}

					String msg = "HAMR code "
							+ (toolRet == 0 ? "successfully generated" : "generation was unsuccessful");
					displayPopup(msg);

					refreshWorkspace();

				} catch (Throwable ex) {
					ex.printStackTrace();
					displayPopup("Error encountered while running HAMR.\n\n" + ex.getLocalizedMessage());
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
}