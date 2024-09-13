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
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.ui.dialogs.Dialog;
import org.sireum.IS;
import org.sireum.MS;
import org.sireum.Option;
import org.sireum.Z;
import org.sireum.Cli.SireumTopOption;
import org.sireum.aadl.osate.hamr.PreferenceValues;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.HW;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.Platform;
import org.sireum.aadl.osate.hamr.handlers.HAMRUtil.ErrorReport;
import org.sireum.aadl.osate.hamr.handlers.HAMRUtil.Report;
import org.sireum.aadl.osate.hamr.plugin.HAMRPluginUtil;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;
import org.sireum.aadl.osate.util.SlangUtil;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SeverityLevel;
import org.sireum.aadl.osate.util.VisitorUtil;
import org.sireum.hamr.arsit.ArsitBridge;
import org.sireum.hamr.arsit.plugin.ArsitPlugin;
import org.sireum.hamr.codegen.common.plugin.Plugin;
import org.sireum.hamr.codegen.LongKeys;
import org.sireum.hamr.codegen.CodeGenJavaFactory;
import org.sireum.hamr.ir.Aadl;
import org.sireum.message.Reporter;

public class LaunchHAMR extends AbstractSireumHandler {
	private HAMRPrompt prompt = null;

	@Override
	public String getToolName() {
		return "HAMR";
	}

	private NamedElement lastElem = null;

