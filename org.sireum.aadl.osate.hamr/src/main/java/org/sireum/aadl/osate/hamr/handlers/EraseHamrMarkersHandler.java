package org.sireum.aadl.osate.hamr.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osate.aadl2.Element;
import org.sireum.aadl.osate.hamr.PreferenceValues;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;
import org.sireum.aadl.osate.util.SelectionHelper;

public class EraseHamrMarkersHandler extends AbstractSireumHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IProject project = SelectionHelper.getProject();
		if (project != null) {
			try {
				for (IResource r : project.members()) {
					r.deleteMarkers(PreferenceValues.HAMR_MARKER_ID, true, IResource.DEPTH_INFINITE);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return Status.OK_STATUS;
	}

	@Override
	protected IStatus runJob(Element sel, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}
}
