package org.sireum.aadl.osate.act.handlers;

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

public class ActPrompt extends TitleAreaDialog {
	private Text txtOutputDirectory;
	private Text txtCSourceDirectory;

	IProject project;
	IEclipsePreferences projectNode;

	String PREF_KEY = "org.sireum.aadl.act";

	private final String KEY_OUTPUT_DIRECTORY = "output.directory";
	private final String KEY_C_SRC_DIRECTORY = "c.src.directory";

	/**
	 * @wbp.parser.constructor
	 */
	public ActPrompt(Shell parentShell) {
		super(parentShell);
	}

	public ActPrompt(IProject p, Shell parentShell) {
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
		setTitle("ACT Options");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		GridData gd_container = new GridData(GridData.FILL_BOTH);
		gd_container.widthHint = 556;
		container.setLayoutData(gd_container);

		Button btnOutputDirectory = new Button(container, SWT.NONE);
		btnOutputDirectory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				promptForOutputDirectory();
			}
		});
		btnOutputDirectory.setBounds(511, 3, 42, 28);
		btnOutputDirectory.setText("...");

		Label lblOutputDirectory = new Label(container, SWT.NONE);
		lblOutputDirectory.setBounds(10, 10, 121, 21);
		lblOutputDirectory.setText("Output Directory");

		txtOutputDirectory = new Text(container, SWT.BORDER);
		txtOutputDirectory.setBounds(166, 7, 334, 19);

		Label lblAuxCDir = new Label(container, SWT.NONE);
		lblAuxCDir.setText("C Source Code Directory");
		lblAuxCDir.setBounds(10, 46, 150, 21);

		txtCSourceDirectory = new Text(container, SWT.BORDER);
		txtCSourceDirectory.setBounds(166, 43, 334, 19);

		Button btnCSourceCode = new Button(container, SWT.NONE);
		btnCSourceCode.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				promptForCSourceDirectory();
			}
		});
		btnCSourceCode.setText("...");
		btnCSourceCode.setBounds(511, 39, 42, 28);

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
		return new Point(563, 372);
	}

	@Override
	protected void okPressed() {
		saveOptions();
		super.okPressed();
	}

	void saveOptions() {
		projectNode.put(KEY_OUTPUT_DIRECTORY, txtOutputDirectory.getText());
		projectNode.put(KEY_C_SRC_DIRECTORY, txtCSourceDirectory.getText());

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

		txtCSourceDirectory.setText(this.getOptionCSourceDirectory());
	}

	public String getOptionOutputDirectory() {
		return projectNode.get(KEY_OUTPUT_DIRECTORY, "");
	}

	public String getOptionCSourceDirectory() {
		return projectNode.get(KEY_C_SRC_DIRECTORY, "");
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

	void promptForCSourceDirectory() {
		DirectoryDialog d = new DirectoryDialog(this.getShell());
		if (!getOptionCSourceDirectory().equals("")) {
			d.setFilterPath(getOptionCSourceDirectory());
		} else {
			d.setFilterPath(project.getLocation().toString());
		}
		d.setText("Select C Source Directory Directory");
		String path = d.open();
		if (path != null) {
			txtCSourceDirectory.setText(path);
		}
	}
}
