package org.sireum.aadl.osate.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osate.aadl2.Element;
import org.sireum.aadl.osate.util.Util;

public class EraseMarkersHandler extends AbstractSireumHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Util.clearAllSireumMarkers();

		return Status.OK_STATUS;
	}

	@Override
	protected IStatus runJob(Element sel, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}
}
