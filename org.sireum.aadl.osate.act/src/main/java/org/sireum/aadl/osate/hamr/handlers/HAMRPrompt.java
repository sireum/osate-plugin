package org.sireum.aadl.osate.hamr.handlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

enum OutputProfile {
	Linux, seL4
}

public class HAMRPrompt extends TitleAreaDialog {
	private static String title = "HAMR Configuration";
	private String subTitle = "";
	private static String message = "";

	private Combo cmbOutputProfile;
	private Text txtCSourceDirectory;
	private Text txtSlangOutputDirectory;
	private Text txtCamkesOutputDirectory;
	private Button btnExcludeSlangImplementations;
	private Text txtBasePackageName;

	IProject project;
	IEclipsePreferences projectNode;

	String PREF_KEY = "org.sireum.aadl.hamr";

	private final String KEY_OUTPUT_PROFILE = "output.profile";
	private final String KEY_C_SRC_DIRECTORY = "c.src.directory";

	private final String KEY_SLANG_OUTPUT_DIRECTORY = "slang.output.directory";
	private final String KEY_CAMKES_OUTPUT_DIRECTORY = "camkes.output.directory";

	private final String KEY_EXCLUDE_SLANG_IMPL = "exclude.slang.impl";

	private final String KEY_BASE_PACKAGE_NAME = "base.package.name";

	Map<String, List<Control>> controls = new HashMap<>();

	// The image to display
	private Image image;

	public HAMRPrompt(Shell parentShell) {
		super(parentShell);
	}

	public HAMRPrompt(IProject p, Shell parentShell, String title) {
		super(parentShell);
		subTitle = title;
		project = p;
		IScopeContext projectScope = new ProjectScope(project);
		projectNode = projectScope.getNode(PREF_KEY);
	}

	@Override
	public void create() {
		super.create();

		setTitle(title + ": " + subTitle);
		setMessage(message);

		image = new Image(null, new ImageData(this.getClass().getResourceAsStream("/resources/hamr.png")));
		setTitleImage(image);
	}

