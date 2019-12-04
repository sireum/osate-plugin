package org.sireum.aadl.osate.hamr;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(PreferenceValues.HAMR_DEVICES_AS_THREADS_OPT, false);

		store.setDefault(PreferenceValues.HAMR_EMBED_ART_OPT, true);

		store.setDefault(PreferenceValues.HAMR_OUTPUT_FOLDER_OPT, ".slang");

		store.setDefault(PreferenceValues.HAMR_SERIALIZE_OPT, true);

		store.setDefault(PreferenceValues.HAMR_VERBOSE_OPT, true);
	}
}
