package org.sireum.aadl.osate.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

public class BijiPrompt extends TitleAreaDialog {
	private Text txtOutputDirectory;

	IProject project;
	IEclipsePreferences projectNode;

	String PREF_KEY = "org.sireum.aadl.biji";

	private final String KEY_OUTPUT_DIRECTORY = "output.directory";

	/**
	 * @wbp.parser.constructor
	 */
	public BijiPrompt(Shell parentShell) {
		super(parentShell);
	}

	public BijiPrompt(IProject p, Shell parentShell) {
		super(parentShell);
		project = p;
		IScopeContext projectScope = new ProjectScope(project);
		projectNode = projectScope.getNode(PREF_KEY);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		setMessage("");
		setTitle("Biji Options");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		GridData gd_container = new GridData(GridData.FILL_BOTH);
		gd_container.widthHint = 552;
		container.setLayoutData(gd_container);

		Button btnOutputDirectory = new Button(container, SWT.NONE);
		btnOutputDirectory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				promptForOutputDirectory();
			}
		});
		btnOutputDirectory.setBounds(477, 3, 42, 28);
		btnOutputDirectory.setText("...");

		Label lblOutputDirectory = new Label(container, SWT.NONE);
		lblOutputDirectory.setBounds(10, 10, 121, 21);
		lblOutputDirectory.setText("Output Directory");

		txtOutputDirectory = new Text(container, SWT.BORDER);
		txtOutputDirectory.setBounds(137, 7, 334, 19);

		initValues();
		return area;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Run", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(530, 372);
	}

	@Override
	protected void okPressed() {
		saveOptions();
		super.okPressed();
	}

	void saveOptions() {
		projectNode.put(KEY_OUTPUT_DIRECTORY, txtOutputDirectory.getText());

		try {
			projectNode.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	void initValues() {

		String outputDirectory = this.getOptionOutputDirectory();
		if (outputDirectory.equals("")) {
			outputDirectory = project.getLocation().toString();
		}
		txtOutputDirectory.setText(outputDirectory);
	}

	public String getOptionOutputDirectory() {
		return projectNode.get(KEY_OUTPUT_DIRECTORY, "");
	}

	void promptForOutputDirectory() {
		DirectoryDialog d = new DirectoryDialog(this.getShell());
		if (!getOptionOutputDirectory().equals("")) {
			d.setFilterPath(getOptionOutputDirectory());
		} else {
			d.setFilterPath(project.getLocation().toString());
		}
		d.setText("Select Output Directory");
		String path = d.open();
		if (path != null) {
			txtOutputDirectory.setText(path);
		}
	}
}
