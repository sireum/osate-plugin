package org.sireum.aadl.osate.awas.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;

public class LaunchAwas extends AbstractSireumHandler {
	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		ComponentInstance root = getComponentInstance(e);
		if (root == null) {
			MessageDialog.openError(shell, "Sireum", "Please select a system implementation or a system instance");
			return null;
		}

		Aadl model = getAir(root, true);

		if (model != null) {

			MessageConsole console = displayConsole("Arsit Console");

			File f = serializeToFile(model, ".IR", root);
			writeToConsole(console, "Wrote: " + f.getAbsolutePath());

			final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			// String s = Util.serialize(model, SerializerType.JSON);
			// writeGeneratedFile(e, "json", s);

			try {
				Class<?> c = Class.forName("org.sireum.awas.AADLBridge.AadlHandler");
				Method m = c.getDeclaredMethod("buildAwasText", Aadl.class);
				String str = (String) m.invoke(null, model);
				String awasFile = writeGeneratedFile(e, "awas", str);
				if (awasFile != null) {

					try {
						generateVisualizer(e, model);
					} catch (URISyntaxException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (Exception e3) {
						e3.printStackTrace();
						String m2 = "Could not invoke visualizer.  Please make sure Awas is configured correctly.\n\n"
								+ e3.getLocalizedMessage();
						MessageDialog.openError(window.getShell(), "Sireum", m2);
					}
				}
			} catch (Exception ex) {
				String m = "Could not generate Awas code.  Please make sure Awas is present.\n\n"
						+ ex.getLocalizedMessage();
				MessageDialog.openError(window.getShell(), "Sireum", m);
			}
			return null;
		}

		return null;
	}

	private void generateVisualizer(ExecutionEvent e, Aadl model) throws Exception {
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IPath path = getInstanceFilePath(e);
		path = path.removeLastSegments(1);
		DirectoryDialog dialog2 = new DirectoryDialog(window.getShell(), SWT.OPEN);
		String outputPath = dialog2.open();
		Class<?> c = Class.forName("org.sireum.awas.AADLBridge.AadlHandler");
		Class<?> awasModel = Class.forName("org.sireum.awas.ast.Model");
		Method awasModelGen = c.getDeclaredMethod("buildAwasModel", Aadl.class);
		Method m = c.getDeclaredMethod("generateWitness", awasModel, String.class, File.class);
		m.invoke(null, awasModelGen.invoke(null, model), outputPath, null);
	}
}
