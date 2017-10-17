package org.sireum.handlers;

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osate.aadl2.Element;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.sireum.aadl.ast.AadlTop;
import org.sireum.aadl.ast.JSON;
import org.sireum.architecture.Visitor;
import org.sireum.util.SelectionHelper;

public abstract class AbstractSireumHandler extends AbstractHandler {

	private String generator = null;
	private SystemImplementation systemImplementation;

	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {
		if(this.generator == null) {
			throw new RuntimeException("Generator is null");
		}

		Element root = AadlUtil.getElement(getCurrentSelection(e));

		if (root == null) {
			root = SelectionHelper.getSelectedSystemImplementation();
		}

		final IWorkbench wb = PlatformUI.getWorkbench();
		final IWorkbenchWindow window = wb.getActiveWorkbenchWindow();

		if (root != null) {
			AadlTop _r = Visitor.visit(root);
			// Visitor.displayTypePackages();
			String str = JSON.fromAadlTop(_r, false);

			try {
				String fileName = System.getProperty("user.home") + "/aadl.json";
				BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
				writer.write(str);
				writer.close();
				MessageDialog.openInformation(window.getShell(), "Sireum", "Wrote: " + fileName);
			} catch (Exception ee) {
				MessageDialog.openError(window.getShell(), "Sireum", ee.getMessage());
			}

		} else {
			MessageDialog.openError(window.getShell(), "Sireum",
					"Please select an instance element");
		}

		return null;
	}

	protected Object getCurrentSelection(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1) {
			Object object = ((IStructuredSelection) selection).getFirstElement();
			return object;
		} else {
			return null;
		}
	}

	protected void setGenerator(String v) {
		this.generator = v;
	}
}
