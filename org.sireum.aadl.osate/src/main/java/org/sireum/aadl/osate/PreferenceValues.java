package org.sireum.aadl.osate;

import org.eclipse.jface.preference.IPreferenceStore;
import org.sireum.aadl.osate.util.Util.SerializerType;

public class PreferenceValues {

	public enum Generators {
		SERIALIZE, GEN_AWAS
	}

	public static SerializerType getSERIALIZATION_METHOD_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return SerializerType.valueOf(store.getString(PreferenceConstants.SERIALIZATION_METHOD_OPT));
	}
}

