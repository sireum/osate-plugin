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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

public class ActPrompt extends TitleAreaDialog {
	private static String title = "ACT Options";
	private static String message = "";

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

	@Override
	public void create() {
		super.create();

		setTitle(title);
		setMessage(message);
	}

	@Override
	protected boolean isResizable() {
		return true;

	}

	@Override
	public boolean isHelpAvailable() {
		return false;
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		int numCols = 3;

		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridLayout layout = new GridLayout(numCols, false);
		container.setLayout(layout);

		/****************************************************************
		 * ROW
		 ****************************************************************/
		{
			// COL 1
			Label lblOutputDirectory = new Label(container, SWT.NONE);
			lblOutputDirectory.setText("Output Directory");
			lblOutputDirectory.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

			// COL 2
			txtOutputDirectory = new Text(container, SWT.BORDER);
			GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.widthHint = 334;
			txtOutputDirectory.setLayoutData(gd);

			// COL 3
			Button btnOutputDirectory = new Button(container, SWT.PUSH);
			btnOutputDirectory.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					promptForOutputDirectory();
				}
			});
			btnOutputDirectory.setText("...");
			gd = new GridData(SWT.RIGHT, SWT.FILL, false, false);
			btnOutputDirectory.setLayoutData(gd);
		}

		/****************************************************************
		 * ROW
		 ****************************************************************/
		{
			// COL 1
			Label lblAuxCDir = new Label(container, SWT.NONE);
			lblAuxCDir.setText("C Source Code Directory");
			lblAuxCDir.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

			// COL 2
			txtCSourceDirectory = new Text(container, SWT.BORDER);
			txtCSourceDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			// COL 3
			Button btnCSourceCode = new Button(container, SWT.NONE);
			btnCSourceCode.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					promptForCSourceDirectory();
				}
			});
			btnCSourceCode.setText("...");
			btnCSourceCode.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		}

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

		String outputDirectory = getOptionOutputDirectory();
		if (outputDirectory.equals("")) {
			outputDirectory = project.getLocation().toString();
		}
		txtOutputDirectory.setText(outputDirectory);

		txtCSourceDirectory.setText(getOptionCSourceDirectory());
	}

	public String getOptionOutputDirectory() {
		return projectNode.get(KEY_OUTPUT_DIRECTORY, "");
	}

	public String getOptionCSourceDirectory() {
		return projectNode.get(KEY_C_SRC_DIRECTORY, "");
	}

	void promptForOutputDirectory() {
		DirectoryDialog d = new DirectoryDialog(getShell());
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
		DirectoryDialog d = new DirectoryDialog(getShell());
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
