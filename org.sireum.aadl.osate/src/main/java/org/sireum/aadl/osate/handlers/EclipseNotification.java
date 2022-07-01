package org.sireum.aadl.osate.handlers;

import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * Sample Notification Popup Class
 */
public class EclipseNotification extends AbstractNotificationPopup {

	String title;
	String message;

	/**
	 * @param display
	 */
	private EclipseNotification(Display display) {
		super(display);
	}

	public EclipseNotification(Display display, String _title, String _message) {
		super(display);
		title = _title;
		message = _message;
	}

	@Override
	protected void createContentArea(Composite parent) {

		Composite container = new Composite(parent, SWT.NULL);

		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		container.setLayoutData(data);

		container.setLayout(new GridLayout(1, false));

		Label lblmsg = new Label(container, SWT.NULL);
		lblmsg.setText(message);

		new Label(container, SWT.NONE);
	}

	@Override
	protected String getPopupShellTitle() {
		return title;
	}
}