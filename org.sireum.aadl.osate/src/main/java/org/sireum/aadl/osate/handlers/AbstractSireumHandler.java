package org.sireum.aadl.osate.handlers;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
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
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osate.aadl2.Element;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.modelsupport.resources.OsateResourceUtil;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.ui.dialogs.Dialog;
import org.sireum.aadl.osate.util.SelectionHelper;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SerializerType;
import org.sireum.hamr.ir.Aadl;
import org.sireum.hamr.ir.JSON;

public abstract class AbstractSireumHandler extends AbstractHandler {

	protected abstract IStatus runJob(Element sel, IProgressMonitor monitor);

	protected String getToolName() {
		return "Sireum";
	}

	protected String getJobName() {
		return getToolName() + " job";
	}

	protected final String MARKER_TYPE = "org.sireum.hamr.marker";

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

		Element elem = getElement(e);

		WorkspaceJob j = new WorkspaceJob(getJobName()) {
			@Override
			public IStatus runInWorkspace(final IProgressMonitor monitor) {
				return runJob(elem, monitor);
			}
		};

		j.setRule(ResourcesPlugin.getWorkspace().getRoot());
		j.schedule();
		return null;
	}

	private Element getElement(ExecutionEvent e) {
		Element root = AadlUtil.getElement(getCurrentSelection(e));

		if (root == null) {
			root = SelectionHelper.getSelectedSystemImplementation();
		}

		return root;
	}

	protected SystemInstance getSystemInstance(Element e) {
		if (e != null) {
			if (e instanceof SystemInstance) {
				return (SystemInstance) e;
			}
			if (e instanceof SystemImplementation) {
				try {
					SystemImplementation si = (SystemImplementation) e;

					writeToConsole("Generating System Instance ...");

					return InstantiateModel.buildInstanceModelFile(si);
				} catch (Exception ex) {
					Dialog.showError(getToolName(), "Could not instantiate model");
					ex.printStackTrace();
				}
			}
		}

		return null;
	}

	protected ComponentInstance getComponentInstance(ExecutionEvent e) {
		Element root = AadlUtil.getElement(getCurrentSelection(e));

		if (root == null) {
			root = SelectionHelper.getSelectedSystemImplementation();
		}

		if (root != null && root instanceof SystemImplementation) {
			try {
				SystemImplementation si = (SystemImplementation) root;

				root = InstantiateModel.buildInstanceModelFile(si);
			} catch (Exception ex) {
				Dialog.showError(getToolName(), "Could not instantiate model");
				return null;
			}
		}

		if (root != null && root instanceof ComponentInstance) {
			return (ComponentInstance) root;
		} else {
			return null;
		}
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
		Element root = getComponentInstance(e);
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

	protected File serializeToFile(Aadl model, String outputFolder, ComponentInstance e) {
		String s = Util.serialize(model, SerializerType.JSON);

		File f = new File(outputFolder);
		if (!f.exists()) {
			f = new File(getProjectPath(e).toFile(), outputFolder);
			f.mkdir();
		}
		String fname = getInstanceFilename(e);
		fname = fname.substring(0, fname.lastIndexOf(".")) + ".json";
		File ret = new File(f, fname);
		writeFile(ret, s, false);
		return ret;
	}

	protected void writeFile(File out, String str) {
		writeFile(out, str, true);
	}

	protected void writeFile(File out, String str, boolean confirm) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(out));
			writer.write(str);
			writer.close();
			if (confirm) {
				Dialog.showInfo(getToolName(), "Wrote: " + out.getAbsolutePath());
			}
		} catch (Exception ee) {
			Dialog.showError(getToolName(),
					"Error encountered while trying to save file: " + out.getAbsolutePath() + "\n\n" + ee.getMessage());
		}
	}

	private MessageConsole getConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}
		// no console found, so create a new one
		MessageConsole mc = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { mc });

		return mc;
	}

	protected MessageConsole displayConsole() {
		return displayConsole(getToolName());
	}

	protected MessageConsole displayConsole(String name) {
		MessageConsole ms = getConsole(name);
		Display.getDefault().syncExec(() -> {
			try {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IConsoleView view;
				view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
				view.display(ms);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		});
		return ms;
	}

	protected boolean writeToConsole(String text) {
		return writeToConsole(text, false);
	}

	protected boolean writeToConsole(String text, boolean clearConsole) {
		MessageConsole ms = displayConsole(getToolName());
		if (clearConsole) {
			ms.clearConsole();
		}
		return writeToConsole(ms, text);
	}

	protected boolean writeToConsole(MessageConsole m, String text) {
		boolean isWritten = false;
		if (m != null) {
			MessageConsoleStream out = m.newMessageStream();
			out.println(text);
			isWritten = true;
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return isWritten;
	}

	protected Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	protected void refreshWorkspace() {
		try {
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
