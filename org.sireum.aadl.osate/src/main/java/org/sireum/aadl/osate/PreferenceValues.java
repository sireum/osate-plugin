package org.sireum.aadl.osate;

import org.eclipse.jface.preference.IPreferenceStore;
import org.sireum.aadl.osate.util.Util.SerializerType;

public class PreferenceValues {

	public static final String SIREUM_MARKER_ID = "org.sireum.plugin.marker";

	public static final String SIREUM_PLUGIN_ID = "Sireum";

	public enum Generators {
		SERIALIZE
	}

	public static SerializerType getSERIALIZATION_METHOD_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return SerializerType.valueOf(store.getString(PreferenceConstants.SERIALIZATION_METHOD_OPT));
	}

	public static boolean getPROCESS_BA_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getBoolean(PreferenceConstants.PROCESS_BA_OPT);
	}

	public static void setPROCESS_BA_OPT(boolean value) {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.PROCESS_BA_OPT, value);
	}
}
