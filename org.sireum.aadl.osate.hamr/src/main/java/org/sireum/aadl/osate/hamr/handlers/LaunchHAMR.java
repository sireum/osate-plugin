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
import org.sireum.SireumApi;
import org.sireum.Z;
import org.sireum.aadl.osate.architecture.VisitorUtil;
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

		writeToConsole("Sireum Version: " + SireumApi.version());

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

					final boolean targetingSel4 = prompt.getOptionPlatform() == Platform.seL4;

					List<Report> report = HAMRUtil.checkModel(si, prompt);

					for (Report r : report) {
						writeToConsole(r.toString());
						if (r instanceof ErrorReport) {
							toolRet = 1;
						}
					}

					if (toolRet == 0) {

						writeToConsole("Generating " + getToolName() + " artifacts...");

						toolRet = Util.callWrapper(getToolName(), console, () -> {
							final File workspaceRoot = getProjectPath(si).toFile();

							final String _slangOutputDir = prompt.getSlangOptionOutputDirectory().equals("") //
									? workspaceRoot.getAbsolutePath()
									: prompt.getSlangOptionOutputDirectory();

							final String _base = prompt.getOptionBasePackageName().equals("") //
									? HAMRUtil.cleanupPackageName(new File(_slangOutputDir).getName())
									: HAMRUtil.cleanupPackageName(prompt.getOptionBasePackageName());

							final String _cOutputDirectory = prompt.getOptionCOutputDirectory().equals("") //
									? null
									: prompt.getOptionCOutputDirectory();

							String _camkesOutputDir = prompt.getOptionCamkesOptionOutputDirectory().equals("") //
									? null
									: prompt.getOptionCamkesOptionOutputDirectory();

							boolean verbose = PreferenceValues.getHAMR_VERBOSE_OPT();
							String platform = prompt.getOptionPlatform().hamrName();
							Option<String> slangOutputDir = ArsitBridge.sireumOption(_slangOutputDir);
							Option<String> slangPackageName = ArsitBridge.sireumOption(_base);
							boolean noEmbedArt = !PreferenceValues.getHAMR_EMBED_ART_OPT();
							boolean devicesAsThreads = PreferenceValues.getHAMR_DEVICES_AS_THREADS_OPT();
							IS<Z, String> slangAuxCodeDirs = prompt.getOptionCAuxSourceDirectory().equals("")
									? VisitorUtil.toISZ()
									: VisitorUtil.toISZ(prompt.getOptionCAuxSourceDirectory());
							Option<String> slangOutputCDirectory = ArsitBridge.sireumOption(_cOutputDirectory);
							boolean excludeComponentImpl = prompt.getOptionExcludesSlangImplementations();
							int bitWidth = prompt.getOptionBitWidth();
							int maxStringSize = prompt.getOptionMaxStringSize();
							int maxArraySize = prompt.getOptionMaxSequenceSize();
							boolean runTranspiler = PreferenceValues.getHAMR_RUN_TRANSPILER();
							Option<String> camkesOutputDirectory = ArsitBridge.sireumOption(_camkesOutputDir);
							IS<Z, String> camkesAuxCodeDirs = prompt.getOptionCamkesAuxSrcDir().equals("")
									? VisitorUtil.toISZ()
									: VisitorUtil.toISZ(prompt.getOptionCamkesAuxSrcDir());
							Option<String> aadlRootDir = ArsitBridge.sireumOption(workspaceRoot.getAbsolutePath());

							IS<Z, String> experimentalOptions = org.sireum.aadl.osate.PreferenceValues.getPROCESS_BA_OPT()
									? VisitorUtil.toISZ("PROCESS_BTS_NODES")
									: VisitorUtil.toISZ();

							return org.sireum.cli.HAMR.codeGen( //
									model, //
									//
									verbose, //
									org.sireum.Cli.HamrPlatform$.MODULE$.byName(platform).get(), //
									slangOutputDir, //
									slangPackageName, //
									noEmbedArt, //
									devicesAsThreads, //
									//
									slangAuxCodeDirs, //
									slangOutputCDirectory, //
									excludeComponentImpl, //
									bitWidth, //
									maxStringSize, //
									maxArraySize, //
									runTranspiler, //
									//
									camkesOutputDirectory, //
									camkesAuxCodeDirs, //
									aadlRootDir, //
									//
									experimentalOptions);
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
