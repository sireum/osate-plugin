package org.sireum.aadl.osate.arsit;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(PreferenceValues.ARSIT_SERIALIZE_OPT, true);

		store.setDefault(PreferenceValues.ARSIT_OUTPUT_FOLDER_OPT, ".slang");
	}
}
