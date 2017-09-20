package org.sireum.handlers;

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
import org.sireum.architecture.Visitor;
import org.sireum.util.SelectionHelper;
import org.sireum.aadl.ast.MyTop;
import org.sireum.aadl.ast.JSON;

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

		// System.out.println("root = " + root);

		final IWorkbench wb = PlatformUI.getWorkbench();
		final IWorkbenchWindow window = wb.getActiveWorkbenchWindow();

		if (root != null) {
			MyTop _r = Visitor.visit(root);

			System.out.println(JSON.fromMyTop(_r, false));

		} else {
			MessageDialog.openError(window.getShell(), "Sireum",
					"Please select a System Implementation (** don't put the cursor in the system's name **");
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
