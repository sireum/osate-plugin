package org.sireum.aadl.osate.hamr.plugin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osate.aadl2.instance.SystemInstance;

public class HAMRPluginUtil {

	public static String PLUGIN_PROVIDER_ID = "org.sireum.aadl.osate.hamr.HamrCodegenPluginProvider";

	public static List<org.sireum.hamr.codegen.common.plugin.Plugin> getHamrPlugins(SystemInstance si) {
		List<org.sireum.hamr.codegen.common.plugin.Plugin> plugins = new ArrayList<>();
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IConfigurationElement[] pluginProviders = reg.getConfigurationElementsFor(PLUGIN_PROVIDER_ID);
		try {
			for (IConfigurationElement p : pluginProviders) {
				org.sireum.hamr.codegen.common.plugin.Plugin b = (org.sireum.hamr.codegen.common.plugin.Plugin) p
						.createExecutableExtension("class");
				if (b instanceof OSATECodegenPlugin) {
					((OSATECodegenPlugin) b).setSystemInstance(si);
				}
				plugins.add(b);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return plugins;
	}

}