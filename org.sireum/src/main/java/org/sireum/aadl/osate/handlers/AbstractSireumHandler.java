package org.sireum.aadl.osate.handlers;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osate.aadl2.Element;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.modelsupport.errorreporting.AnalysisErrorReporterManager;
import org.osate.aadl2.modelsupport.resources.OsateResourceUtil;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.ir.JSON;
import org.sireum.aadl.osate.architecture.Visitor$;
import org.sireum.aadl.osate.util.SelectionHelper;

public abstract class AbstractSireumHandler extends AbstractHandler {
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
		ComponentInstance root = getComponentInstance(e);

		if (root != null) {
			return getAir(root);
		} else {
			final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			MessageDialog.openError(window.getShell(), "Sireum",
					"Please select a component instance element");
			return null;
		}

	}

	ComponentInstance getComponentInstance(ExecutionEvent e) {
		Element root = AadlUtil.getElement(getCurrentSelection(e));

		if (root == null) {
			root = SelectionHelper.getSelectedSystemImplementation();
		}

		if (root != null && root instanceof SystemImplementation) {
			try {
				SystemImplementation si = (SystemImplementation) root;
				InstantiateModel im = new InstantiateModel(new NullProgressMonitor(),
						AnalysisErrorReporterManager.NULL_ERROR_MANANGER);
				URI uri = OsateResourceUtil.getInstanceModelURI(si);
				Resource resource = OsateResourceUtil.getEmptyAaxl2Resource(uri);
				root = im.createSystemInstance(si, resource);
			} catch (Exception ex) {
				final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				MessageDialog.openError(window.getShell(), "Sireum", "Could not instantiate model");
				return null;
			}
		}

		if (root != null && root instanceof ComponentInstance) {
			return (ComponentInstance) root;
		} else {
			return null;
		}
	}

	Aadl getAir(ComponentInstance root) {
		return getAir(root, false);
	}

	Aadl getAir(ComponentInstance root, boolean includeDataComponents) {
		return Visitor$.MODULE$.apply(root, includeDataComponents).get();
	}

	protected void writeJSON(Aadl model) {
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		String str = JSON.fromAadl(model, false);

		// FileDialog fd = new FileDialog(sh, SWT.SAVE);
		// fd.setText("Specify output file name");
		// fd.setFileName(System.getProperty("user.home") + "/aadl.json");
		// fileName = fd.open();
		InputDialog fd = new InputDialog(window.getShell(), "Output file name", "Specify output file name",
				System.getProperty("user.home") + "/aadl.json", null);

		if (fd.open() == Window.OK) {
			File f = new File(fd.getValue());
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(f));
				writer.write(str);
				writer.close();
				MessageDialog.openInformation(window.getShell(), "Sireum", "Wrote: " + f.getAbsolutePath());
			} catch (Exception ee) {
				MessageDialog.openError(window.getShell(), "Sireum", "Error encountered while trying to save file: "
						+ f.getAbsolutePath() + "\n\n" + ee.getMessage());
			}
		}
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

	protected String getInstanceFilename(ComponentInstance root) {
		return OsateResourceUtil.getOsateIFile(root.eResource().getURI()).getName();
	}

	protected IProject getProject(ComponentInstance root) {
		return OsateResourceUtil.getOsateIFile(root.eResource().getURI()).getProject();
	}

	protected IPath getProjectPath(ComponentInstance e) {
		return getProject(e).getLocation();
	}

	protected IPath getInstanceFilePath(ExecutionEvent e) {
		Element root = AadlUtil.getElement(getCurrentSelection(e));
		Resource res = root.eResource();
		URI uri = res.getURI();
		IPath path = OsateResourceUtil.getOsatePath(uri);
		return path;
	}

	protected String writeGeneratedFile(ExecutionEvent e, String type, String content) {
		IPath path = getInstanceFilePath(e);
		path = path.removeFileExtension();
		String filename = path.lastSegment() + "__" + type;
		path = path.removeLastSegments(1).append("/.IR/" + type + "/" + filename);
		path = path.addFileExtension(type);
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		if (file != null) {
			final InputStream input = new ByteArrayInputStream(content.getBytes());
			try {
				if (file.exists()) {
					file.setContents(input, true, true, null);
				} else {
					AadlUtil.makeSureFoldersExist(path);
					file.create(input, true, null);
				}
			} catch (final CoreException excp) {
			}
			return file.getLocation().toString();
		}
		return null;
	}

	protected MessageConsole displayConsole(String name) {
		MessageConsole ms = findConsole(name);
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IConsoleView view;
			view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
			view.display(ms);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		ms.clearConsole();
		return ms;
	}

	protected MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });

		return myConsole;
	}
}
