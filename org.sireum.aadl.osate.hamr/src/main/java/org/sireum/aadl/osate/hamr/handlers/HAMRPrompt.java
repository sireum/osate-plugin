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
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.HW;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.Platform;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.Ros2LaunchLanguage;
import org.sireum.aadl.osate.hamr.handlers.HAMRPropertyProvider.Ros2NodesLanguage;

public class HAMRPrompt extends TitleAreaDialog {

	public final HAMROption OPTION_PLATFORM = new HAMROption("platform", "Platform", "Target platform");

	public final HAMROption OPTION_SLANG_OUTPUT_DIRECTORY = new HAMROption("slang.output.directory", "Output Directory",
			"Directory where Slang resources will be written to");
	public final HAMROption OPTION_BASE_PACKAGE_NAME = new HAMROption("base.package.name", "Base Package Name",
			"The root package name for the generated Slang project");
	public final HAMROption OPTION_ENABLE_RUNTIME_MONITORING = new HAMROption("enable.runtime.monitoring",
			"Enable Runtime Monitoring", "Enables runtime monitoring");

	public final HAMROption OPTION_HW = new HAMROption("hw", "HW", "");

	public final HAMROption OPTION_EXCLUDE_SLANG_IMPL = new HAMROption("exclude.slang.impl",
			"Exclude Slang Component Implementations",
			"Allows component behavior code to be provided in C rather than Slang");
	public final HAMROption OPTION_BIT_WIDTH = new HAMROption("bit.width", "Bit Width",
			"Bit size to use for unbounded numeric values (e.g. Base_Types::Integer)");
	public final HAMROption OPTION_MAX_SEQUENCE_SIZE = new HAMROption("max.sequence.size", "Max Sequence Size",
			"Maximum size of Slang sequences");
	public final HAMROption OPTION_MAX_STRING_SIZE = new HAMROption("max.string.size", "Max String Size",
			"Max size of Slang strings");
	public final HAMROption OPTION_C_OUTPUT_DIRECTORY = new HAMROption("c.output.directory", "C Output Directory",
			"Directory where C resources will be written to");
	public final HAMROption OPTION_C_AUX_SRC_DIRECTORY = new HAMROption("c.aux.src.directory", "Aux Code Directory",
			"Directory containing C code to be included in HAMR project");

	public final HAMROption OPTION_CAMKES_OUTPUT_DIRECTORY = new HAMROption("camkes.output.directory",
			"seL4/CAmkES Output Directory", "Directory where CAmkES resources will be written to");
	public final HAMROption OPTION_CAMKES_AUX_SRC_DIR = new HAMROption("camkes.aux.src.dir",
			"Aux Code Directory for CAmkES", "Directory containing C code to be included in CAmkES project");

	
	public final HAMROption OPTION_ROS2_STRICT_AADL_MODE = new HAMROption("ros2.strict.aadl.mode",
			"Strict AADL Mode", "Whether to generate strictly AADL-compliant code or not");
	public final HAMROption OPTION_ROS2_OUTPUT_WORKSPACE_DIRECTORY = new HAMROption("ros2.output.workspace.directory",
			"Output Workspace Directory", "The path to the ROS2 workspace to generate the packages intoDirectory where CAmkES resources will be written to");
	public final HAMROption OPTION_ROS2_DIRECTORY = new HAMROption("ros2.directory",
			"ROS2 Directory", "The path to your ROS2 installation, including the version");
	public final HAMROption OPTION_ROS2_NODES_LANGUAGE = new HAMROption("ros2.nodes.language", "Nodes Language", "The programming language for the generated node files");
	public final HAMROption OPTION_ROS2_LAUNCH_LANGUAGE = new HAMROption("ros2.launch.language", "Launch Language", "The programming language for the launch file");
	
	public Platform getOptionPlatform() {
		return Platform.valueOf(getSavedStringOption(OPTION_PLATFORM));
	}

