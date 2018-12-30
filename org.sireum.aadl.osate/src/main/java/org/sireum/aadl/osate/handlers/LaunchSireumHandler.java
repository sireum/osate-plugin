package org.sireum.aadl.osate.handlers;

import java.io.File;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.osate.PreferenceValues;
import org.sireum.aadl.osate.PreferenceValues.Generators;
import org.sireum.aadl.osate.architecture.Check;
import org.sireum.aadl.osate.architecture.ErrorReport;
import org.sireum.aadl.osate.architecture.Report;
import org.sireum.aadl.osate.util.Util;
import org.sireum.aadl.osate.util.Util.SerializerType;

public class LaunchSireumHandler extends AbstractSireumHandler {
	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {
		if (e.getParameter("org.sireum.commands.launchsireum.generator") == null) {
			throw new RuntimeException("Unable to retrive generator argument");
		}
		Generators generator = Generators.valueOf(e.getParameter("org.sireum.commands.launchsireum.generator"));

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		ComponentInstance root = getComponentInstance(e);
		if (root == null) {
			MessageDialog.openError(shell, "Sireum", "Please select a system implementation or a system instance");
			return null;
		}

		Aadl model = getAir(root, true);

		if (model != null) {

			MessageConsole console = displayConsole("Sireum Console");

			switch (generator) {
			case SERIALIZE: {

				SerializerType ser = PreferenceValues.getSERIALIZATION_METHOD_OPT();

				String s = Util.serialize(model, ser);

				FileDialog fd = new FileDialog(shell, SWT.SAVE);
				fd.setFileName("aadl." + (ser == SerializerType.MSG_PACK ? "msgpack" : "json"));
				fd.setText("Specify filename");
				fd.setFilterPath(getProjectPath(root).toString());
				String fname = fd.open();

				if (fname != null) {
					File out = new File(fname);
					writeFile(out, s, false);
					writeToConsole(console, "Wrote: " + out.getAbsolutePath());
				}
				break;
			}
			default:
				MessageDialog.openError(shell, "Sireum", "Not expecting generator: " + generator);
				break;
			}
		} else {
			MessageDialog.openError(shell, "Sireum", "Could not generate AIR");
		}

		return null;
	}

	public boolean check(ComponentInstance root) {
		boolean hasErrors = false;
		List<Report> l = Check.check(root);
		if (!l.isEmpty()) {
			String m = "";
			for (Report er : l) {
				hasErrors |= er instanceof ErrorReport;
				String name = ((NamedElement) er.component().eContainer()).getQualifiedName() + "."
						+ er.component().getQualifiedName();
				m += name + " : " + er.message() + "\n";

				try {
					int severity = er instanceof ErrorReport ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING;
					IMarker marker = getIResource(er.component().eResource()).createMarker(IMarker.PROBLEM);
					marker.setAttribute(IMarker.MESSAGE, name + " - " + er.message());
					marker.setAttribute(IMarker.SEVERITY, severity);
				} catch (CoreException exception) {
					exception.printStackTrace();
				}
			}
		}
		return !hasErrors;
	}
}
