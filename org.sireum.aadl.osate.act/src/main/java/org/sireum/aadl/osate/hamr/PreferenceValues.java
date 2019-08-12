package org.sireum.aadl.osate.hamr;

import org.eclipse.jface.preference.IPreferenceStore;
import org.sireum.aadl.osate.act.Activator;

public class PreferenceValues {

	public static final String PLUGIN_ID = "HAMR";

	public static final String HAMR_SERIALIZE_OPT = "org.sireum.HAMR_SERIALIZE_OPT";

	public static final String HAMR_OUTPUT_FOLDER_OPT = "org.sireum.HAMR_OUTPUT_FOLDER_OPT";

	public enum Generators {
		GEN_CAMKES
	}

	public static boolean getHAMR_SERIALIZE_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getBoolean(HAMR_SERIALIZE_OPT);
	}

	public static String getHAMR_OUTPUT_FOLDER_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getString(HAMR_OUTPUT_FOLDER_OPT);
	}
}

