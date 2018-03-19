package org.sireum.aadl.osate;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {
	public static final String PLUGIN_ID = PreferenceConstants.PLUGIN_ID;

	// The shared instance
	private static Activator plugin;

	private IPreferenceStore preferenceStore = new ScopedPreferenceStore(ConfigurationScope.INSTANCE,
			Activator.PLUGIN_ID);

	public Activator() {

	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// Call the preference initializer manually because eclipse will not call it if the preference page uses a scoped preference store
		new PreferenceInitializer().initializeDefaultPreferences();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	@Override
	public IPreferenceStore getPreferenceStore() {
		return preferenceStore;
	}

	/**
	 * Returns the shared instance
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
}
