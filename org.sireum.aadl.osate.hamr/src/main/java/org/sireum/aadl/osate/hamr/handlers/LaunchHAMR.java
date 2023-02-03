package org.sireum.aadl.osate.hamr.handlers;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import org.sireum.aadl.osate.architecture.BlessBehaviorProvider;
import org.sireum.aadl.osate.architecture.BlessDatatypeProvider;
import org.sireum.aadl.osate.architecture.BlessEntrypointProvider;
import org.sireum.aadl.osate.hamr.HAMRPluginUtil;
import org.sireum.aadl.osate.hamr.PreferenceValues;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.HW;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.Platform;
import org.sireum.aadl.osate.hamr.handlers.HAMRUtil.ErrorReport;
import org.sireum.aadl.osate.hamr.handlers.HAMRUtil.Report;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;
import org.sireum.aadl.osate.util.ApiUtil;
import org.sireum.aadl.osate.util.SlangUtil;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SeverityLevel;
import org.sireum.aadl.osate.util.VisitorUtil;
import org.sireum.hamr.arsit.ArsitBridge;
import org.sireum.hamr.ir.Aadl;
import org.sireum.message.Reporter;

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

		if (!Util.emitSireumVersion(console) || //
				!(ApiUtil.hamrCliApiCompatible(new PrintStream(console.newMessageStream())))) {
			displayPopup("HAMR code generation was unsuccessful");
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

			final int bit_width = HAMRPropertyProvider.getDefaultBitWidthFromElement(si);
			if (!HAMRPropertyProvider.bitWidths.contains(bit_width)) {
				String options = HAMRPropertyProvider.bitWidths.stream()
						.map(Object::toString)
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

			if (PreferenceValues.HAMR_SERIALIZE_AIR_OPT.getValue()) {
				File f = serializeToFile(model, PreferenceValues.HAMR_AIR_OUTPUT_FOLDER_OPT.getValue(), si);
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

					List<Report> report = HAMRUtil.checkModel(si, prompt);

					for (Report r : report) {
						writeToConsole(r.toString());
						if (r instanceof ErrorReport) {
							toolRet = 1;
						}
					}

					if (toolRet == 0) {

						writeToConsole("Generating " + getToolName() + " artifacts...");

						final File workspaceRoot = getProjectPath(si).toFile();

						final org.sireum.String _slangOutputDir = prompt.getSlangOptionOutputDirectory().equals("") //
								? new org.sireum.String(workspaceRoot.getAbsolutePath())
								: new org.sireum.String(prompt.getSlangOptionOutputDirectory());

						final org.sireum.String _base = prompt.getOptionBasePackageName().equals("") //
								? new org.sireum.String(
										HAMRUtil.cleanupPackageName(new File(_slangOutputDir.string()).getName()))
								: new org.sireum.String(HAMRUtil.cleanupPackageName(prompt.getOptionBasePackageName()));

						final org.sireum.String _cOutputDirectory = prompt.getOptionCOutputDirectory().equals("") //
								? null
								: new org.sireum.String(prompt.getOptionCOutputDirectory());

						org.sireum.String _camkesOutputDir = prompt.getOptionCamkesOptionOutputDirectory().equals("") //
								? null
								: new org.sireum.String(prompt.getOptionCamkesOptionOutputDirectory());

						toolRet = Util.callWrapper(getToolName(), console, () -> {

							boolean verbose = PreferenceValues.HAMR_VERBOSE_OPT.getValue();
							String platform = prompt.getOptionPlatform().hamrName();
							Option<org.sireum.String> slangOutputDir = ArsitBridge.sireumOption(_slangOutputDir);
							Option<org.sireum.String> slangPackageName = ArsitBridge.sireumOption(_base);
							boolean noProyekIve = !PreferenceValues.HAMR_RUN_PROYEK_IVE_OPT.getValue();
							boolean noEmbedArt = !PreferenceValues.HAMR_EMBED_ART_OPT.getValue();
							boolean devicesAsThreads = PreferenceValues.HAMR_DEVICES_AS_THREADS_OPT.getValue();
							boolean genSbtMill = PreferenceValues.HAMR_GEN_SBT_MILL_OPT.getValue();
							IS<Z, org.sireum.String> slangAuxCodeDirs = prompt.getOptionCAuxSourceDirectory().equals("")
									? VisitorUtil.toISZ()
									: VisitorUtil.toISZ(new org.sireum.String(prompt.getOptionCAuxSourceDirectory()));
							Option<org.sireum.String> slangOutputCDirectory = ArsitBridge
									.sireumOption(_cOutputDirectory);
							boolean excludeComponentImpl = prompt.getOptionExcludesSlangImplementations();
							Z bitWidth = SlangUtil.toZ(prompt.getOptionBitWidth());
							Z maxStringSize = SlangUtil.toZ(prompt.getOptionMaxStringSize());
							Z maxArraySize = SlangUtil.toZ(prompt.getOptionMaxSequenceSize());
							boolean runTranspiler = PreferenceValues.HAMR_RUN_TRANSPILER_OPT.getValue();
							Option<org.sireum.String> camkesOutputDirectory = ArsitBridge
									.sireumOption(_camkesOutputDir);
							IS<Z, org.sireum.String> camkesAuxCodeDirs = prompt.getOptionCamkesAuxSrcDir().equals("")
									? VisitorUtil.toISZ()
									: VisitorUtil.toISZ(new org.sireum.String(prompt.getOptionCamkesAuxSrcDir()));
							Option<org.sireum.String> aadlRootDir = ArsitBridge
									.sireumOption(new org.sireum.String(workspaceRoot.getAbsolutePath()));

							List<org.sireum.String> exOptions = new ArrayList<>();

							exOptions.add(new org.sireum.String("PROCESS_BTS_NODES"));

							if (PreferenceValues.HAMR_PROOF_GENERATE.getValue()) {
								exOptions.add(new org.sireum.String("GENERATE_REFINEMENT_PROOF"));
							}

							IS<Z, org.sireum.String> experimentalOptions = VisitorUtil.toISZ(exOptions);
//add BLESS provider plugins
//get plugins from Eclipse extension points							
							IS<Z, org.sireum.hamr.codegen.common.plugin.Plugin> 
							  plugins = VisitorUtil.toISZ(
							      HAMRPluginUtil.getHamrPlugins(si)
//							      new BlessEntrypointProvider(si), 
//							      new BlessBehaviorProvider(si),
//							      new BlessDatatypeProvider()
							      );
							
							Z codegenRet = org.sireum.cli.HAMR.codeGenP( //
									model, //
									//
									verbose, //
									org.sireum.Cli.SireumHamrCodegenHamrPlatform$.MODULE$.byName(platform).get(), //
									slangOutputDir, //
									slangPackageName, //
									//
									noProyekIve, //
									noEmbedArt, //
									devicesAsThreads, //
									genSbtMill, //
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
									experimentalOptions,

									plugins,
									
									reporter);

							// only propagate error messages to eclipse's problem view (all messages are emitted
							// to the console view)
							Util.addMarkers(PreferenceValues.HAMR_MARKER_ID, VisitorUtil.toIList(SeverityLevel.Error),
									si, reporter);

							return codegenRet.toInt();
						});

						if (toolRet == 0 && PreferenceValues.HAMR_PROOF_GENERATE.getValue()
								&& (prompt.getOptionPlatform() == Platform.seL4
										|| prompt.getOptionPlatform() == Platform.seL4_Only)) {
							String sep = File.separator;
							File smt2FileLocation = new File(new File(_slangOutputDir.string()),
									"src" + sep + "c" + sep + "camkes" + sep + "proof" + sep + "smt2_case.smt2");
							if (_camkesOutputDir != null) {
								smt2FileLocation = new File(new File(_camkesOutputDir.string()),
										"proof" + sep + "smt2_case.smt2");
							}

							// or perhaps store the path in the eclipse store
							ProofUtil.lastSMT2Proof = smt2FileLocation;

							if (PreferenceValues.HAMR_PROOF_CHECK.getValue()) {
								PrintStream out = new PrintStream(console.newMessageStream());

								toolRet = ProofUtil.checkProof(smt2FileLocation, out);

								out.close();
							}
						}
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
		} else {
			Dialog.showError(getToolName(), "AIR generation failed");
			writeToConsole("AIR generation failed");

			Util.addMarkers(PreferenceValues.HAMR_MARKER_ID, VisitorUtil.toIList(), si, reporter);

			return Status.CANCEL_STATUS;
		}

		return Status.OK_STATUS;
	}

}
