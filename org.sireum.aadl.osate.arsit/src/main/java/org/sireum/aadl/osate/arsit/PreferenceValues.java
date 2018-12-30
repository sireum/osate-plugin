package org.sireum.aadl.osate.arsit;

import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceValues {

	public static final String PLUGIN_ID = "Arsit";

	public static final String ARSIT_SERIALIZE_OPT = "org.sireum.ARSIT_SERIALIZE_OPT";

	public static final String ARSIT_OUTPUT_FOLDER_OPT = "org.sireum.ARSIT_OUTPUT_FOLDER_OPT";


	public enum Generators {
		GEN_ARSIT
	}

	public static boolean getARSIT_SERIALIZE_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getBoolean(PreferenceValues.ARSIT_SERIALIZE_OPT);
	}

	public static String getARSIT_OUTPUT_FOLDER_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getString(PreferenceValues.ARSIT_OUTPUT_FOLDER_OPT);
	}
}

