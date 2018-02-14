package org.sireum.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
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
		Model awas = AadlHandler.buildAwasModel(model);
		String str = PrettyPrinter.apply(awas);
		String awasFile = writeGeneratedFile(e, "awas", str);
		if(awasFile != null) {
			generateVisualizer(e, awasFile);
		}
		return null;
	}

	private void generateVisualizer(ExecutionEvent e, String awasFile) {
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IPath path = getInstanceFilePath(e);
		path = path.removeLastSegments(1);

		FileDialog dialog = new FileDialog(window.getShell(), SWT.OPEN);

		dialog.setFilterExtensions(new String[] { "*.aq" });
		dialog.setFilterPath(path.toString());
		String queryFile = dialog.open();
		DirectoryDialog dialog2 = new DirectoryDialog(window.getShell(), SWT.OPEN);

		String outputPath = dialog2.open();

		AadlHandler.generateWitness(awasFile, queryFile, outputPath);
	}
}
