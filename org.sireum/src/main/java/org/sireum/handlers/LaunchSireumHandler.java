package org.sireum.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.sireum.PreferenceValues;
import org.sireum.PreferenceValues.SerializerType;
import org.sireum.aadl.arsit.Runner;
import org.sireum.aadl.skema.ast.Aadl;

public class LaunchSireumHandler extends AbstractSireumHandler {
	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {

		String generator = e.getParameter("org.sireum.commands.launchsireum.generator");
		if (generator == null) {
			throw new RuntimeException("Unable to retrive generator argument");
		}

		this.setGenerator(generator);
		Aadl model = (Aadl) super.execute(e);

		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		System.out.println(getInstanceFilePath(e));

		if (generator.equals("serialize")) {

			SerializerType ser = PreferenceValues.getSERIALIZER();

			String s = serialize(model, ser);

			IPath path = getInstanceFilePath(e);
			path = path.removeLastSegments(1);

			FileDialog fd = new FileDialog(window.getShell(), SWT.SAVE);

			fd.setFileName("aadl." + (ser == SerializerType.MSG_PACK ? "msgpack" : "json"));
			fd.setText("Specify filename");
			fd.setFilterPath(path.toString());
			String fname = fd.open();

			if (fname != null) {
				File out = new File(fname);
				writeFile(out, s);
			}
		} else if (generator.equals("genslang")) {

			DirectoryDialog dd = new DirectoryDialog(window.getShell());
			// dd.setFilterPath(string);
			dd.setText("Select directory");
			String path = dd.open();

			if (path != null) {
				File out = new File(path);
				Runner.run(out, model);
			}
		}
		return null;
	}

	protected void writeFile(File out, String str) {
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(out));
			writer.write(str);
			writer.close();
			MessageDialog.openInformation(window.getShell(), "Sireum", "Wrote: " + out.getAbsolutePath());
		} catch (Exception ee) {
			MessageDialog.openError(window.getShell(), "Sireum",
					"Error encountered while trying to save file: " + out.getAbsolutePath() + "\n\n" + ee.getMessage());
		}
	}
}
