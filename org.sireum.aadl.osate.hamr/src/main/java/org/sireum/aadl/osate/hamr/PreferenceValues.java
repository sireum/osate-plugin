package org.sireum.aadl.osate.hamr;

import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceValues {

	public static final String PLUGIN_ID = "HAMR";

	public static final String HAMR_DEVICES_AS_THREADS_OPT = "HAMR_DEVICES_AS_THREADS_OPT";
	public static final String HAMR_EMBED_ART_OPT = "HAMR_EMBED_ART_OPT";
	public static final String HAMR_OUTPUT_FOLDER_OPT = "org.sireum.HAMR_OUTPUT_FOLDER_OPT";
	public static final String HAMR_SERIALIZE_OPT = "org.sireum.HAMR_SERIALIZE_OPT";
	public static final String HAMR_VERBOSE_OPT = "org.sireum.HAMR_VERBOSE_OPT";

	public enum Generators {
		GEN_CAMKES
	}

	public static boolean getHAMR_DEVICES_AS_THREADS_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getBoolean(HAMR_DEVICES_AS_THREADS_OPT);
	}

	public static boolean getHAMR_EMBED_ART_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getBoolean(HAMR_EMBED_ART_OPT);
	}

	public static String getHAMR_OUTPUT_FOLDER_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getString(HAMR_OUTPUT_FOLDER_OPT);
	}

	public static boolean getHAMR_SERIALIZE_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getBoolean(HAMR_SERIALIZE_OPT);
	}

	public static boolean getHAMR_VERBOSE_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getBoolean(HAMR_VERBOSE_OPT);
	}
}

