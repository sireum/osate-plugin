package org.sireum.aadl.osate;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.sireum.aadl.osate.util.Util.SerializerType;

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

		BooleanFieldEditor ba = new BooleanFieldEditor(PreferenceConstants.PROCESS_BA_OPT, "Process BA", airTab);
		ba.getDescriptionControl(airTab).setToolTipText("Process BA/Bless annexes");
		addField(ba);

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
