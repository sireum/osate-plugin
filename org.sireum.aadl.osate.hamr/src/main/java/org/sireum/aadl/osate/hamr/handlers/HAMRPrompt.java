package org.sireum.aadl.osate.hamr.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osate.ui.dialogs.Dialog;
import org.osgi.service.prefs.BackingStoreException;
import org.sireum.Option;
import org.sireum.aadl.arsit.ArsitBridge;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.HW;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.Platform;

public class HAMRPrompt extends TitleAreaDialog {

	private final String KEY_PLATFORM = "platform";

	private final String KEY_SLANG_OUTPUT_DIRECTORY = "slang.output.directory";
	private final String KEY_BASE_PACKAGE_NAME = "base.package.name";

	private final String KEY_HW = "hw";

	private final String KEY_EXCLUDE_SLANG_IMPL = "exclude.slang.impl";
	private final String KEY_BIT_WIDTH = "bit.width";
	private final String KEY_MAX_SEQUENCE_SIZE = "max.sequence.size";
	private final String KEY_MAX_STRING_SIZE = "max.string.size";
	private final String KEY_C_SRC_DIRECTORY = "c.src.directory";

	private final String KEY_CAMKES_OUTPUT_DIRECTORY = "camkes.output.directory";
	private final String KEY_TRUSTED_BUILD_PROFILE = "trusted.build.profile";
	private final String KEY_CAMKES_AUX_SRC_DIR = "camkes.aux.src.dir";

	public ArsitBridge.Platform getOptionPlatform() {
		Platform p = Platform.valueOf(getSavedStringOption(KEY_PLATFORM));
		// slang cligen causes first letter of enum value to always be upper case
		return ArsitBridge.Platform.valueOf(p.name().substring(0, 1).toUpperCase() + p.name().substring(1));
	}

	public HW getOptionHW() {
		return HW.valueOf(getSavedStringOption(KEY_HW));
	}

	public String getOptionCSourceDirectory() {
		return getSavedStringOption(KEY_C_SRC_DIRECTORY);
	}

	public String getSlangOptionOutputDirectory() {
		return getSavedStringOption(KEY_SLANG_OUTPUT_DIRECTORY);
	}

	public boolean getOptionExcludesSlangImplementations() {
		return getSavedBooleanOption(KEY_EXCLUDE_SLANG_IMPL);
	}

	public String getOptionBasePackageName() {
		return getSavedStringOption(KEY_BASE_PACKAGE_NAME);
	}

	public int getOptionMaxSequenceSize() {
		return Integer.valueOf(getSavedStringOption(KEY_MAX_SEQUENCE_SIZE));
	}

	public int getOptionMaxStringSize() {
		return Integer.valueOf(getSavedStringOption(KEY_MAX_STRING_SIZE));
	}

	public int getOptionBitWidth() {
		return Integer.valueOf(getSavedStringOption(KEY_BIT_WIDTH));
	}

	public String getOptionCamkesOptionOutputDirectory() {
		return getSavedStringOption(KEY_CAMKES_OUTPUT_DIRECTORY);
	}

	public boolean getOptionTrustedBuildProfile() {
		return getSavedBooleanOption(KEY_TRUSTED_BUILD_PROFILE);
	}

	public String getOptionCamkesAuxSrcDir() {
		return getSavedStringOption(KEY_CAMKES_AUX_SRC_DIR);
	}

	/* runs after controls have been created */
	private void initControlValues() {
		// get previous values from eclipse project
		for (Entry<String, Control> option : optionControls.entrySet()) {
			if (option.getValue() instanceof Text) {
				((Text) option.getValue()).setText(getSavedStringOption(option.getKey()));
			} else if (option.getValue() instanceof Combo) {
				((Combo) option.getValue()).setText(getSavedStringOption(option.getKey()));
			} else if (option.getValue() instanceof Button) {
				((Button) option.getValue()).setSelection(getSavedBooleanOption(option.getKey()));
			} else {
				throw new RuntimeException();
			}
		}

		// SPECIAL INIT CASES
		String slangOutputDirectory = getSlangOptionOutputDirectory();
		if (slangOutputDirectory.equals("")) {
			slangOutputDirectory = project.getLocation().toString();
		}
		getTextControl(KEY_SLANG_OUTPUT_DIRECTORY).setText(slangOutputDirectory);

		String camkesOutputDirectory = getOptionCamkesOptionOutputDirectory();
		if (camkesOutputDirectory.equals("")) {
			camkesOutputDirectory = project.getLocation().toString();
		}
		getTextControl(KEY_CAMKES_OUTPUT_DIRECTORY).setText(camkesOutputDirectory);

		getComboControl(KEY_BIT_WIDTH).select(HAMRPropertyProvider.bitWidths.indexOf(theBitWidth));
		getTextControl(KEY_MAX_SEQUENCE_SIZE).setText(Integer.toString(theMaxSequenceSize));
		getTextControl(KEY_MAX_STRING_SIZE).setText(Integer.toString(theMaxStringSize));
	}

