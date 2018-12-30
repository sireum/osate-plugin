package org.sireum.aadl.osate;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.sireum.aadl.osate.util.Util.SerializerType;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(PreferenceConstants.SERIALIZATION_METHOD_OPT, SerializerType.MSG_PACK.toString());

		store.setDefault(PreferenceConstants.ARSIT_SERIALIZE_OPT, true);

		store.setDefault(PreferenceConstants.ARSIT_OUTPUT_FOLDER_OPT, ".slang");
	}

}
