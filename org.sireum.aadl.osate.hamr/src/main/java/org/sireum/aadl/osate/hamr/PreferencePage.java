package org.sireum.aadl.osate.hamr;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		// setDescription("Code generation configuration page");
	}

	@Override
	protected void createFieldEditors() {
		// TabFolder tabFolder = new TabFolder(getFieldEditorParent(), SWT.NONE);
		// tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Composite comp = addTab(tabFolder, "Code Generation");
		Composite comp = getFieldEditorParent();

		comp.setLayout(new GridLayout());

		addField(PreferenceValues.HAMR_VERBOSE_OPT.getEditor(comp));

		addField(PreferenceValues.HAMR_SERIALIZE_AIR_OPT.getEditor(comp));

		addField(PreferenceValues.HAMR_AIR_OUTPUT_FOLDER_OPT.getEditor(comp));


		// blank line
		new Label(comp, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));


		Group seGroup = new Group(comp, SWT.BORDER);
		seGroup.setText("Slang-Embedded Options");
		seGroup.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 3, 1));

		addField(PreferenceValues.HAMR_DEVICES_AS_THREADS_OPT.getEditor(seGroup));

		addField(PreferenceValues.HAMR_EMBED_ART_OPT.getEditor(seGroup));

		addField(PreferenceValues.HAMR_RUN_PROYEK_IVE_OPT.getEditor(seGroup));

		addField(PreferenceValues.HAMR_RUN_TRANSPILER_OPT.getEditor(seGroup));

		Group proofGroup = new Group(comp, SWT.BORDER);
		proofGroup.setText("Proof Options");
		proofGroup.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 3, 1));

		addField(PreferenceValues.HAMR_PROOF_GENERATE.getEditor(proofGroup));

		addField(PreferenceValues.HAMR_PROOF_CHECK.getEditor(proofGroup));

		addField(PreferenceValues.HAMR_SMT2_TIMEOUT.getEditor(proofGroup));

		addField(PreferenceValues.HAMR_SMT2_OPTIONS.getEditor(proofGroup));

		addField(PreferenceValues.HAMR_SMT2_PATH.getEditor(proofGroup));
	}

	private Composite addTab(TabFolder tabFolder, String tabName) {
		Composite newTab = new Composite(tabFolder, SWT.NULL);
		newTab.setLayout(new GridLayout());
		newTab.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		TabItem item = new TabItem(tabFolder, SWT.NONE);
		item.setText(tabName);
		item.setControl(newTab);

		return newTab;
	}

	@Override
	public void init(IWorkbench workbench) {

	}
}
