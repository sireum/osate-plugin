package org.sireum.aadl.osate;

import org.eclipse.jface.preference.IPreferenceStore;
import org.sireum.aadl.osate.util.Util.SerializerType;

public class PreferenceValues {

	public static final String SIREUM_MARKER_ID = "org.sireum.aadl.osate.marker";

	public static final String SIREUM_PLUGIN_ID = "Sireum";

	public enum Generators {
		SERIALIZE, MARKERS
	}

	public static SerializerType getSERIALIZATION_METHOD_OPT() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return SerializerType.valueOf(store.getString(PreferenceConstants.SERIALIZATION_METHOD_OPT));
	}
}
