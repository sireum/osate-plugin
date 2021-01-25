package org.sireum.aadl.osate;

import org.eclipse.jface.preference.IPreferenceStore;
import org.sireum.aadl.osate.util.Util.SerializerType;

public class PreferenceValues {

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
}

