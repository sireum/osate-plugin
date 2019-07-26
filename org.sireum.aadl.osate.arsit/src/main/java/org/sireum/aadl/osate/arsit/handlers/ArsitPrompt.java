package org.sireum.aadl.osate.arsit.handlers;

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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

public class ArsitPrompt extends TitleAreaDialog {
	private static String title = "Arsit Options";
	private static String message = "";

	private Text txtOutputDirectory;
	private Text txtBasePackageName;
	private Button btnGenerateTranspilerArtifacts;
	private Button btnEmbedArtProject;
	private Combo cmbIPCMechanism;

	private Button btnGenerateBlessEntryPoints;

	IProject project;
	IEclipsePreferences projectNode;

	String PREF_KEY = "org.sireum.aadl.arsit";

	private final String KEY_OUTPUT_DIRECTORY = "output.directory";
	private final String KEY_BASE_PACKAGE_NAME = "base.package.name";
	private final String KEY_EMBED_ART = "embed.art";
	private final String KEY_GEN_TRANSPILER_ARTIFACTS = "gen.transpiler.artifacts";
	private final String KEY_IPC_MECHANISM = "ipc.mechanism";
	private final String KEY_GEN_BLESS_ENTRY_POINTS = "gen.bless.entry.points";

	/**
	 * @wbp.parser.constructor
	 */
	public ArsitPrompt(Shell parentShell) {
		super(parentShell);
	}

	public ArsitPrompt(IProject p, Shell parentShell) {
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
			Label lblBasePackageName = new Label(container, SWT.NONE);
			lblBasePackageName.setText("Base Package Name");
			lblBasePackageName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

			// COL 2
			txtBasePackageName = new Text(container, SWT.BORDER);
			txtBasePackageName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			// COL 3
			new Label(container, SWT.NULL); // col padding
		}

		fillerRow(container, numCols);

		/****************************************************************
		 * ROW
		 ****************************************************************/
		{
			btnEmbedArtProject = new Button(container, SWT.CHECK);
			btnEmbedArtProject.setText("Embed ART project files");
			btnEmbedArtProject.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		}

		/****************************************************************
		 * ROW
		 ****************************************************************/
		{
			btnGenerateBlessEntryPoints = new Button(container, SWT.CHECK);
			btnGenerateBlessEntryPoints.setText("Generate BLESS Entrypoints");
			btnGenerateBlessEntryPoints.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		}

		fillerRow(container, numCols);

		/****************************************************************
		 * ROW
		 ****************************************************************/
		{
			// COL 1
			Group grpTranspilerOptions = new Group(container, SWT.NONE);
			grpTranspilerOptions.setText("Transpiler Options");
			grpTranspilerOptions.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));

			layout = new GridLayout(2, false);
			grpTranspilerOptions.setLayout(layout);

			btnGenerateTranspilerArtifacts = new Button(grpTranspilerOptions, SWT.CHECK);
			btnGenerateTranspilerArtifacts.setText("Generate Slang/C transpiler artifacts");
			btnGenerateTranspilerArtifacts.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

			Label lblIPCMechanism = new Label(grpTranspilerOptions, SWT.NONE);
			lblIPCMechanism.setText("IPC Mechanism");
			lblIPCMechanism.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

			cmbIPCMechanism = new Combo(grpTranspilerOptions, SWT.READ_ONLY);
			cmbIPCMechanism.setItems(new String[] { "Message Queue", "Shared Memory" });
			cmbIPCMechanism.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
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

		projectNode.put(KEY_BASE_PACKAGE_NAME, txtBasePackageName.getText());

		projectNode.putBoolean(KEY_EMBED_ART, btnEmbedArtProject.getSelection());

		projectNode.putBoolean(KEY_GEN_TRANSPILER_ARTIFACTS, btnGenerateTranspilerArtifacts.getSelection());

		projectNode.put(KEY_IPC_MECHANISM, cmbIPCMechanism.getText());

		projectNode.putBoolean(KEY_GEN_BLESS_ENTRY_POINTS, btnGenerateBlessEntryPoints.getSelection());

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

		txtBasePackageName.setText(getOptionBasePackageName());

		btnEmbedArtProject.setSelection(getOptionEmbedArt());

		btnGenerateTranspilerArtifacts.setSelection(getOptionGenerateTranspilerArtifacts());

		String ipcMechanism = getOptionIPCMechanism();
		if (!ipcMechanism.equals("")) {
			cmbIPCMechanism.setText(ipcMechanism);
		}

		btnGenerateBlessEntryPoints.setSelection(getOptionGenerateBlessEntryPoints());
	}

	public boolean getOptionGenerateBlessEntryPoints() {
		return projectNode.getBoolean(KEY_GEN_BLESS_ENTRY_POINTS, false);
	}

	public String getOptionOutputDirectory() {
		return projectNode.get(KEY_OUTPUT_DIRECTORY, "");
	}

	public String getOptionBasePackageName() {
		return projectNode.get(KEY_BASE_PACKAGE_NAME, "");
	}

	public boolean getOptionEmbedArt() {
		return projectNode.getBoolean(KEY_EMBED_ART, true);
	}

	public boolean getOptionGenerateTranspilerArtifacts() {
		return projectNode.getBoolean(KEY_GEN_TRANSPILER_ARTIFACTS, false);
	}

	public String getOptionIPCMechanism() {
		return projectNode.get(KEY_IPC_MECHANISM, "");
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

	private void fillerRow(Composite container, int numCols) {
		int height = 10;
		Label l = new Label(container, SWT.NULL);
		l.setSize(0, height);
		GridData x = new GridData(SWT.LEFT, SWT.FILL, false, false, numCols, 1);
		x.heightHint = height;
		l.setLayoutData(x);
	}
}