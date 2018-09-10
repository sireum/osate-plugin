package org.sireum.aadl.osate.handlers;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
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
			Method m = c.getDeclaredMethod("buildAwasString", Aadl.class);
			String str = (String) m.invoke(null, model);
			String awasFile = writeGeneratedFile(e, "awas", str);
			if (awasFile != null) {
				Bundle sireumBundle = Platform.getBundle("org.sireum");
				try {
					generateVisualizer(e, awasFile);
				} catch (URISyntaxException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (Exception e3) {
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

	private void generateVisualizer(ExecutionEvent e, String awasFile) throws Exception {
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IPath path = getInstanceFilePath(e);
		path = path.removeLastSegments(1);

		FileDialog dialog = new FileDialog(window.getShell(), SWT.OPEN);

		dialog.setFilterExtensions(new String[] { "*.aq" });
		dialog.setFilterPath(path.toString());
		String queryFile = dialog.open();
		DirectoryDialog dialog2 = new DirectoryDialog(window.getShell(), SWT.OPEN);

		String outputPath = dialog2.open();
		Bundle sireumBundle = Platform.getBundle("org.sireum");
		URL url = sireumBundle.getEntry("lib/sireum.jar");
		URI uri = FileLocator.toFileURL(url).toURI();
		String sireumJarLoc = uri.getPath();
		Class<?> c = Class.forName("org.sireum.awas.AADLBridge.AadlHandler");

		Method m = c.getDeclaredMethod("generateWitness", String.class, String.class, String.class, String.class);
		m.invoke(null, awasFile, queryFile, outputPath, sireumJarLoc);
	}
}
