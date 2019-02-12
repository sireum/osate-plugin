package org.sireum.aadl.osate.awas.handlers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;

public class LaunchAwas extends AbstractSireumHandler {

	@Override
	protected IStatus runJob(Element arg0, IProgressMonitor arg1) {
		// TODO Auto-generated method stub
		return null;
	}

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

			MessageConsole console = displayConsole("Awas Console");

			File f = serializeToFile(model, ".IR", root);
			writeToConsole(console, "Wrote: " + f.getAbsolutePath());

			final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			// String s = Util.serialize(model, SerializerType.JSON);
			// writeGeneratedFile(e, "json", s);

//				String str = buildAwasText(model);
			String str = org.sireum.awas.AADLBridge.AadlHandler.buildAwasText(model);
			try {
				generateVisualizer(root, model, console);
				getProject(root).refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
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

			return null;
		}
		return null;
	}

	private void generateVisualizer(ComponentInstance root, Aadl model, MessageConsole console) throws Exception {
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
//		IPath path = getInstanceFilePath(e);
//		path = path.removeLastSegments(1);
		DirectoryDialog dialog2 = new DirectoryDialog(window.getShell(), SWT.OPEN);
		dialog2.setFilterPath(getProjectPath(root).toString());
		String outputPath = dialog2.open();
		org.sireum.awas.ast.Model awasModel = org.sireum.awas.AADLBridge.AadlHandler.buildAwasModel(model);
		org.sireum.awas.AADLBridge.AadlHandler.generateWitness(awasModel, outputPath, null);
		String m2 = "Visulaizer generated at: " + outputPath + "/index.html";

		writeToConsole(console, m2);
	}
}
