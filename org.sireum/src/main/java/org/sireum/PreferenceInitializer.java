package org.sireum;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.sireum.PreferenceValues.SerializerType;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(PreferenceConstants.SERIALIZER, SerializerType.MSG_PACK.toString());

		if(Platform.getOS().equals(Platform.OS_WIN32)) {
			store.setDefault(PreferenceConstants.SIREUM_JAR_PATH, "C:\\Sireum\\sireum.jar");
		} else {
			store.setDefault(PreferenceConstants.SIREUM_JAR_PATH, "/usr/bin/sireum.jar");
		}
	}

}