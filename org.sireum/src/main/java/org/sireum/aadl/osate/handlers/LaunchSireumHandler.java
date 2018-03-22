package org.sireum.aadl.osate.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.PreferenceValues;
import org.sireum.aadl.osate.PreferenceValues.SerializerType;

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
					String s = serialize(model, SerializerType.JSON);
					String outputFolder = PreferenceValues.getARSIT_OUTPUT_FOLDER_OPT();

					File f = new File(outputFolder);
					if (!f.exists()) {
						f = new File(getProjectPath(e).toFile(), outputFolder);
						f.mkdir();
					}
					String fname = getInstanceFilename(e);
					fname = fname.substring(0, fname.lastIndexOf(".")) + ".json";
					writeFile(new File(f, fname), s);
				}

				DirectoryDialog dd = new DirectoryDialog(shell);
				dd.setFilterPath(getProjectPath(e).toString());
				dd.setText("Select directory");
				String path = dd.open();

				if (path != null) {
					File out = new File(path);
					try {
						Class<?> c = Class.forName("org.sireum.aadl.arsit.Runner");
						Method m = c.getDeclaredMethod("run", File.class, Aadl.class);

						int ret = ((Integer) m.invoke(null, out, model)).intValue();

						MessageDialog.openInformation(shell, "Sireum", "Slang-Embedded code "
								+ (ret == 0 ? "successfully generated" : "generation was unsuccessful"));
					} catch (Exception ex) {
						String m = "Could not generate Slang-Embedded code.  Please make sure Arsit is present.\n\n"
								+ ex.getLocalizedMessage();
						MessageDialog.openError(shell, "Sireum", m);
					}
				}
			}
		}

		return null;
	}

	protected void writeFile(File out, String str) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(out));
			writer.write(str);
			writer.close();
			MessageDialog.openInformation(shell, "Sireum", "Wrote: " + out.getAbsolutePath());
		} catch (Exception ee) {
			MessageDialog.openError(shell, "Sireum",
					"Error encountered while trying to save file: " + out.getAbsolutePath() + "\n\n" + ee.getMessage());
		}
	}
}