	@Override
	public IStatus runJob(Element elem, IProgressMonitor monitor) {
		prompt = null;

		MessageConsole console = displayConsole();
		console.clearConsole();

		if (!Util.emitSireumVersion(console)) { 
				//|| //
				//!(ApiUtil.hamrCliApiCompatible(new PrintStream(console.newMessageStream())))) {
			displayPopup("HAMR code generation was unsuccessful");
			return Status.CANCEL_STATUS;
		}

		SystemInstance siTemp = getSystemInstance(elem);
		if (siTemp == null) {
			if (lastElem == null) {
				Dialog.showError(getToolName(), "Please select a system implementation or a system instance");
				return Status.CANCEL_STATUS;
			} else {
				writeToConsole(
						"Could not generate a system instance from the current selection so re-running HAMR codegen on "
								+ lastElem.getQualifiedName());

				siTemp = getSystemInstance(lastElem);

				if (siTemp == null) {
					Dialog.showError(getToolName(),
							"Could not generate a system instance from " + lastElem.getQualifiedName());
					return Status.CANCEL_STATUS;
				}
			}
		} else {
			lastElem = (NamedElement) elem;
		}

		final SystemInstance si = siTemp;

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

			// TODO: provide mechanism to disable gumbo plugin when bless2air is present
			MS<Z, Plugin> eplugins = VisitorUtil.toISZ(HAMRPluginUtil.getHamrPlugins(si)).toMSZ();
			final MS<Z, Plugin> hamrPlugins = eplugins.isEmpty() //
					? ArsitPlugin.gumboEnhancedPlugins()
					: eplugins.$plus$plus(ArsitPlugin.defaultPlugins());

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

						final org.sireum.String _camkesOutputDir = prompt.getOptionCamkesOptionOutputDirectory()
								.equals("") //
										? null
										: new org.sireum.String(prompt.getOptionCamkesOptionOutputDirectory());
						final org.sireum.String _ros2OutputWorkspaceDir = prompt.getOptionRos2OutputWorkspaceDirectory().equals("") //
								? null
								: new org.sireum.String(prompt.getOptionRos2OutputWorkspaceDirectory());	
						final org.sireum.String _ros2Ros2Dir = prompt.getOptionRos2Directory().equals("") //
								? null
										: new org.sireum.String(prompt.getOptionRos2Directory());
						
						final File ideaDir = new File(_slangOutputDir.string() + File.separator + ".idea");

						toolRet = Util.callWrapper(getToolName(), console, () -> {

							boolean verbose = PreferenceValues.HAMR_VERBOSE_OPT.getValue();
							boolean runtimeMonitoring = prompt.getOptionEnableRuntimeMonitoring();
							String platform = prompt.getOptionPlatform().name();
							Option<org.sireum.String> slangOutputDir = ArsitBridge.sireumOption(_slangOutputDir);
							Option<org.sireum.String> slangPackageName = ArsitBridge.sireumOption(_base);
							boolean noProyekIve = !PreferenceValues.HAMR_RUN_PROYEK_IVE_OPT.getValue()
									|| ideaDir.exists();
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
							Option<org.sireum.String> workspaceRootDir = ArsitBridge
									.sireumOption(new org.sireum.String(workspaceRoot.getAbsolutePath()));

							boolean strictAadlMode = prompt.getOptionRos2StrictAadlMode();
							Option<org.sireum.String> ros2OutputWorkspaceDir = ArsitBridge.sireumOption(_ros2OutputWorkspaceDir);
							Option<org.sireum.String> ros2Dir = ArsitBridge.sireumOption(_ros2Ros2Dir);
							String ros2NodesLanguage = prompt.getOptionRos2NodesLanguage().name();
							String ros2LaunchLanguage = prompt.getOptionRos2LaunchLanguage().name();
							
							List<org.sireum.String> exOptions = new ArrayList<>();
							exOptions.add(new org.sireum.String("PROCESS_BTS_NODES"));

							if (PreferenceValues.HAMR_PROOF_GENERATE.getValue()) {
								exOptions.add(new org.sireum.String("GENERATE_REFINEMENT_PROOF"));
							}

							IS<Z, org.sireum.String> experimentalOptions = VisitorUtil.toISZ(exOptions);

							// build the argument sequence 
							IS<Z, org.sireum.String> args = VisitorUtil.toISZ();
							args = args.$colon$plus(s("hamr")).$colon$plus(s("codegen"));

							if (verbose) {
								args = args.$colon$plus(s(LongKeys.verbose()));
							}
							if (runtimeMonitoring) {
								args = args.$colon$plus(s(LongKeys.runtimeMonitoring()));
							}
							args = args.$colon$plus(s(LongKeys.platform())).$colon$plus(s(platform));
							
							// Slang Options
							if (slangOutputDir.nonEmpty()) {
								args = args.$colon$plus(s(LongKeys.Slang_slangOutputDir())).$colon$plus(slangOutputDir.get());
							}
							if (slangPackageName.nonEmpty()) {
								args = args.$colon$plus(s(LongKeys.Slang_packageName())).$colon$plus(slangPackageName.get());
							}
							if (noProyekIve) {
								args = args.$colon$plus(s(LongKeys.Slang_noProyekIve()));
							}
							if (noEmbedArt) {
								args = args.$colon$plus(s(LongKeys.Slang_noEmbedArt()));
							}
							if (devicesAsThreads) {
								args = args.$colon$plus(s(LongKeys.Slang_devicesAsThreads()));
							}
							if (genSbtMill) {
								args = args.$colon$plus(s(LongKeys.Slang_genSbtMill()));
							}
							
							// Transpiler Options
							if (slangAuxCodeDirs.nonEmpty()) {
								args = args.$colon$plus(s(LongKeys.Transpiler_slangAuxCodeDirs())).$colon$plus(s(CodeGenJavaFactory.iszToST(slangAuxCodeDirs, File.pathSeparator).render()));
							}
							if (slangOutputCDirectory.nonEmpty()) {
								args = args.$colon$plus(s(LongKeys.Transpiler_slangOutputCDir())).$colon$plus(slangOutputCDirectory.get());
							}
							if(excludeComponentImpl) {
								args = args.$colon$plus(s(LongKeys.Transpiler_excludeComponentImpl()));
							}
							args = args.$colon$plus(s(LongKeys.Transpiler_bitWidth())).$colon$plus(s(bitWidth.string()));
							args = args.$colon$plus(s(LongKeys.Transpiler_maxStringSize())).$colon$plus(s(maxStringSize.string()));
							args = args.$colon$plus(s(LongKeys.Transpiler_maxArraySize())).$colon$plus(s(maxArraySize.string()));
							if (runTranspiler) {
								args = args.$colon$plus(s(LongKeys.Transpiler_runTranspiler()));
							}
							
							// CAmkES Options
							if (camkesOutputDirectory.nonEmpty()) {
								args = args.$colon$plus(s(LongKeys.CAmkES_camkesOutputDir())).$colon$plus(camkesOutputDirectory.get());
							}
							if (camkesAuxCodeDirs.nonEmpty()) {
								args = args.$colon$plus(s(LongKeys.CAmkES_camkesAuxCodeDirs())).$colon$plus(s(CodeGenJavaFactory.iszToST(camkesAuxCodeDirs, File.pathSeparator).render()));
							}
							if (workspaceRootDir.nonEmpty()) {
								args = args.$colon$plus(s(LongKeys.CAmkES_workspaceRootDir())).$colon$plus(workspaceRootDir.get());
							}
							
							// ROS2 Options
							if (strictAadlMode) {
								args = args.$colon$plus(s(LongKeys.ROS2_strictAadlMode()));
							}
							if (ros2OutputWorkspaceDir.nonEmpty()) {
								args = args.$colon$plus(s(LongKeys.ROS2_ros2OutputWorkspaceDir())).$colon$plus(ros2OutputWorkspaceDir.get());
							}
							if (ros2Dir.nonEmpty()) {
								args = args.$colon$plus(s(LongKeys.ROS2_ros2Dir())).$colon$plus(ros2Dir.get());
							}
							args = args.$colon$plus(s(LongKeys.ROS2_ros2NodesLanguage())).$colon$plus(s(ros2NodesLanguage));
							args = args.$colon$plus(s(LongKeys.ROS2_ros2LaunchLanguage())).$colon$plus(s(ros2LaunchLanguage));
							
							if (experimentalOptions.nonEmpty()) {
								args = args.$colon$plus(s(LongKeys.Experimental_experimentalOptions())).$colon$plus(s(CodeGenJavaFactory.iszToST(experimentalOptions, ";").render()));
							}

							Z codegenRet = org.sireum.Z.apply(0);
							
							Option<SireumTopOption> opts = getOptions(args);
							if (opts.nonEmpty() && opts.get() instanceof org.sireum.Cli.SireumHamrCodegenOption) {
								org.sireum.cli.HAMR.codeGenReporterP(model, (org.sireum.Cli.SireumHamrCodegenOption) opts.get(), hamrPlugins, reporter);
								if (reporter.hasError()) {
									codegenRet = org.sireum.Z.apply(1);
								}
							} else {
								codegenRet = org.sireum.Z.apply(2);
							}
							
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
	
	org.sireum.String s(java.lang.String s) {
		return SlangUtil.sireumString(s);
	}

	private Option<SireumTopOption> getOptions(IS<Z, org.sireum.String> appArgs) {
		return org.sireum.Cli$.MODULE$.apply(File.pathSeparatorChar).parseSireum(appArgs, org.sireum.Z.apply(0));
	}
}