	public HW getOptionHW() {
		return HW.valueOf(getSavedStringOption(OPTION_HW));
	}

	public String getOptionCOutputDirectory() {
		return getSavedStringOption(OPTION_C_OUTPUT_DIRECTORY);
	}

	public String getOptionCAuxSourceDirectory() {
		return getSavedStringOption(OPTION_C_AUX_SRC_DIRECTORY);
	}

	public String getSlangOptionOutputDirectory() {
		return getSavedStringOption(OPTION_SLANG_OUTPUT_DIRECTORY);
	}

	public boolean getOptionExcludesSlangImplementations() {
		return getSavedBooleanOption(OPTION_EXCLUDE_SLANG_IMPL);
	}

	public String getOptionBasePackageName() {
		return getSavedStringOption(OPTION_BASE_PACKAGE_NAME);
	}

	public boolean getOptionEnableRuntimeMonitoring() {
		return getSavedBooleanOption(OPTION_ENABLE_RUNTIME_MONITORING);
	}

	public int getOptionMaxSequenceSize() {
		return Integer.valueOf(getSavedStringOption(OPTION_MAX_SEQUENCE_SIZE));
	}

	public int getOptionMaxStringSize() {
		return Integer.valueOf(getSavedStringOption(OPTION_MAX_STRING_SIZE));
	}

	public int getOptionBitWidth() {
		return Integer.valueOf(getSavedStringOption(OPTION_BIT_WIDTH));
	}

	public String getOptionCamkesOptionOutputDirectory() {
		return getSavedStringOption(OPTION_CAMKES_OUTPUT_DIRECTORY);
	}

	public String getOptionCamkesAuxSrcDir() {
		return getSavedStringOption(OPTION_CAMKES_AUX_SRC_DIR);
	}
	
	public boolean getOptionRos2StrictAadlMode() {
		return getSavedBooleanOption(OPTION_ROS2_STRICT_AADL_MODE);
	}
	
	public String getOptionRos2OutputWorkspaceDirectory() {
		return getSavedStringOption(OPTION_ROS2_OUTPUT_WORKSPACE_DIRECTORY);
	}
	
	public String getOptionRos2Directory() {
		return getSavedStringOption(OPTION_ROS2_DIRECTORY);
	}
	
	public Ros2NodesLanguage getOptionRos2NodesLanguage() {
		return Ros2NodesLanguage.valueOf(getSavedStringOption(OPTION_ROS2_NODES_LANGUAGE));
	}
	
	public Ros2LaunchLanguage getOptionRos2LaunchLanguage() {
		return Ros2LaunchLanguage.valueOf(getSavedStringOption(OPTION_ROS2_LAUNCH_LANGUAGE));
	}
	

