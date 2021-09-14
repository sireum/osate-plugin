package org.sireum.aadl.osate.handlers;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
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
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.Element;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.SystemType;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.ui.dialogs.Dialog;
import org.sireum.SireumApi;
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

	public boolean emitSireumVersion() {
		String propName = "org.sireum.home";
		String propValue = System.getProperty(propName);
		if (propValue != null) {
			File sireum_jar = new File(propValue, "bin/sireum.jar");
			if (!sireum_jar.exists()) {
				writeToConsole("sireum.jar not found. Expecting it to be at: " + sireum_jar.getAbsolutePath() //
						+ "\n\n" //
						+ "Ensure that the '" + propName + "' Java system property (current value is '"
						+ propValue + "') is set \n"
						+ "to the absolute path to your Sireum installation (sireum.jar should be in its 'bin' directory). \n"
						+ "You must restart OSATE in order for changes to osate.ini to take effect.\n");
				return false;
			} else {
				writeToConsole(
						"Sireum Version: " + SireumApi.version() + " located at " + sireum_jar.getAbsolutePath());
				return true;
			}
		} else {
			writeToConsole("Java system property '" + propName + "' not set. \n" //
					+ "\n" //
					+ "The prefered way of setting this is by installing the HAMR plugin via Phantom.  Run " //
					+ "the following from the command line for more information\n" //
					+ "\n" //
					+ "    $SIREUM_HOME/bin/sireum hamr phantom -h\n"
					+ "\n" //
					+ "If you don't have Sireum installed then refer to https://github.com/sireum/kekinian#installing\n"
					+ "\n\n" //
					+ "To set this property manually, in your osate.ini file locate the line containing '-vmargs' and \n"
					+ "add the following on a new line directly after that \n"
					+ "\n"
					+ "    -D" + propName + "=<path-to-sireum>\n"
					+ "\n"
					+ "replacing <path-to-sireum> with the absolute path to your Sireum installation \n"
					+ "(sireum.jar should be under its 'bin' directory).  Then restart OSATE." + "\n" //
					+ "\n" //
					+ "Alternatively, start OSATE using the vmargs option.  For example: " //
					+ "\n" //
					+ "\n" //
					+ "    <path-to-osate>/osate -vmargs " + propName + "=<path-to-sireum>\n");
			return false;
		}
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

		if (root == null) {
			ISelection selection = SelectionHelper.getSelection();
			if (selection instanceof TreeSelection) {
				TreeSelection ts = (TreeSelection) selection;

				List<SystemImplementation> candidates = new ArrayList<>();
				for (Object o : ts.toList()) {
					if (o instanceof EObjectNode) {
						EObjectNode eon = (EObjectNode) o;
						EObject eo = SelectionHelper.getXtextEditor().getDocument().readOnly(resource -> {
							return eon.getEObject(resource);
						});
						if (eo instanceof SystemImplementation) {
							candidates.add((SystemImplementation) eo);
						}
					}
				}

				if (candidates.size() == 1) {
					// selected items in outline view only include a single system implementation
					// so use that
					root = candidates.get(0);
				}

			} else if (selection instanceof TextSelection) {
				TextSelection ts = (TextSelection) selection;
				EObject selectedObject = SelectionHelper.getEObjectFromSelection(selection);
				if (selectedObject instanceof SystemType) {
					// cursor is probably in the xx part of 'system implementation xx.yy ...'
					SystemType st = (SystemType) selectedObject;

					// find all system implementations of st and see if the cursor
					// is in one of them
					AadlPackage ap = AadlUtil.getContainingPackage(st);
					for (SystemImplementation si : EcoreUtil2.getAllContentsOfType(ap, SystemImplementation.class)) {
						if (si.getType().equals(st)) {
							INode node = NodeModelUtils.findActualNodeFor(si);
							if (node != null && //
									(node.getStartLine() <= ts.getStartLine() + 1 //
											&& ts.getEndLine() + 1 <= node.getEndLine())) {
								root = si;
							}
						}
					}
				}
			}
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
		return Util.toIFile(root.eResource().getURI()).getName();
	}

	protected IProject getProject(ComponentInstance root) {
		return Util.toIFile(root.eResource().getURI()).getProject();
	}

	protected IPath getProjectPath(ComponentInstance e) {
		return getProject(e).getLocation();
	}

	protected IPath getInstanceFilePath(ExecutionEvent e) {
		Element root = getComponentInstance(e);
		Resource res = root.eResource();
		URI uri = res.getURI();
		IPath path = Util.toIFile(uri).getFullPath();
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

	protected void displayPopup(String msg) {
		Display.getDefault().syncExec(() -> {
			writeToConsole(msg);
			AbstractNotificationPopup notification = new EclipseNotification(Display.getCurrent(),
					getToolName() + " Message", msg);
			notification.open();
		});
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