	private boolean validate() {
		Option<Integer> mss = getIntFromControl(KEY_MAX_SEQUENCE_SIZE);
		if (mss.isEmpty() || mss.get().intValue() < 0) {
			Dialog.showError("Input Error", "Max Sequence Size must be a number greater than or equal to 0");
			return false;
		}

		Option<Integer> mstring = getIntFromControl(KEY_MAX_STRING_SIZE);
		if (mstring.isEmpty() || mstring.get().intValue() < 0) {
			Dialog.showError("Input Error", "Max String Size must be a number greater than or equal to 0");
			return false;
		}

		return true;
	}

	/*
	 * EVERYTHING ELSE
	 */
	private static String title = "HAMR Configuration";
	private String subTitle = "";
	private static String message = "";

	private IProject project;
	private IEclipsePreferences projectNode;

	private final String PREF_KEY = "org.sireum.aadl.hamr";

	private final String KEY_TRANSPILER_GROUP = "KEY_GROUP_TRANSPILER";
	private final String KEY_CAMKES_GROUP = "KEY_GROUP_CAMKES";

	// The image to display
	private Image image;

	private List<Platform> thePlatforms = null;
	private List<HW> theHardwares = null;
	private int theBitWidth = -1;
	private int theMaxSequenceSize = -1;
	private int theMaxStringSize = -1;

	public HAMRPrompt(Shell parentShell) {
		super(parentShell);
	}