	/* runs after controls have been created */
	private void initControlValues() {
		// get previous values from eclipse project
		for (Entry<HAMROption, Control> option : optionControls.entrySet()) {
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
		{
			String slangOutputDirectory = getSlangOptionOutputDirectory();
			if (slangOutputDirectory.equals("")) {
				slangOutputDirectory = project.getLocation().toString();
			}
			getTextControl(OPTION_SLANG_OUTPUT_DIRECTORY).setText(slangOutputDirectory);
		}

		if (getIntFromControl(OPTION_BIT_WIDTH).isEmpty()) {
			getComboControl(OPTION_BIT_WIDTH).select(HAMRPropertyProvider.bitWidths.indexOf(theDefaultBitWidth));
		}

		if (getIntFromControl(OPTION_MAX_SEQUENCE_SIZE).isEmpty()) {
			getTextControl(OPTION_MAX_SEQUENCE_SIZE).setText(Integer.toString(theDefaultMaxSequenceSize));
		}

		if (getIntFromControl(OPTION_MAX_STRING_SIZE).isEmpty()) {
			getTextControl(OPTION_MAX_STRING_SIZE).setText(Integer.toString(theDefaultMaxStringSize));
		}
	}

	private boolean validate() {

		if (getComboControl(OPTION_PLATFORM).getSelectionIndex() < 0) {
			Dialog.showError("Input Error", "Please select a platform");
			return false;
		}

		Option<Integer> mss = getIntFromControl(OPTION_MAX_SEQUENCE_SIZE);
		if (mss.isEmpty() || mss.get().intValue() < 0) {
			Dialog.showError("Input Error", "Max Sequence Size must be a number greater than or equal to 0");
			return false;
		}

		Option<Integer> mstring = getIntFromControl(OPTION_MAX_STRING_SIZE);
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

	private final HAMRGroup GROUP_TRANSPILER = new HAMRGroup("KEY_GROUP_TRANSPILER", "Transpiler Options", "");
	private final HAMRGroup GROUP_CAMKES = new HAMRGroup("KEY_GROUP_CAMKES", "CAmkES Options", "");
	private final HAMRGroup GROUP_ROS2 = new HAMRGroup("KEY_GROUP_ROS2", "ROS2 Options", "");
	
	// The image to display
	private Image image;

	private List<Platform> thePlatforms = null;
	private List<HW> theHardwares = null;
	private int theDefaultBitWidth = -1;
	private int theDefaultMaxSequenceSize = -1;
	private int theDefaultMaxStringSize = -1;

	public HAMRPrompt(Shell parentShell) {
		super(parentShell);
	}

	public HAMRPrompt(IProject p, Shell parentShell, String title, List<Platform> platforms, List<HW> hardwares,
			int defaultBitWidth, int defaultMaxSequenceSize, int defaultMaxStringSize) {
		super(parentShell);
		subTitle = title;
		project = p;

		thePlatforms = platforms;
		theHardwares = hardwares;
		theDefaultBitWidth = defaultBitWidth;
		theDefaultMaxSequenceSize = defaultMaxSequenceSize;
		theDefaultMaxStringSize = defaultMaxStringSize;

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
			final HAMROption key = OPTION_PLATFORM;

			// COL 1
			addLabel(key.displayText, container, key).setToolTipText(key.toolTipText);

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
		/*
		 * {
		 * final HAMROption key = OPTION_HW;
		 *
		 * // COL 1
		 * addLabel(key.displayText, container, key);
		 *
		 * // COL 2
		 * Combo cmb = new Combo(container, SWT.READ_ONLY);
		 * GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		 * gd.widthHint = 100;
		 * cmb.setLayoutData(gd);
		 * cmb.setItems(getHWs());
		 * registerOptionControl(key, cmb);
		 *
		 * // COL 3
		 * registerViewControl(key, addColumnPad(container)); // col padding
		 * }
		 */

		/****************************************************************
		 * ROW - Slang Output Directory
		 ****************************************************************/

		{
			final HAMROption key = OPTION_SLANG_OUTPUT_DIRECTORY;

			// COL 1
			addLabel(key.displayText, container, key).setToolTipText(key.toolTipText);

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
			final HAMROption key = OPTION_BASE_PACKAGE_NAME;

			// COL 1
			addLabel(key.displayText, container, key).setToolTipText(key.toolTipText);

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
			final HAMROption key = OPTION_ENABLE_RUNTIME_MONITORING;

			// COL 1
			Button btn = new Button(container, SWT.CHECK);
			btn.setText(key.displayText);
			btn.setToolTipText(key.toolTipText);
			btn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, numCols, 1));
			registerOptionControl(key, btn, false);

			// COL 2
			registerViewControl(key, addColumnPad(container)); // col padding
		}

		fillerRow(container, numCols);

		/****************************************************************
		 * ROW
		 ****************************************************************/
		{
			final HAMRGroup key = GROUP_TRANSPILER;
			final int numGroupCols = 3;

			// COL 1
			Group grpContainer = new Group(container, SWT.NONE);
			grpContainer.setText(key.displayText);
			grpContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, numCols, 1));
			registerViewControl(key, grpContainer);

