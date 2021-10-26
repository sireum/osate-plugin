package org.sireum.aadl.osate.hamr.handlers;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.sireum.aadl.osate.architecture.VisitorUtil;
import org.sireum.aadl.osate.hamr.PreferenceValues;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.HW;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.Platform;
import org.sireum.aadl.osate.hamr.handlers.HAMRUtil.ErrorReport;
import org.sireum.aadl.osate.hamr.handlers.HAMRUtil.Report;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;
import org.sireum.aadl.osate.util.SlangUtils;
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

		if (!Util.emitSireumVersion(console)) {
			displayPopup("HAMR code generation was unsuccessful");
			return Status.CANCEL_STATUS;
		}

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
							IS<Z, org.sireum.String> slangAuxCodeDirs = prompt.getOptionCAuxSourceDirectory().equals("")
									? VisitorUtil.toISZ()
									: VisitorUtil.toISZ(new org.sireum.String(prompt.getOptionCAuxSourceDirectory()));
							Option<org.sireum.String> slangOutputCDirectory = ArsitBridge
									.sireumOption(_cOutputDirectory);
							boolean excludeComponentImpl = prompt.getOptionExcludesSlangImplementations();
							Z bitWidth = SlangUtils.toZ(prompt.getOptionBitWidth());
							Z maxStringSize = SlangUtils.toZ(prompt.getOptionMaxStringSize());
							Z maxArraySize = SlangUtils.toZ(prompt.getOptionMaxSequenceSize());
							boolean runTranspiler = PreferenceValues.HAMR_RUN_TRANSPILER_OPT.getValue();
							Option<org.sireum.String> camkesOutputDirectory = ArsitBridge
									.sireumOption(_camkesOutputDir);
							IS<Z, org.sireum.String> camkesAuxCodeDirs = prompt.getOptionCamkesAuxSrcDir().equals("")
									? VisitorUtil.toISZ()
									: VisitorUtil.toISZ(new org.sireum.String(prompt.getOptionCamkesAuxSrcDir()));
							Option<org.sireum.String> aadlRootDir = ArsitBridge
									.sireumOption(new org.sireum.String(workspaceRoot.getAbsolutePath()));

							List<org.sireum.String> exOptions = new ArrayList<>();
							if(org.sireum.aadl.osate.PreferenceValues.getPROCESS_BA_OPT()) {
								exOptions.add(new org.sireum.String("PROCESS_BTS_NODES"));
							}
							if(PreferenceValues.HAMR_PROOF_GENERATE.getValue()) {
								exOptions.add(new org.sireum.String("GENERATE_REFINEMENT_PROOF"));
							}

							IS<Z, org.sireum.String> experimentalOptions = VisitorUtil.toISZ(exOptions);


							return org.sireum.cli.HAMR.codeGenH( //
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
									experimentalOptions).toInt();
						});

						if (toolRet == 0 &&
								PreferenceValues.HAMR_PROOF_GENERATE.getValue() &&
								PreferenceValues.HAMR_PROOF_CHECK.getValue() &&
								(prompt.getOptionPlatform() == Platform.seL4
										|| prompt.getOptionPlatform() == Platform.seL4_Only)) {
							File smt2FileLocation = new File(new File(_slangOutputDir.string()),
									"src/c/camkes/proof/smt2_case.smt2");
							if (_camkesOutputDir != null) {
								smt2FileLocation = new File(new File(_camkesOutputDir.string()),
										"proof/smt2_case.smt2");
							}

							if (smt2FileLocation.exists()) {
								File smt2solver = PreferenceValues.HAMR_SMT2_PATH.getValue();
								if (smt2solver != null) {
									PrintStream out = new PrintStream(console.newMessageStream());

									String[] solverOptions = PreferenceValues.HAMR_SMT2_OPTIONS.getValue().split(" ");
									int timeout = PreferenceValues.HAMR_SMT2_TIMEOUT.getValue();
									toolRet = ProofUtil.checkProof(smt2solver, Arrays.asList(solverOptions),
											smt2FileLocation, timeout, out);

									out.close();
								} else {
									writeToConsole("Location of SMT2 solver not specified");
									toolRet = 1;
								}
							} else {
								writeToConsole("Expected smt2 file not found: " + smt2FileLocation.getAbsolutePath());
								toolRet = 1;
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
		}

		return Status.OK_STATUS;
	}

	private boolean sireumApiCompatible() {

		String msg = null;
		try {
			Class<?> clsHAMR = Class.forName("org.sireum.cli.HAMR");
			Method[] mms = clsHAMR.getMethods();
			Class<?> clsAadl = Class.forName("org.sireum.hamr.ir.Aadl");
			Class<?> clsBool = boolean.class;
			Class<?> clsPlatform = Class.forName("org.sireum.Cli$SireumHamrCodegenHamrPlatform$Type");
			Class<?> clsOption = Class.forName("org.sireum.Option");
			Class<?> clsIS = Class.forName("org.sireum.IS");
			Class<?> clsZ = Class.forName("org.sireum.Z");
			Method m = clsHAMR.getMethod("codeGenH", //
					clsAadl, // model
					clsBool, // verbose
					clsPlatform, // platform
					clsOption, // slangOutputDir
					clsOption, // slangPackageName
					clsBool, // noEmbedArt
					clsBool, // devicesAsThreads
					clsIS, // slangAuxCodeDir
					clsOption, // slangOutputCDirectory
					clsBool, // excludeComponentImpl
					clsZ, // bitWidth
					clsZ, // maxStringSize
					clsZ, // maxArraySize
					clsBool, // runTranspiler
					clsOption, // camkesOutputDirectory
					clsIS, // camkesAuxCodeDirs
					clsOption, // aadlRootDir
					clsIS // experimentalOptions
			);
			return true;
		} catch (ClassNotFoundException e) {
			msg = e.getMessage();
		} catch (NoSuchMethodException e) {
			msg = e.getMessage();
		} catch (SecurityException e) {
			msg = e.getMessage();
		}
		writeToConsole("\nCannot run HAMR Codegen. " + msg);
		writeToConsole("Run Phantom to update HAMR's OSATE plugin (\"$SIREUM_HOME/bin/sireum hamr phantom -u\"). ");
		writeToConsole("If that does not resolve the issue then please report it.\n");
		return false;
	}
}
