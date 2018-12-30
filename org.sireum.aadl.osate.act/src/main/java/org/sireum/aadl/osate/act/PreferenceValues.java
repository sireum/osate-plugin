package org.sireum.aadl.osate.act;

import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceValues {

	public static final String PLUGIN_ID = "Act";

	public static final String ACT_SERIALIZE_OPT = "org.sireum.ACT_SERIALIZE_OPT";

	public static final String ACT_OUTPUT_FOLDER_OPT = "org.sireum.ACT_OUTPUT_FOLDER_OPT";

	public enum Generators {
		GEN_CAMKES
	}

	public static boolean getACT_SERIALIZE_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getBoolean(ACT_SERIALIZE_OPT);
	}

	public static String getACT_OUTPUT_FOLDER_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getString(ACT_OUTPUT_FOLDER_OPT);
	}
}

