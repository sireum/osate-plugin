package org.sireum.aadl.osate.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.PreferenceValues;
import org.sireum.aadl.osate.PreferenceValues.Generators;
import org.sireum.aadl.osate.PreferenceValues.SerializerType;
import org.sireum.aadl.osate.architecture.Check;
import org.sireum.aadl.osate.architecture.ErrorReport;
import org.sireum.aadl.osate.architecture.Report;
import org.sireum.aadl.osate.util.ActPrompt;
import org.sireum.aadl.osate.util.ArsitPrompt;
import org.sireum.aadl.osate.util.ArsitUtil;
import org.sireum.aadl.osate.util.ScalaUtil;

public class LaunchSireumHandler extends AbstractSireumHandler {
	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {
		if (e.getParameter("org.sireum.commands.launchsireum.generator") == null) {
			throw new RuntimeException("Unable to retrive generator argument");
		}
		Generators generator = Generators.valueOf(e.getParameter("org.sireum.commands.launchsireum.generator"));

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		ComponentInstance root = getComponentInstance(e);
		if (root == null) {
			MessageDialog.openError(shell, "Sireum", "Please select a system implementation or a system instance");
			return null;
		}

		if (generator == Generators.GEN_ARSIT && !check(root)) {
			MessageDialog.openError(shell, "Sireum", "Errors found in model");
			return null;
		}

		Aadl model = getAir(root, generator == Generators.GEN_CAMKES);

		if (model != null) {
			switch (generator) {
			case SERIALIZE: {

				SerializerType ser = PreferenceValues.getSERIALIZATION_METHOD_OPT();

				String s = serialize(model, ser);

				FileDialog fd = new FileDialog(shell, SWT.SAVE);
				fd.setFileName("aadl." + (ser == SerializerType.MSG_PACK ? "msgpack" : "json"));
				fd.setText("Specify filename");
				fd.setFilterPath(getProjectPath(root).toString());
				String fname = fd.open();

				if (fname != null) {
					File out = new File(fname);
					writeFile(out, s);
				}
				break;
			}
			case GEN_ARSIT: {

				if (PreferenceValues.getARSIT_SERIALIZE_OPT()) {
					serializeToFile(model, PreferenceValues.getARSIT_OUTPUT_FOLDER_OPT(), root);
				}

				ArsitPrompt p = new ArsitPrompt(getProject(root), shell);
				if (p.open() == Window.OK) {
					try {
						// Eclipse doesn't seem to like accessing nested scala classes
						// (e.g. org.sireum.cli.Cli$ArsitOption$) so invoke Arsit from scala instead

						int ret = ArsitUtil.launchArsit(p, model);

						MessageDialog.openInformation(shell, "Sireum", "Slang-Embedded code "
								+ (ret == 0 ? "successfully generated" : "generation was unsuccessful"));
					} catch (Exception ex) {
						ex.printStackTrace();
						String m = "Could not generate Slang-Embedded code.  Please make sure Arsit is present.\n\n"
								+ ex.getLocalizedMessage();
						MessageDialog.openError(shell, "Sireum", m);
					}
				}
				break;
			}
			case GEN_CAMKES: {

				if (PreferenceValues.getACT_SERIALIZE_OPT()) {
					serializeToFile(model, PreferenceValues.getACT_OUTPUT_FOLDER_OPT(), root);
				}

				ActPrompt p = new ActPrompt(getProject(root), shell);
				if (p.open() == Window.OK) {
					try {
						File out = new File(p.getOptionOutputDirectory());
						if (!out.exists()) {
							if (MessageDialog.openQuestion(shell, "Create Directory?", "Directory '"
									+ out.getAbsolutePath() + "' does not exist.  Should it be created?")) {
								if (!out.mkdirs()) {
									MessageDialog.openError(shell, "Error",
											"Could not create directory " + out.getAbsolutePath());
									return null;
								}
							}
						}
						int ret = ScalaUtil.launchAct(p, model, displayConsole("ACT Console"));

						MessageDialog.openInformation(shell, "Sireum",
								"CAmkES code " + (ret == 0 ? "successfully generated" : "generation was unsuccessful"));

					} catch (Exception ex) {
						String m = "Could not generate CAmkES.  Please make sure ACT is present.\n\n"
								+ ex.getLocalizedMessage();
						MessageDialog.openError(shell, "Sireum", m);
					}
				}
				break;
			}
			default:
				MessageDialog.openError(shell, "Sireum", "Not expecting generator: " + generator);
				break;
			}
		} else {
			MessageDialog.openError(shell, "Sireum", "Could not generate AIR");
		}

		return null;
	}

	protected boolean check(ComponentInstance root) {
		boolean hasErrors = false;
		List<Report> l = Check.check(root);
		if (!l.isEmpty()) {
			String m = "";
			for (Report er : l) {
				hasErrors |= er instanceof ErrorReport;
				String name = ((NamedElement) er.component().eContainer()).getQualifiedName() + "."
						+ er.component().getQualifiedName();
				m += name + " : " + er.message() + "\n";

				try {
					int severity = er instanceof ErrorReport ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING;
					IMarker marker = getIResource(er.component().eResource()).createMarker(IMarker.PROBLEM);
					marker.setAttribute(IMarker.MESSAGE, name + " - " + er.message());
					marker.setAttribute(IMarker.SEVERITY, severity);
				} catch (CoreException exception) {
					exception.printStackTrace();
				}
			}
		}
		return !hasErrors;
	}

	protected void serializeToFile(Aadl model, String outputFolder, ComponentInstance e) {
		String s = serialize(model, SerializerType.JSON);

		File f = new File(outputFolder);
		if (!f.exists()) {
			f = new File(getProjectPath(e).toFile(), outputFolder);
			f.mkdir();
		}
		String fname = getInstanceFilename(e);
		fname = fname.substring(0, fname.lastIndexOf(".")) + ".json";
		writeFile(new File(f, fname), s, false);
	}

	protected void writeFile(File out, String str) {
		writeFile(out, str, true);
	}

	protected void writeFile(File out, String str, boolean confirm) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(out));
			writer.write(str);
			writer.close();
			if (confirm) {
				MessageDialog.openInformation(shell, "Sireum", "Wrote: " + out.getAbsolutePath());
			}
		} catch (Exception ee) {
			MessageDialog.openError(shell, "Sireum",
					"Error encountered while trying to save file: " + out.getAbsolutePath() + "\n\n" + ee.getMessage());
		}
	}
}
