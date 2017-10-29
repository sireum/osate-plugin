package org.sireum.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osate.aadl2.Element;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.sireum.aadl.ast.AadlXml;
import org.sireum.aadl.ast.JSON;
import org.sireum.architecture.Check;
import org.sireum.architecture.ErrorReport;
import org.sireum.architecture.Visitor;
import org.sireum.util.SelectionHelper;

public abstract class AbstractSireumHandler extends AbstractHandler {

	private String generator = null;
	// private SystemImplementation systemImplementation;

	protected final String MARKER_TYPE = "org.sireum.aadl.marker";

	protected static IResource getIResource(Resource r) {
		final URI uri = r.getURI();
		final IPath path = new Path(uri.toPlatformString(true));
		final IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (resource == null) {
			throw new RuntimeException("Unable to get IResource for Resource: " + r);
		}
		return resource;
	}

	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {
		if(this.generator == null) {
			throw new RuntimeException("Generator is null");
		}

		Element root = AadlUtil.getElement(getCurrentSelection(e));

		if (root == null) {
			root = SelectionHelper.getSelectedSystemImplementation();
		}

		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		if (root != null && root instanceof ComponentInstance) {
			List<ErrorReport> l = Check.check((ComponentInstance) root);
			if (!l.isEmpty()) {
				String m = "";
				for (ErrorReport er : l) {
					String name = ((NamedElement) er.component().eContainer()).getQualifiedName() + "."
							+ er.component().getQualifiedName();
					m += name + " : " + er.message() + "\n";

					try {
						IMarker marker = getIResource(er.component().eResource()).createMarker(IMarker.PROBLEM);
						marker.setAttribute(IMarker.MESSAGE, name + " - " + er.message());
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					} catch (CoreException exception) {
						exception.printStackTrace();
					}
				}
				System.out.println(m);
				MessageDialog.openError(window.getShell(), "Sireum", m);
				return null;
			}

			AadlXml _r = Visitor.convert(root);
			String str = JSON.fromAadlTop(_r, false);

			// FileDialog fd = new FileDialog(sh, SWT.SAVE);
			// fd.setText("Specify output file name");
			// fd.setFileName(System.getProperty("user.home") + "/aadl.json");
			// fileName = fd.open();
			InputDialog fd = new InputDialog(window.getShell(), "Output file name",
					"Specify output file name",
					System.getProperty("user.home") + "/aadl.json", null);

			if (fd.open() == Window.OK) {
				File f = new File(fd.getValue());
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(f));
					writer.write(str);
					writer.close();
					MessageDialog.openInformation(window.getShell(), "Sireum", "Wrote: " + f.getAbsolutePath());
				} catch (Exception ee) {
					MessageDialog.openError(window.getShell(), "Sireum",
							"Error encountered while trying to save file: " + f.getAbsolutePath() + "\n\n" + ee.getMessage());
				}
			}
		} else {
			MessageDialog.openError(window.getShell(), "Sireum",
					"Please select a component instance element");
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
