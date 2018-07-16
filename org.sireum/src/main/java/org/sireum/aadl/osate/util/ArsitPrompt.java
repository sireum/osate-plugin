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
	private Text txtOutputDirectory;
	private Text txtBasePackageName;
	private Button btnGenerateTranspilerArtifacts;
	private Button btnEmbedArtProject;
	private Button btnGenerateBlessEntryPoints;
	private Combo cmbIPCMechanism;

	IProject project;
	IEclipsePreferences projectNode;

	String PREF_KEY = "org.sireum.aadl.arsit";

	private final String KEY_OUTPUT_DIRECTORY = "output.directory";
	private final String KEY_BASE_PACKAGE_NAME = "base.package.name";
	private final String KEY_EMBED_ART = "embed.art";
	private final String KEY_GEN_BLESS_ENTRY_POINTS = "gen.bless.entry.points";
	private final String KEY_GEN_TRANSPILER_ARTIFACTS = "gen.transpiler.artifacts";
	private final String KEY_IPC_MECHANISM = "ipc.mechanism";

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

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		setMessage("");
		setTitle("Arsit Options");
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

		txtBasePackageName = new Text(container, SWT.BORDER);
		txtBasePackageName.setBounds(137, 32, 334, 19);

		Label lblBasePackageName = new Label(container, SWT.NONE);
		lblBasePackageName.setText("Base Package Name");
		lblBasePackageName.setBounds(10, 35, 122, 21);

		btnGenerateBlessEntryPoints = new Button(container, SWT.CHECK);
		btnGenerateBlessEntryPoints.setBounds(10, 65, 209, 18);
		btnGenerateBlessEntryPoints.setText("Generate Bless Entry Points");

		btnEmbedArtProject = new Button(container, SWT.CHECK);
		btnEmbedArtProject.setText("Embed ART project files");
		btnEmbedArtProject.setBounds(10, 90, 209, 18);

		Group grpTranspilerOptions = new Group(container, SWT.NONE);
		grpTranspilerOptions.setText("Transpiler Options");
		grpTranspilerOptions.setBounds(10, 120, 451, 92);

		btnGenerateTranspilerArtifacts = new Button(grpTranspilerOptions, SWT.CHECK);
		btnGenerateTranspilerArtifacts.setBounds(10, 10, 411, 18);
		btnGenerateTranspilerArtifacts.setText("Generate Slang/C transpiler artifacts");

		cmbIPCMechanism = new Combo(grpTranspilerOptions, SWT.READ_ONLY);
		cmbIPCMechanism.setItems(new String[] { "Message Queue", "Shared Memory" });
		cmbIPCMechanism.setBounds(117, 19, 214, 55);

		Label lblIPCMechanism = new Label(grpTranspilerOptions, SWT.NONE);
		lblIPCMechanism.setBounds(10, 38, 101, 14);
		lblIPCMechanism.setText("IPC Mechanism");

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

		projectNode.put(KEY_BASE_PACKAGE_NAME, txtBasePackageName.getText());

		projectNode.putBoolean(KEY_EMBED_ART, btnEmbedArtProject.getSelection());

		projectNode.putBoolean(KEY_GEN_BLESS_ENTRY_POINTS, btnGenerateBlessEntryPoints.getSelection());

		projectNode.putBoolean(KEY_GEN_TRANSPILER_ARTIFACTS, btnGenerateTranspilerArtifacts.getSelection());

		projectNode.put(KEY_IPC_MECHANISM, cmbIPCMechanism.getText());

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

		txtBasePackageName.setText(getOptionBasePackageName());

		btnEmbedArtProject.setSelection(getOptionEmbedArt());

		btnGenerateBlessEntryPoints.setSelection(getOptionGenerateBlessEntryPoints());

		btnGenerateTranspilerArtifacts.setSelection(getOptionGenerateTranspilerArtifacts());

		String ipcMechanism = getOptionIPCMechanism();
		if (!ipcMechanism.equals("")) {
			cmbIPCMechanism.setText(ipcMechanism);
		}
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

	public boolean getOptionGenerateBlessEntryPoints() {
		return projectNode.getBoolean(KEY_GEN_BLESS_ENTRY_POINTS, false);
	}

	public boolean getOptionGenerateTranspilerArtifacts() {
		return projectNode.getBoolean(KEY_GEN_TRANSPILER_ARTIFACTS, false);
	}

	public String getOptionIPCMechanism() {
		return projectNode.get(KEY_IPC_MECHANISM, "");
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
