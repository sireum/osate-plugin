package org.sireum.aadl.osate.handlers;

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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.PreferenceValues.SerializerType;



public class LaunchAwasHandler extends AbstractSireumHandler {
	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {
		String generator = e.getParameter("org.sireum.commands.genawas.generator");
		if (generator == null) {
			throw new RuntimeException("Unable to retrive generator argument");
		}
		Aadl model = (Aadl) super.execute(e);
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		String s = serialize(model, SerializerType.JSON);
		writeGeneratedFile(e, "json", s);
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
