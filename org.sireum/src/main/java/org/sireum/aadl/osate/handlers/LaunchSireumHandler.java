package org.sireum.aadl.osate.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.PreferenceValues;
import org.sireum.aadl.osate.PreferenceValues.SerializerType;
import org.sireum.aadl.osate.util.ActPrompt;
import org.sireum.aadl.osate.util.ArsitPrompt;

public class LaunchSireumHandler extends AbstractSireumHandler {
	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {

		String generator = e.getParameter("org.sireum.commands.launchsireum.generator");
		if (generator == null) {
			throw new RuntimeException("Unable to retrive generator argument");
		}

		setGenerator(generator);
		Aadl model = (Aadl) super.execute(e);

		if (model != null) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

			if (generator.equals("serialize")) {

				SerializerType ser = PreferenceValues.getSERIALIZATION_METHOD_OPT();

				String s = serialize(model, ser);

				FileDialog fd = new FileDialog(shell, SWT.SAVE);
				fd.setFileName("aadl." + (ser == SerializerType.MSG_PACK ? "msgpack" : "json"));
				fd.setText("Specify filename");
				fd.setFilterPath(getProjectPath(e).toString());
				String fname = fd.open();

				if (fname != null) {
					File out = new File(fname);
					writeFile(out, s);
				}
			} else if (generator.equals("genslang")) {

				if (PreferenceValues.getARSIT_SERIALIZE_OPT()) {
					serializeToFile(model, PreferenceValues.getARSIT_OUTPUT_FOLDER_OPT(), e);
				}

				ArsitPrompt p = new ArsitPrompt(getProject(e), shell);
				if (p.open() == Window.OK) {
					try {
						// Eclipse doesn't seem to like accessing nested scala classes
						// (e.g. org.sireum.cli.Cli$ArsitOption$) so invoke Arsit from scala instead

						int ret = org.sireum.aadl.osate.util.Util.launchArsit(p, model);

						MessageDialog.openInformation(shell, "Sireum", "Slang-Embedded code "
								+ (ret == 0 ? "successfully generated" : "generation was unsuccessful"));
					} catch (Exception ex) {
						ex.printStackTrace();
						String m = "Could not generate Slang-Embedded code.  Please make sure Arsit is present.\n\n"
								+ ex.getLocalizedMessage();
						MessageDialog.openError(shell, "Sireum", m);
					}
				}
			} else if (generator.equals("gencamkes")) {

				if (PreferenceValues.getACT_SERIALIZE_OPT()) {
					serializeToFile(model, PreferenceValues.getACT_OUTPUT_FOLDER_OPT(), e);
				}

				ActPrompt p = new ActPrompt(getProject(e), shell);
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
						int ret = org.sireum.aadl.osate.util.Util.launchAct(p, model);

						MessageDialog.openInformation(shell, "Sireum",
								"CAmkES code " + (ret == 0 ? "successfully generated" : "generation was unsuccessful"));

					} catch (Exception ex) {
						String m = "Could not generate CAmkES.  Please make sure ACT is present.\n\n"
								+ ex.getLocalizedMessage();
						MessageDialog.openError(shell, "Sireum", m);
					}
				}
			}
		}

		return null;
	}

	protected void serializeToFile(Aadl model, String outputFolder, ExecutionEvent e) {
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
