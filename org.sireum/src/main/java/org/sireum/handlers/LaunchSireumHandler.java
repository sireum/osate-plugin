package org.sireum.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.sireum.aadl.skema.ast.Aadl;

public class LaunchSireumHandler extends AbstractSireumHandler {
	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {

		String generator = e.getParameter("org.sireum.commands.launchsireum.generator");
		if (generator == null) {
			throw new RuntimeException("Unable to retrive generator argument");
		}

		this.setGenerator(generator);
		Aadl model = (Aadl) super.execute(e);
		writeJSON(model);
		return null;
	}
}