	public HAMRPrompt(IProject p, Shell parentShell, String title, List<Platform> platforms, List<HW> hardwares,
			int bitWidth, int maxSequenceSize, int maxStringSize) {
		super(parentShell);
		subTitle = title;
		project = p;

		thePlatforms = platforms;
		theHardwares = hardwares;
		theBitWidth = bitWidth;
		theMaxSequenceSize = maxSequenceSize;
		theMaxStringSize = maxStringSize;

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
		final int numCols = 3;

		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final GridLayout layout = new GridLayout(numCols, false);
		container.setLayout(layout);

		/****************************************************************
		 * ROW - Platform
		 ****************************************************************/
		{
			final String key = KEY_PLATFORM;

			// COL 1
			addLabel("Platform", container, key);

			// COL 2
			Combo cmb = new Combo(container, SWT.READ_ONLY);
			cmb.setItems(filterPlatforms());
			GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
			gd.widthHint = 100;
			cmb.setLayoutData(gd);
			registerOptionControl(key, cmb);

			cmb.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					computeVisibilityAndPack(container);
				}
			});

			// COL 3
			registerViewControl(key, addColumnPad(container)); // col padding
		}

		/****************************************************************
		 * ROW - HW
		 ****************************************************************/
		{
			final String key = KEY_HW;

			// COL 1
			addLabel("HW", container, key);

			// COL 2
			Combo cmb = new Combo(container, SWT.READ_ONLY);
			GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
			gd.widthHint = 100;
			cmb.setLayoutData(gd);
			cmb.setItems(getHWs());
			registerOptionControl(key, cmb);

			cmb.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {

				}
			});

			// COL 3
			registerViewControl(key, addColumnPad(container)); // col padding
		}

		/****************************************************************
		 * ROW - Slang Output Directory
		 ****************************************************************/

		{
			final String key = KEY_SLANG_OUTPUT_DIRECTORY;

			// COL 1
			addLabel("Output Directory", container, key);

			// COL 2
			Text txt = new Text(container, SWT.BORDER);
			GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.widthHint = 334;
			txt.setLayoutData(gd);
			registerOptionControl(key, txt);

			// COL 3
			Button btn = new Button(container, SWT.PUSH);
			btn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String path = promptForDirectory("Select Slang Output Directory", getSlangOptionOutputDirectory());
					if (path != null) {
						txt.setText(path);
					}
				}
			});
			btn.setText("...");
			gd = new GridData(SWT.RIGHT, SWT.FILL, false, false);
			btn.setLayoutData(gd);
			registerViewControl(key, btn);
		}

		/****************************************************************
		 * ROW
		 ****************************************************************/
		{
			final String key = KEY_BASE_PACKAGE_NAME;

			// COL 1
			addLabel("Base Package Name", container, key);

			// COL 2
			Text txt = new Text(container, SWT.BORDER);
			txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			registerOptionControl(key, txt);

			// COL 3
			registerViewControl(key, addColumnPad(container)); // col padding
		}

		fillerRow(container, numCols);

		/****************************************************************
		 * ROW
		 ****************************************************************/
		{
			final String key = KEY_TRANSPILER_GROUP;
			final int numGroupCols = 3;

			// COL 1
			Group grpContainer = new Group(container, SWT.NONE);
			grpContainer.setText("Transpiler Options");
			grpContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, numCols, 1));
			registerViewControl(key, grpContainer);

			final GridLayout grpLayout = new GridLayout(numGroupCols, false);
			grpContainer.setLayout(grpLayout);

			/****************************************************************
			 * GROUP ROW - Exclude Slang Impl
			 ****************************************************************/
			{
				String subKey = KEY_EXCLUDE_SLANG_IMPL;

				Button btn = new Button(grpContainer, SWT.CHECK);
				btn.setText("Exclude Slang Component Implementations");
				btn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, numGroupCols, 1));
				registerOptionControl(subKey, btn, false);
			}

			/****************************************************************
			 * GROUP ROW - Bit Width
			 ****************************************************************/
			{
				String subKey = KEY_BIT_WIDTH;

				// COL 1
				addLabel("Bit Width", grpContainer);

				// COL 2
				Combo cmb = new Combo(grpContainer, SWT.READ_ONLY);
				cmb.setItems(getBitWidths());
				GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
				gd.widthHint = 100;
				cmb.setLayoutData(gd);
				registerOptionControl(subKey, cmb, false);

				// COL 3
				addColumnPad(grpContainer);
			}

			/****************************************************************
			 * GROUP ROW - Max Sequence Size
			 ****************************************************************/
			{
				String subKey = KEY_MAX_SEQUENCE_SIZE;

				// COL 1
				addLabel("Max Sequence Size", grpContainer);

				// COL 2
				Text txt = new Text(grpContainer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				registerOptionControl(subKey, txt, false);

				// COL 3
				addColumnPad(grpContainer);
			}

			/****************************************************************
			 * GROUP ROW - Max String Size
			 ****************************************************************/
			{
				String subKey = KEY_MAX_STRING_SIZE;

				// COL 1
				addLabel("Max String Size", grpContainer);

				// COL 2
				Text txt = new Text(grpContainer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				registerOptionControl(subKey, txt, false);

				// COL 3
				addColumnPad(grpContainer);
			}

			/****************************************************************
			 * GROUP ROW - C Source Code Directory
			 ****************************************************************/
			{
				String subKey = KEY_C_SRC_DIRECTORY;

				// COL 1
				addLabel("Aux Code Directory", grpContainer);

				// COL 2
				Text txt = new Text(grpContainer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				txt.setToolTipText("Directory containing C code to be included in HAMR project");
				registerOptionControl(subKey, txt, false);

				// COL 3
				Button btn = new Button(grpContainer, SWT.NONE);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String path = promptForDirectory("Select C Source Directory Directory",
								getOptionCSourceDirectory());
						if (path != null) {
							txt.setText(path);
						}
					}
				});
				btn.setText("...");
				btn.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				// registerViewControl(subKey, btn);
			}
		}

		fillerRow(container, numCols);

		/****************************************************************
		 * ROW
		 ****************************************************************/
		{
			final String key = KEY_CAMKES_GROUP;
			final int numGroupCols = 3;

			// COL 1
			Group grpContainer = new Group(container, SWT.NONE);
			grpContainer.setText("CAmkES Options");
			grpContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, numCols, 1));
			registerViewControl(key, grpContainer);

			final GridLayout grpLayout = new GridLayout(numGroupCols, false);
			grpContainer.setLayout(grpLayout);

			/****************************************************************
			 * ROW - trusted build profile
			 ****************************************************************/

			{
				final String subKey = KEY_TRUSTED_BUILD_PROFILE;

				// COL 1
				Button btn = new Button(grpContainer, SWT.CHECK);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						computeVisibilityAndPack(container);
					}
				});
				btn.setText("Trusted Build Profile");
				btn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, numGroupCols, 1));
				registerOptionControl(subKey, btn, false);
			}

			/****************************************************************
			 * ROW - Camkes Output Directory
			 ****************************************************************/

			{
				final String subKey = KEY_CAMKES_OUTPUT_DIRECTORY;

				// COL 1
				addLabel("seL4/CAmkES Output Directory", grpContainer);

				// COL 2
				Text txt = new Text(grpContainer, SWT.BORDER);
				GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
				gd.widthHint = 334;
				txt.setLayoutData(gd);
				registerOptionControl(subKey, txt, false);

				// COL 3
				Button btn = new Button(grpContainer, SWT.PUSH);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String path = promptForDirectory("Select SeL4/CAmkES Output Directory",
								getOptionCamkesOptionOutputDirectory());
						if (path != null) {
							txt.setText(path);
						}
					}
				});
				btn.setText("...");
				gd = new GridData(SWT.RIGHT, SWT.FILL, false, false);
				btn.setLayoutData(gd);
				// registerViewControl(subKey, btn);
			}

			/****************************************************************
			 * ROW - Camkes Aux Source Directory
			 ****************************************************************/

			{
				final String subKey = KEY_CAMKES_AUX_SRC_DIR;

				// COL 1
				addLabel("Aux Code Directory for CAmkES", grpContainer);

				// COL 2
				Text txt = new Text(grpContainer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				txt.setToolTipText("Directory containing C code to be included in CAmkES project");
				registerOptionControl(subKey, txt, false);

				// COL 3
				Button btn = new Button(grpContainer, SWT.NONE);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String path = promptForDirectory("Select Aux Source Directory Directory",
								getOptionCSourceDirectory());
						if (path != null) {
							txt.setText(path);
						}
					}
				});
				btn.setText("...");
				btn.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
			}
		}

		initControlValues();

		computeVisibility(container);

		return area;
	}

	protected List<String> addAll(List<String> controls, List<String> other) {
		List<String> ret = new ArrayList<>(controls);
		ret.addAll(other);
		return ret;
	}

	private Label addLabel(String label, Composite container) {
		Label l = new Label(container, SWT.NONE);
		l.setText(label);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		return l;
	}

	private Label addLabel(String label, Composite container, String key) {
		Label l = addLabel(label, container);
		registerViewControl(key, l);
		return l;
	}

	private Control addColumnPad(Composite container) {
		Label l = new Label(container, SWT.NULL); // col padding
		GridData d = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		l.setLayoutData(d);
		return l;
	}

	private final Map<String, Collection<Control>> viewControlMap = new HashMap<>();
	private final Map<String, Control> optionControls = new HashMap<>();

	private Combo getComboControl(String key) {
		if (optionControls.containsKey(key) && optionControls.get(key) instanceof Combo) {
			return (Combo) optionControls.get(key);
		} else {
			return null;
		}
	}

	private Text getTextControl(String key) {
		if (optionControls.containsKey(key) && optionControls.get(key) instanceof Text) {
			return (Text) optionControls.get(key);
		} else {
			return null;
		}
	}

	private Control registerOptionControl(String KEY, Control control) {
		return registerOptionControl(KEY, control, true);
	}

	private Control registerOptionControl(String KEY, Control control, boolean addToViewControl) {
		assert (!optionControls.containsKey(KEY));
		optionControls.put(KEY, control);
		if (addToViewControl) {
			registerViewControl(KEY, control);
		}
		return control;
	}

	private Control registerViewControl(String KEY, Control control) {
		assert (control.getLayoutData() instanceof GridData);
		if (!viewControlMap.containsKey(KEY)) {
			viewControlMap.put(KEY, new ArrayList<>());
		}
		viewControlMap.get(KEY).add(control);
		return control;
	}

	protected void showOnly(Composite container, List<String> keys) {
		for (Entry<String, Collection<Control>> e : viewControlMap.entrySet()) {
			boolean makeVisible = keys.contains(e.getKey());
			for (Control c : e.getValue()) {
				GridData gd = (GridData) c.getLayoutData();
				gd.exclude = !makeVisible;
				c.setVisible(makeVisible);
			}
		}
	}

	private void show(boolean show, Composite container, String... KEYS) {
		for (String KEY : KEYS) {
			if (viewControlMap.containsKey(KEY)) {
				for (Control c : viewControlMap.get(KEY)) {
					GridData gd = (GridData) c.getLayoutData();
					gd.exclude = !show;
					c.setVisible(show);
				}
			}
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
		if (validate()) {
			saveOptions();
			super.okPressed();
		}
	}

	/* save value of the control fields to eclipse project */
	private void saveOptions() {
		for (Entry<String, Control> option : optionControls.entrySet()) {
			if (option.getValue() instanceof Text) {
				projectNode.put(option.getKey(), ((Text) option.getValue()).getText());
			} else if (option.getValue() instanceof Combo) {
				projectNode.put(option.getKey(), ((Combo) option.getValue()).getText());
			} else if (option.getValue() instanceof Button) {
				projectNode.putBoolean(option.getKey(), ((Button) option.getValue()).getSelection());
			} else {
				throw new RuntimeException();
			}
		}

		try {
			projectNode.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private boolean getSavedBooleanOption(String key) {
		return projectNode.getBoolean(key, false);
	}

	private String getSavedStringOption(String key) {
		return projectNode.get(key, "");
	}

	private String promptForDirectory(String title, String init) {
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

	private String[] getBitWidths() {
		return HAMRPropertyProvider.bitWidths.stream().map(f -> f.toString()).toArray(String[]::new);
	}

	private String[] getHWs() {
		return Arrays.asList(HAMRPropertyProvider.HW.values()).stream().map(f -> f.toString()).toArray(String[]::new);
	}

	private String[] filterPlatforms() {
		if (thePlatforms.isEmpty()) {
			return Arrays.asList(Platform.values()).stream().map(f -> f.toString()).toArray(String[]::new);
		} else {
			return thePlatforms.stream().map(f -> f.toString()).toArray(String[]::new);
		}
	}

	private String[] filterHW(HW... validOption) {
		return Arrays.asList(validOption).stream().distinct().filter(theHardwares::contains).collect(Collectors.toSet())
				.stream().map(f -> f.toString()).toArray(String[]::new);
	}

	private Option<String> getStringFromControl(String key) {
		if (optionControls.containsKey(key)) {
			Control c = optionControls.get(key);
			if (c instanceof Text) {
				return org.sireum.Some$.MODULE$.apply(((Text) c).getText());
			} else if (c instanceof Combo) {
				return org.sireum.Some$.MODULE$.apply(((Combo) c).getText());
			} else if (c instanceof Button) {
				return org.sireum.Some$.MODULE$.apply(new Boolean(((Button) c).getSelection()).toString());
			}
		}
		return org.sireum.None.apply();
	}

	private Option<Integer> getIntFromControl(String key) {
		try {
			return org.sireum.Some$.MODULE$.apply(new Integer(getStringFromControl(key).get()));
		} catch (Exception nfe) {
		}
		return org.sireum.None.apply();
	}

	private Option<Boolean> getBooleanFromControl(String key) {
		try {
			return org.sireum.Some$.MODULE$.apply(new Boolean(getStringFromControl(key).get()));
		} catch (Exception nfe) {
		}
		return org.sireum.None.apply();
	}

	private void computeVisibilityAndPack(Composite container) {
		computeVisibility(container);
		container.pack();
		getShell().pack();
		getShell().layout();
	}

	private void computeVisibility(Composite container) {
		Combo cmb = getComboControl(KEY_PLATFORM);

		if (cmb.getSelectionIndex() < 0) {
			showOnly(container, Arrays.asList(KEY_PLATFORM));
			return;
		}

		String selection = cmb.getItem(cmb.getSelectionIndex());
		Platform p = HAMRPropertyProvider.Platform.valueOf(selection);

		List<String> JVM_controls = Arrays.asList( //
				KEY_PLATFORM, KEY_SLANG_OUTPUT_DIRECTORY, KEY_BASE_PACKAGE_NAME);

		List<String> NIX_controls = addAll(JVM_controls, Arrays.asList( //
				KEY_HW, KEY_C_SRC_DIRECTORY, KEY_EXCLUDE_SLANG_IMPL, //
				KEY_TRANSPILER_GROUP));

		List<String> SEL4_controls = addAll(NIX_controls, Arrays.asList( //
				KEY_CAMKES_GROUP));

		List<String> toShow = JVM_controls;

		String[] hwItems = null;
		switch (p) {
		case JVM:
			hwItems = filterHW();
			break;
		case Linux:
			hwItems = filterHW(HW.x86, HW.amd64);
			toShow = NIX_controls;
			break;
		case macOS:
			hwItems = filterHW(HW.amd64);
			toShow = NIX_controls;
			break;
		case Cygwin:
			hwItems = filterHW(HW.x86, HW.amd64);
			toShow = NIX_controls;
			break;
		case seL4:
			hwItems = filterHW(HW.QEMU, HW.ODROID_XU4);
			toShow = SEL4_controls;
			break;
		}
		getComboControl(KEY_HW).setItems(hwItems);

		Option<Boolean> tb = getBooleanFromControl(KEY_TRUSTED_BUILD_PROFILE);
		if (p == Platform.seL4 && tb.nonEmpty() && tb.get()) {
			toShow = Arrays.asList(KEY_PLATFORM, KEY_HW, KEY_CAMKES_GROUP);
		}

		// show(showControls, container, JVM_controls);
		showOnly(container, toShow);

	}
}