			final GridLayout grpLayout = new GridLayout(numGroupCols, false);
			grpContainer.setLayout(grpLayout);

			/****************************************************************
			 * GROUP ROW - Exclude Slang Impl
			 ****************************************************************/
			{
				HAMROption subKey = OPTION_EXCLUDE_SLANG_IMPL;

				Button btn = new Button(grpContainer, SWT.CHECK);
				btn.setText(subKey.displayText);
				btn.setToolTipText(subKey.toolTipText);
				btn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, numGroupCols, 1));
				registerOptionControl(subKey, btn, false);
			}

			/****************************************************************
			 * GROUP ROW - Bit Width
			 ****************************************************************/
			{
				HAMROption subKey = OPTION_BIT_WIDTH;

				// COL 1
				addLabel(subKey.displayText, grpContainer).setToolTipText(subKey.toolTipText);

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
				HAMROption subKey = OPTION_MAX_SEQUENCE_SIZE;

				// COL 1
				addLabel(subKey.displayText, grpContainer).setToolTipText(subKey.toolTipText);

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
				HAMROption subKey = OPTION_MAX_STRING_SIZE;

				// COL 1
				addLabel(subKey.displayText, grpContainer).setToolTipText(subKey.toolTipText);

				// COL 2
				Text txt = new Text(grpContainer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				registerOptionControl(subKey, txt, false);

				// COL 3
				addColumnPad(grpContainer);
			}

			{
				HAMROption subKey = OPTION_C_OUTPUT_DIRECTORY;

				// COL 1
				addLabel(subKey.displayText, grpContainer).setToolTipText(subKey.toolTipText);

				// COL 2
				Text txt = new Text(grpContainer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				registerOptionControl(subKey, txt, false);

				// COL 3
				Button btn = new Button(grpContainer, SWT.NONE);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String path = promptForDirectory("Select C Output Directory", getOptionCOutputDirectory());
						if (path != null) {
							txt.setText(path);
						}
					}
				});
				btn.setText("...");
				btn.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				// registerViewControl(subKey, btn);
			}

			/****************************************************************
			 * GROUP ROW - C AUX Source Code Directory
			 ****************************************************************/
			{
				HAMROption subKey = OPTION_C_AUX_SRC_DIRECTORY;

				// COL 1
				addLabel(subKey.displayText, grpContainer).setToolTipText(subKey.toolTipText);

				// COL 2
				Text txt = new Text(grpContainer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				registerOptionControl(subKey, txt, false);

				// COL 3
				Button btn = new Button(grpContainer, SWT.NONE);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String path = promptForDirectory("Select C Source Directory", getOptionCAuxSourceDirectory());
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
			final HAMRGroup key = GROUP_CAMKES;
			final int numGroupCols = 3;

			// COL 1
			Group grpContainer = new Group(container, SWT.NONE);
			grpContainer.setText(key.displayText);
			grpContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, numCols, 1));
			registerViewControl(key, grpContainer);

			final GridLayout grpLayout = new GridLayout(numGroupCols, false);
			grpContainer.setLayout(grpLayout);

			/****************************************************************
			 * ROW - Camkes Output Directory
			 ****************************************************************/

			{
				final HAMROption subKey = OPTION_CAMKES_OUTPUT_DIRECTORY;

				// COL 1
				addLabel(subKey.displayText, grpContainer).setToolTipText(subKey.toolTipText);

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
				final HAMROption subKey = OPTION_CAMKES_AUX_SRC_DIR;

				// COL 1
				addLabel(subKey.displayText, grpContainer).setToolTipText(subKey.toolTipText);

				// COL 2
				Text txt = new Text(grpContainer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				registerOptionControl(subKey, txt, false);

				// COL 3
				Button btn = new Button(grpContainer, SWT.NONE);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String path = promptForDirectory("Select Aux Source Directory Directory",
								getOptionCamkesAuxSrcDir());
						if (path != null) {
							txt.setText(path);
						}
					}
				});
				btn.setText("...");
				btn.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
			}
		}
		
		fillerRow(container, numCols);
		
		/****************************************************************
		 * ROW
		 ****************************************************************/
		{
			final HAMRGroup key = GROUP_ROS2;
			final int numGroupCols = 3;

			// COL 1
			Group grpContainer = new Group(container, SWT.NONE);
			grpContainer.setText(key.displayText);
			grpContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, numCols, 1));
			registerViewControl(key, grpContainer);

			final GridLayout grpLayout = new GridLayout(numGroupCols, false);
			grpContainer.setLayout(grpLayout);

			/****************************************************************
			 * GROUP ROW - ROS2 Strict AADL Mode
			 ****************************************************************/
			{
				HAMROption subKey = OPTION_ROS2_STRICT_AADL_MODE;

				Button btn = new Button(grpContainer, SWT.CHECK);
				btn.setText(subKey.displayText);
				btn.setToolTipText(subKey.toolTipText);
				btn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, numGroupCols, 1));
				registerOptionControl(subKey, btn, false);
			}

			/****************************************************************
			 * ROW - ROS2 Output Workspace Directory
			 ****************************************************************/

			{
				final HAMROption subKey = OPTION_ROS2_OUTPUT_WORKSPACE_DIRECTORY;

				// COL 1
				addLabel(subKey.displayText, grpContainer).setToolTipText(subKey.toolTipText);

				// COL 2
				Text txt = new Text(grpContainer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				registerOptionControl(subKey, txt, false);

				// COL 3
				Button btn = new Button(grpContainer, SWT.NONE);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String path = promptForDirectory("Select ROS2 Ouptut Workspace Directory",
								getOptionCamkesAuxSrcDir());
						if (path != null) {
							txt.setText(path);
						}
					}
				});
				btn.setText("...");
				btn.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
			}
			/****************************************************************
			 * ROW - ROS2 Directory
			 ****************************************************************/
			{
				final HAMROption subKey = OPTION_ROS2_DIRECTORY;

				// COL 1
				addLabel(subKey.displayText, grpContainer).setToolTipText(subKey.toolTipText);

				// COL 2
				Text txt = new Text(grpContainer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				registerOptionControl(subKey, txt, false);

				// COL 3
				Button btn = new Button(grpContainer, SWT.NONE);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String path = promptForDirectory("Select ROS2 Directory",
								getOptionCamkesAuxSrcDir());
						if (path != null) {
							txt.setText(path);
						}
					}
				});
				btn.setText("...");
				btn.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
			}
			
			/****************************************************************
			 * ROW - ROS2 Nodes Language
			 ****************************************************************/
			{
				final HAMROption subKey = OPTION_ROS2_NODES_LANGUAGE;

				// COL 1
				addLabel(subKey.displayText, grpContainer).setToolTipText(key.toolTipText);

				// COL 2
				Combo cmb = new Combo(grpContainer, SWT.READ_ONLY);
				cmb.setItems(getRos2NodesLanguages());
				GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
				gd.widthHint = 100;
				cmb.setLayoutData(gd);
				registerOptionControl(subKey, cmb, false);

				//cmb.addSelectionListener(new SelectionAdapter() {
				//	@Override
				//	public void widgetSelected(SelectionEvent e) {
				//		computeVisibilityAndPack(grpContainer);
				//	}
				//});

				// COL 3
				//registerViewControl(key, addColumnPad(grpContainer)); // col padding
				addColumnPad(grpContainer);
			}
			/****************************************************************
			 * ROW - ROS2 Launch Language
			 ****************************************************************/
			{
				final HAMROption subKey = OPTION_ROS2_LAUNCH_LANGUAGE;

				// COL 1
				addLabel(subKey.displayText, grpContainer).setToolTipText(key.toolTipText);

				// COL 2
				Combo cmb = new Combo(grpContainer, SWT.READ_ONLY);
				cmb.setItems(getRos2LaunchLanguages());
				GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
				gd.widthHint = 100;
				cmb.setLayoutData(gd);
				registerOptionControl(subKey, cmb, false);

				//cmb.addSelectionListener(new SelectionAdapter() {
				//	@Override
				//	public void widgetSelected(SelectionEvent e) {
				//		computeVisibilityAndPack(grpContainer);
				//	}
				//});

				// COL 3
				//registerViewControl(key, addColumnPad(grpContainer)); // col padding
				addColumnPad(grpContainer);
			}
		}

		initControlValues();

		computeVisibility(container);

		return area;
	}

	protected <T> List<T> addAll(List<T> controls, List<T> other) {
		List<T> ret = new ArrayList<>(controls);
		ret.addAll(other);
		return ret;
	}

	private Label addLabel(String label, Composite container) {
		Label l = new Label(container, SWT.NONE);
		l.setText(label);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		return l;
	}

	private Label addLabel(String label, Composite container, HAMREntry key) {
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

	private final Map<HAMREntry, Collection<Control>> viewControlMap = new HashMap<>();
	private final Map<HAMROption, Control> optionControls = new HashMap<>();

	private Combo getComboControl(HAMROption key) {
		if (optionControls.containsKey(key) && optionControls.get(key) instanceof Combo) {
			return (Combo) optionControls.get(key);
		} else {
			return null;
		}
	}

	private Text getTextControl(HAMROption key) {
		if (optionControls.containsKey(key) && optionControls.get(key) instanceof Text) {
			return (Text) optionControls.get(key);
		} else {
			return null;
		}
	}

	private Control registerOptionControl(HAMROption KEY, Control control) {
		return registerOptionControl(KEY, control, true);
	}

	private Control registerOptionControl(HAMROption KEY, Control control, boolean addToViewControl) {
		assert (!optionControls.containsKey(KEY));
		optionControls.put(KEY, control);
		if (addToViewControl) {
			registerViewControl(KEY, control);
		}
		return control;
	}

	private Control registerViewControl(HAMREntry KEY, Control control) {
		assert (control.getLayoutData() instanceof GridData);
		if (!viewControlMap.containsKey(KEY)) {
			viewControlMap.put(KEY, new ArrayList<>());
		}
		viewControlMap.get(KEY).add(control);
		return control;
	}

	protected void showOnly(Composite container, List<HAMREntry> keys) {
		for (Entry<HAMREntry, Collection<Control>> e : viewControlMap.entrySet()) {
			boolean makeVisible = keys.contains(e.getKey());
			for (Control c : e.getValue()) {
				GridData gd = (GridData) c.getLayoutData();
				gd.exclude = !makeVisible;
				c.setVisible(makeVisible);
			}
		}
	}

	private void show(boolean show, Composite container, HAMREntry... KEYS) {
		for (HAMREntry KEY : KEYS) {
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
		for (Entry<HAMROption, Control> option : optionControls.entrySet()) {
			if (option.getValue() instanceof Text) {
				projectNode.put(option.getKey().id, ((Text) option.getValue()).getText());
			} else if (option.getValue() instanceof Combo) {
				projectNode.put(option.getKey().id, ((Combo) option.getValue()).getText());
			} else if (option.getValue() instanceof Button) {
				projectNode.putBoolean(option.getKey().id, ((Button) option.getValue()).getSelection());
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

	private boolean getSavedBooleanOption(HAMROption key) {
		return projectNode.getBoolean(key.id, false);
	}

	private String getSavedStringOption(HAMROption key) {
		return projectNode.get(key.id, "");
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
	
	private String[] getRos2NodesLanguages() {
		return Arrays.asList(HAMRPropertyProvider.Ros2NodesLanguage.values()).stream().map(f -> f.toString()).toArray(String[]::new);
	}
	
	private String[] getRos2LaunchLanguages() {
		return Arrays.asList(HAMRPropertyProvider.Ros2LaunchLanguage.values()).stream().map(f -> f.toString()).toArray(String[]::new);
	}

	private String[] filterHW(HW... validOption) {
		return Arrays.asList(validOption)
				.stream()
				.distinct()
				.filter(theHardwares::contains)
				.collect(Collectors.toSet())
				.stream()
				.map(f -> f.toString())
				.toArray(String[]::new);
	}

	private Option<String> getStringFromControl(HAMROption key) {
		if (optionControls.containsKey(key)) {
			Control c = optionControls.get(key);
			if (c instanceof Text) {
				return org.sireum.Some$.MODULE$.apply(((Text) c).getText());
			} else if (c instanceof Combo) {
				return org.sireum.Some$.MODULE$.apply(((Combo) c).getText());
			} else if (c instanceof Button) {
				return org.sireum.Some$.MODULE$.apply(Boolean.valueOf(((Button) c).getSelection()).toString());
			}
		}
		return org.sireum.None.apply();
	}

	private Option<Integer> getIntFromControl(HAMROption key) {
		try {
			return org.sireum.Some$.MODULE$.apply(Integer.valueOf(getStringFromControl(key).get()));
		} catch (Exception nfe) {
		}
		return org.sireum.None.apply();
	}

	private Option<Boolean> getBooleanFromControl(HAMROption key) {
		try {
			return org.sireum.Some$.MODULE$.apply(Boolean.valueOf(getStringFromControl(key).get()));
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
		Combo cmb = getComboControl(OPTION_PLATFORM);

		if (cmb.getSelectionIndex() < 0) {
			showOnly(container, Arrays.asList(OPTION_PLATFORM));
			return;
		}

		String selection = cmb.getItem(cmb.getSelectionIndex());
		Platform p = HAMRPropertyProvider.Platform.valueOf(selection);

		List<HAMREntry> JVM_controls = Arrays.asList( //
				OPTION_PLATFORM, OPTION_SLANG_OUTPUT_DIRECTORY, OPTION_BASE_PACKAGE_NAME,
				OPTION_ENABLE_RUNTIME_MONITORING);

		List<HAMREntry> NIX_controls = addAll(JVM_controls, Arrays.asList( //
				// OPTION_HW,
				OPTION_C_OUTPUT_DIRECTORY, OPTION_C_AUX_SRC_DIRECTORY, //
				OPTION_EXCLUDE_SLANG_IMPL, GROUP_TRANSPILER));

		List<HAMREntry> SEL4_controls = addAll(NIX_controls, Arrays.asList( //
				GROUP_CAMKES));
		
		List<HAMREntry> toShow = JVM_controls;

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
		case seL4_Only:
		case seL4_TB:
			hwItems = filterHW(HW.QEMU, HW.ODROID_XU4);
			// toShow = Arrays.asList(OPTION_PLATFORM, OPTION_HW, GROUP_CAMKES);
			toShow = Arrays.asList(OPTION_PLATFORM, GROUP_CAMKES);
			break;
		case ros2:
			hwItems = filterHW();
			toShow = Arrays.asList(OPTION_PLATFORM, GROUP_ROS2);
			break;
			
		default:
			throw new RuntimeException("Not expecting platform " + p.name());
		}
		// getComboControl(OPTION_HW).setItems(hwItems);

		// show(showControls, container, JVM_controls);
		showOnly(container, toShow);

	}

	public abstract class HAMREntry {
		final String id;
		final String displayText;
		final String toolTipText;

		HAMREntry(String id, String displayText, String toolTipText) {
			this.id = id;
			this.displayText = displayText;
			this.toolTipText = toolTipText;
		}
	}

	public class HAMRGroup extends HAMREntry {
		HAMRGroup(String id, String displayText, String toolTipText) {
			super(id, displayText, toolTipText);
		}
	}

	public class HAMROption extends HAMREntry {
		HAMROption(String id, String displayText, String toolTipText) {
			super(id, displayText, toolTipText);
		}
	}
}
