package org.sireum.aadl.osate;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.sireum.aadl.osate.PreferenceValues.SerializerType;
import org.sireum.aadl.osate.util.Util.Tool;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Sireum configuration page");
	}

	@Override
	protected void createFieldEditors() {
		TabFolder tabFolder = new TabFolder(getFieldEditorParent(), SWT.NONE);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite airTab = addTab(tabFolder, "AIR");
		addField(new RadioGroupFieldEditor(PreferenceConstants.SERIALIZATION_METHOD_OPT,
				"Method to use when serializing AIR to a file:", 1, new String[][] { //
						{ "JSON", SerializerType.JSON.toString() }, //
						{ "JSON (compact)", SerializerType.JSON_COMPACT.toString() }, //
						{ "MsgPack", SerializerType.MSG_PACK.toString() } },
				airTab, true));

		if (Tool.ARSIT.exists()) {
			Composite arsitTab = addTab(tabFolder, "Arsit");
			addField(new BooleanFieldEditor(PreferenceConstants.ARSIT_SERIALIZE_OPT,
					"Serialize AIR to JSON (non-compact) when generating Slang-Embedded", arsitTab));

			addField(new StringFieldEditor(PreferenceConstants.ARSIT_OUTPUT_FOLDER_OPT, "Output folder", arsitTab));
		}

		if (Tool.ACT.exists()) {
			Composite actTab = addTab(tabFolder, "ACT");
			addField(new BooleanFieldEditor(PreferenceConstants.ACT_SERIALIZE_OPT,
					"Serialize AIR to JSON (non-compact) when generating CAmkES", actTab));

			addField(new StringFieldEditor(PreferenceConstants.ACT_OUTPUT_FOLDER_OPT, "Output folder", actTab));
		}

		if (Tool.AWAS.exists()) {
			Composite awasTab = addTab(tabFolder, "Awas");
		}
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
