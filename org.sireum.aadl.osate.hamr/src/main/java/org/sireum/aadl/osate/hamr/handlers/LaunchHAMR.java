package org.sireum.aadl.osate.hamr.handlers;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
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
import org.sireum.aadl.osate.util.ApiUtil;
import org.sireum.aadl.osate.util.SlangUtils;
import org.sireum.aadl.osate.util.Util;
import org.sireum.hamr.arsit.ArsitBridge;
import org.sireum.hamr.ir.Aadl;
import org.sireum.message.Message;
import org.sireum.message.Position;
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
							if (org.sireum.aadl.osate.PreferenceValues.getPROCESS_BA_OPT()) {
								exOptions.add(new org.sireum.String("PROCESS_BTS_NODES"));
							}
							if (PreferenceValues.HAMR_PROOF_GENERATE.getValue()) {
								exOptions.add(new org.sireum.String("GENERATE_REFINEMENT_PROOF"));
							}

							IS<Z, org.sireum.String> experimentalOptions = VisitorUtil.toISZ(exOptions);

							Reporter reporter = org.sireum.cli.HAMR.codeGenR( //
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
									experimentalOptions);

							// Currently the only way to remove HAMR markers is to resolve the issue
							// and then run the full HAMR toolchain. The expected approach would be to
							// do a quick validation check when the resource is saved. That could be
							// done by:
							// a) doing all the checking at the OSATE level
							// b) having a HAMR cli option that just runs the symbol res phase
							// and then a validation check
							// For now just making HAMR markers an opt-in feature
							if (PreferenceValues.HAMR_PROPOGATE_MARKERS.getValue()) {
								report(reporter, si);
							}

							return reporter.hasError() ? 1 : 0;
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
		}

		return Status.OK_STATUS;
	}

	private void clearHamrMarkers(SystemInstance si) {
		ResourceSet rs = si.eResource().getResourceSet();
		for (Resource r : rs.getResources()) {
			IFile i = Util.toIFile(r.getURI());
			try {
				i.deleteMarkers(PreferenceValues.HAMR_MARKER_ID, true, IResource.DEPTH_INFINITE);
			} catch (CoreException e) {
				// e.printStackTrace();
			}
		}
	}

	/**
	 * Adds any message with position info to the OSATE problems view
	 * @param reporter
	 * @param si
	 */
	private void report(Reporter reporter, SystemInstance si) {
		clearHamrMarkers(si);

		for (int i = 0; i < reporter.messages().size().toInt(); i++) {
			Message m = reporter.messages().apply(SlangUtils.toZ(i));

			if (m.getPosOpt() == null) {
				System.out.println(
						"Sireum message's position info is null rather than None.  Please report - " + m.getText());
			} else if (m.getPosOpt().nonEmpty()) {
				Position pos = m.getPosOpt().get();
				if (pos.uriOpt().nonEmpty()) {
					String uri = "/resource" + pos.uriOpt().get().value();

					Resource r = null;
					for (Resource cand : si.eResource().getResourceSet().getResources()) {
						if (cand.getURI().path().equals(uri)) {
							r = cand;
							break;
						}
					}

					if (r != null) {
						IFile iresource = Util.toIFile(r.getURI());
						try {
							IMarker marker = iresource.createMarker(PreferenceValues.HAMR_MARKER_ID);

							marker.setAttribute(IMarker.MESSAGE, m.getText().toString());
							if (m.isError()) {
								marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
								marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
							} else if (m.isWarning()) {
								marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
								marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
							} else {
								marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_LOW);
								marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
							}
							marker.setAttribute(IMarker.LINE_NUMBER, pos.beginLine().toInt());
							if (pos.offset().toInt() != 0 && pos.length().toInt() > 0) {
								marker.setAttribute(IMarker.CHAR_START, pos.offset().toInt());
								marker.setAttribute(IMarker.CHAR_END, pos.offset().toInt() + pos.length().toInt());
							}

						} catch (CoreException e) {
							// e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
