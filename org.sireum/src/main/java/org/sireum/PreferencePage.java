package org.sireum;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.sireum.PreferenceValues.SerializerType;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(
				"Sireum configuration page.\n\nNote you should first install Sireum. See documentation for more details");
	}

	@Override
	protected void createFieldEditors() {
		// addField(new FileFieldEditor(PreferenceConstants.SIREUM_JAR_PATH, "&Path to sireum.jar:", true,
		// getFieldEditorParent()));

		addField(new RadioGroupFieldEditor(PreferenceConstants.SERIALIZER, "Serialization method:", 1,
				new java.lang.String[][] { //
						{ "JSON", SerializerType.JSON.toString() }, //
						{ "JSON (compact)", SerializerType.JSON_COMPACT.toString() }, //
						{ "MsgPack", SerializerType.MSG_PACK.toString() } },
				getFieldEditorParent(), true));
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
