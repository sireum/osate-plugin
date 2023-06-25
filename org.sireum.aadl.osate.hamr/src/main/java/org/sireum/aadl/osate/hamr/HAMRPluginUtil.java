//HAMRPluginUtil.java

// get Eclipse plugins to HAMR which have connected to entry points:
// HamrBehaviorProviderPlugin, HamrEntrypointProviderPlugin, and HamrDatatypeProviderPlugin

package org.sireum.aadl.osate.hamr;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osate.aadl2.instance.SystemInstance;
import org.sireum.aadl.osate.architecture.BlessBehaviorProvider;
import org.sireum.aadl.osate.architecture.BlessEntrypointProvider;
import org.sireum.hamr.codegen.common.plugin.Plugin;

public class HAMRPluginUtil
  {

  static String BEHAVIOR_PROVIDER_ID = "org.sireum.aadl.osate.extensions.HamrBehaviorProvider";
  static String ENTRYPOINT_PROVIDER_ID = "org.sireum.aadl.osate.extensions.HamrEntrypointProvider";
  static String DATATYPE_PROVIDER_ID = "org.sireum.aadl.osate.extensions.HamrDatatypeProvider";
  static String OPTIONS_PROVIDER_ID = "org.sireum.aadl.osate.extensions.HamrOptionsProvider";
  
  public static List<org.sireum.hamr.codegen.common.plugin.Plugin> getHamrPlugins(SystemInstance si)
    {
    List<org.sireum.hamr.codegen.common.plugin.Plugin> plugins = new ArrayList<org.sireum.hamr.codegen.common.plugin.Plugin>();
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement[] behaviorElements = reg.getConfigurationElementsFor(BEHAVIOR_PROVIDER_ID);
    IConfigurationElement[] entrypointElements = reg.getConfigurationElementsFor(ENTRYPOINT_PROVIDER_ID);
    IConfigurationElement[] datatypeElements = reg.getConfigurationElementsFor(DATATYPE_PROVIDER_ID);
    try
      {
      for (IConfigurationElement be : behaviorElements)
        {
        org.sireum.hamr.codegen.common.plugin.Plugin b = (org.sireum.hamr.codegen.common.plugin.Plugin) be
            .createExecutableExtension("class");
// TODO uncomment when setSystemInstance(si) is added to Plugin
        if (b instanceof BlessBehaviorProvider)
          ((BlessBehaviorProvider)b).setSystemInstance(si);
        plugins.add(b);
        }
      for (IConfigurationElement ee : entrypointElements)
        {
        org.sireum.hamr.codegen.common.plugin.Plugin e = (org.sireum.hamr.codegen.common.plugin.Plugin) ee.createExecutableExtension("class");
// TODO uncomment when setSystemInstance(si) is added to Plugin
        if (e instanceof BlessEntrypointProvider)
          ((BlessEntrypointProvider)e).setSystemInstance(si);
        plugins.add(e);
        }
      for (IConfigurationElement de : datatypeElements)
        plugins.add((org.sireum.hamr.codegen.common.plugin.Plugin) de.createExecutableExtension("class"));
      }
    catch (CoreException e)
      {
      e.printStackTrace();
      }
    
    return plugins;
    }  //end of getHamrPlugins
  
  public static List<org.sireum.String> getHamrExperimentalOptions()
    {
    List<org.sireum.String> options = new ArrayList<org.sireum.String>();
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement[] optionsElements = reg.getConfigurationElementsFor(OPTIONS_PROVIDER_ID);
  
    return options;
    }  //end of getHamrExperimentalOptions
  
  }  //end of HAMRPluginUtil
