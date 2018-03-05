package org.sireum.handlers;

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
import org.sireum.PreferenceValues.SerializerType;
import org.sireum.aadl.skema.ast.Aadl;
import org.sireum.awas.AADLBridge.AadlHandler;
import org.sireum.awas.ast.Model;
import org.sireum.awas.ast.PrettyPrinter;

public class LaunchAwasHandler extends AbstractSireumHandler {
	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {
		String generator = e.getParameter("org.sireum.commands.genawas.generator");
		if (generator == null) {
			throw new RuntimeException("Unable to retrive generator argument");
		}
		this.setGenerator(generator);
		Aadl model = (Aadl) super.execute(e);
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		String s = serialize(model, SerializerType.JSON);
		writeGeneratedFile(e, "json", s);
		try {
			Class<?> c = Class.forName("org.sireum.awas.AADLBridge.AadlHandler");
			Method m = c.getDeclaredMethod("buildAwasModel", Aadl.class);
			Model awas = (Model) m.invoke(null, model);
			String str = PrettyPrinter.apply(awas);
			String awasFile = writeGeneratedFile(e, "awas", str);
			if (awasFile != null) {
				Bundle sireumBundle = Platform.getBundle("org.sireum");
				System.out.println(sireumBundle.getLocation());
				try {
					generateVisualizer(e, awasFile);
				} catch (URISyntaxException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		} catch (Exception ex) {
			String m = "Could not generate Awas code.  Please make sure Awas is present.\n\n"
					+ ex.getLocalizedMessage();
			MessageDialog.openError(window.getShell(), "Sireum", m);
		}
		return null;
	}

	private void generateVisualizer(ExecutionEvent e, String awasFile) throws URISyntaxException, IOException {
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

		AadlHandler.generateWitness(awasFile, queryFile, outputPath, sireumJarLoc);
	}
}