	@Override
	public boolean close() {
		if (image != null) {
			image.dispose();
		}
		return super.close();
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
		 * ROW - Target Platform
		 ****************************************************************/
		{
			// COL 1
			Label lblOutputProfile = new Label(container, SWT.NONE);
			lblOutputProfile.setText("Target Platform");
			lblOutputProfile.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

			// COL 2
			cmbOutputProfile = new Combo(container, SWT.READ_ONLY);
			String[] vals = Arrays.asList(OutputProfile.values()).stream().map(f -> f.toString()).toArray(String[]::new);
			cmbOutputProfile.setItems(vals);
			GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
			gd.widthHint = 100;
			cmbOutputProfile.setLayoutData(gd);

			cmbOutputProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setCamkesOptionsVisible();
				}
			});

			// COL 3
			new Label(container, SWT.NULL); // col padding
		}


		/****************************************************************
		 * ROW - Slang Output Directory
		 ****************************************************************/

		{
			// COL 1
			Label lblOutputDirectory = new Label(container, SWT.NONE);
			lblOutputDirectory.setText("Output Directory");
			lblOutputDirectory.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

			// COL 2
			txtSlangOutputDirectory = new Text(container, SWT.BORDER);
			GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.widthHint = 334;
			txtSlangOutputDirectory.setLayoutData(gd);

			// COL 3
			Button btnOutputDirectory = new Button(container, SWT.PUSH);
			btnOutputDirectory.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String path = promptForDirectory("Select Slang Output Directory",
							getSlangOptionOutputDirectory());
					if (path != null) {
						txtSlangOutputDirectory.setText(path);
					}
				}
			});
			btnOutputDirectory.setText("...");
			gd = new GridData(SWT.RIGHT, SWT.FILL, false, false);
			btnOutputDirectory.setLayoutData(gd);
		}

		/****************************************************************
		 * ROW - C Source Code Directory
		 ****************************************************************/
		{
			// COL 1
			Label lblAuxCDir = new Label(container, SWT.NONE);
			lblAuxCDir.setText("C Source Code Directory");
			lblAuxCDir.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

			// COL 2
			txtCSourceDirectory = new Text(container, SWT.BORDER);
			txtCSourceDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			txtCSourceDirectory.setToolTipText("Directory containing component implementations");

			// COL 3
			Button btnCSourceCode = new Button(container, SWT.NONE);
			btnCSourceCode.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String path = promptForDirectory("Select C Source Directory Directory",
							getOptionCSourceDirectory());
					if (path != null) {
						txtCSourceDirectory.setText(path);
					}
				}
			});
			btnCSourceCode.setText("...");
			btnCSourceCode.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

			controls.put(KEY_C_SRC_DIRECTORY, Arrays.asList(lblAuxCDir, txtCSourceDirectory, btnCSourceCode));
			this.setOptionsVisible(KEY_C_SRC_DIRECTORY, true);
		}

		/****************************************************************
		 * ROW - Camkes Output Directory
		 ****************************************************************/

		{
			// COL 1
			Label lblOutputDirectory = new Label(container, SWT.NONE);
			lblOutputDirectory.setText("seL4/CAmkES Output Directory");
			lblOutputDirectory.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

			// COL 2
			txtCamkesOutputDirectory = new Text(container, SWT.BORDER);
			GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.widthHint = 334;
			txtCamkesOutputDirectory.setLayoutData(gd);

			// COL 3
			Button btnOutputDirectory = new Button(container, SWT.PUSH);
			btnOutputDirectory.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String path = promptForDirectory("Select SeL4/CAmkES Output Directory",
							getCamkesOptionOutputDirectory());
					if (path != null) {
						txtCamkesOutputDirectory.setText(path);
					}
				}
			});
			btnOutputDirectory.setText("...");
			gd = new GridData(SWT.RIGHT, SWT.FILL, false, false);
			btnOutputDirectory.setLayoutData(gd);

			controls.put(KEY_CAMKES_OUTPUT_DIRECTORY,
					Arrays.asList(lblOutputDirectory, txtCamkesOutputDirectory, btnOutputDirectory));

			setCamkesOptionsVisible();
		}

		// fillerRow(container, numCols);

		/****************************************************************
		 * ROW - Exclude Slang Impl
		 ****************************************************************/
		{
			btnExcludeSlangImplementations = new Button(container, SWT.CHECK);
			btnExcludeSlangImplementations.setText("Exclude Slang Component Implementations");
			btnExcludeSlangImplementations.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));

			controls.put(KEY_EXCLUDE_SLANG_IMPL, Arrays.asList(btnExcludeSlangImplementations));
			this.setOptionsVisible(KEY_EXCLUDE_SLANG_IMPL, false);
		}

		// fillerRow(container, numCols);

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

			controls.put(KEY_BASE_PACKAGE_NAME, Arrays.asList(lblBasePackageName, txtBasePackageName));
			this.setOptionsVisible(KEY_BASE_PACKAGE_NAME, false);
		}

		initValues();
		return area;
	}

	void setCamkesOptionsVisible() {
		String val = cmbOutputProfile.getText();
		boolean visible = (val != null && !val.isEmpty() && OutputProfile.valueOf(val) == OutputProfile.seL4);
		setOptionsVisible(KEY_CAMKES_OUTPUT_DIRECTORY, visible);
	}

	void setOptionsVisible(String key, boolean b) {
		List<Control> cntls = controls.get(key);
		for (Control c : cntls) {
			c.setVisible(b);
		}
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
		projectNode.put(KEY_OUTPUT_PROFILE, cmbOutputProfile.getText());

		projectNode.putBoolean(KEY_EXCLUDE_SLANG_IMPL, btnExcludeSlangImplementations.getSelection());

		projectNode.put(KEY_C_SRC_DIRECTORY, txtCSourceDirectory.getText());

		projectNode.put(KEY_SLANG_OUTPUT_DIRECTORY, txtSlangOutputDirectory.getText());

		projectNode.put(KEY_CAMKES_OUTPUT_DIRECTORY, txtCamkesOutputDirectory.getText());

		projectNode.put(KEY_BASE_PACKAGE_NAME, txtBasePackageName.getText());

		try {
			projectNode.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	void initValues() {

		cmbOutputProfile.setText(getOptionOutputProfile().name());
		setCamkesOptionsVisible();

		btnExcludeSlangImplementations.setSelection(getOptionExcludesSlangImplementations());

		txtCSourceDirectory.setText(getOptionCSourceDirectory());

		String slangOutputDirectory = getSlangOptionOutputDirectory();
		if (slangOutputDirectory.equals("")) {
			slangOutputDirectory = project.getLocation().toString();
		}
		txtSlangOutputDirectory.setText(slangOutputDirectory);

		String camkesOutputDirectory = getCamkesOptionOutputDirectory();
		if (camkesOutputDirectory.equals("")) {
			camkesOutputDirectory = project.getLocation().toString();
		}
		txtCamkesOutputDirectory.setText(camkesOutputDirectory);

		txtBasePackageName.setText(getOptionBasePackageName());
	}

	public OutputProfile getOptionOutputProfile() {
		String val = projectNode.get(KEY_OUTPUT_PROFILE, "");
		if (val.isEmpty()) {
			return OutputProfile.Linux;
		} else {
			return OutputProfile.valueOf(val);
		}
	}

	public boolean getOptionExcludesSlangImplementations() {
		return projectNode.getBoolean(KEY_EXCLUDE_SLANG_IMPL, false);
	}

	public String getOptionCSourceDirectory() {
		return projectNode.get(KEY_C_SRC_DIRECTORY, "");
	}

	public String getSlangOptionOutputDirectory() {
		return projectNode.get(KEY_SLANG_OUTPUT_DIRECTORY, "");
	}

	public String getCamkesOptionOutputDirectory() {
		return projectNode.get(KEY_CAMKES_OUTPUT_DIRECTORY, "");
	}

	public String getOptionBasePackageName() {
		return projectNode.get(KEY_BASE_PACKAGE_NAME, "");
	}

	String promptForDirectory(String title, String init) {
		DirectoryDialog d = new DirectoryDialog(getShell());
		if (init != null && !init.equals("")) {
			d.setFilterPath(init);
		} else {
			d.setFilterPath(project.getLocation().toString());
		}
		d.setText(title);
		return d.open();
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
