package org.sireum;

import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceValues {

	public enum SerializerType {
		JSON, JSON_COMPACT, MSG_PACK
	}

	public static SerializerType getSERIALIZER() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return SerializerType.valueOf(store.getString(PreferenceConstants.SERIALIZER));
	}
}

