//HAMRPluginUtil.java

// get Eclipse plugins to HAMR which have connected to entry points:
// HamrBehaviorProviderPlugin, HamrEntrypointProviderPlugin, HamrDatatypeProviderPlugin, and HamrPlatformProviderPlugin

package org.sireum.aadl.osate.hamr;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osate.aadl2.instance.SystemInstance;
import org.sireum.hamr.codegen.common.plugin.Plugin;

import com.multitude.bless.bless2hamr.BlessBehaviorProvider;
import com.multitude.bless.bless2hamr.BlessEntrypointProvider;

public class HAMRPluginUtil
  {

  static String BEHAVIOR_PROVIDER_ID = "org.sireum.aadl.osate.extensions.HamrBehaviorProvider";
  static String ENTRYPOINT_PROVIDER_ID = "org.sireum.aadl.osate.extensions.HamrEntrypointProvider";
  static String DATATYPE_PROVIDER_ID = "org.sireum.aadl.osate.extensions.HamrDatatypeProvider";
  static String PLATFORM_PROVIDER_ID = "org.sireum.aadl.osate.extensions.HamrPlatformProvider";
  
  public static List<org.sireum.hamr.codegen.common.plugin.Plugin> getHamrPlugins(SystemInstance si)
    {
    List<org.sireum.hamr.codegen.common.plugin.Plugin> plugins = new ArrayList<org.sireum.hamr.codegen.common.plugin.Plugin>();
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement[] behaviorElements = reg.getConfigurationElementsFor(BEHAVIOR_PROVIDER_ID);
    IConfigurationElement[] entrypointElements = reg.getConfigurationElementsFor(ENTRYPOINT_PROVIDER_ID);
    IConfigurationElement[] datatypeElements = reg.getConfigurationElementsFor(DATATYPE_PROVIDER_ID);
    IConfigurationElement[] platformElements = reg.getConfigurationElementsFor(PLATFORM_PROVIDER_ID);
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
      for (IConfigurationElement pe : platformElements)
        plugins.add((org.sireum.hamr.codegen.common.plugin.Plugin) pe.createExecutableExtension("class"));
      }
    catch (CoreException e)
      {
      e.printStackTrace();
      }
    
    return plugins;
    }  //end of getHamrPlugins
  
//  public static List<org.sireum.String> getHamrExperimentalOptions()
//    {
//    List<org.sireum.String> options = new ArrayList<org.sireum.String>();
//    IExtensionRegistry reg = Platform.getExtensionRegistry();
//    IConfigurationElement[] optionsElements = reg.getConfigurationElementsFor(OPTIONS_PROVIDER_ID);
//    for (IConfigurationElement op : optionsElements)
//      {
//      org.sireum.aadl.osate.extensions.HamrOptionsProvider hop;
//      try
//        {
//        hop = (org.sireum.aadl.osate.extensions.HamrOptionsProvider) op.createExecutableExtension("class");
//        options.addAll(hop.getOptions());
//        }
//      catch (CoreException e)
//        {
//        e.printStackTrace();
//        }
//      }
//    return options;
//    }  //end of getHamrExperimentalOptions
  
  }  //end of HAMRPluginUtil
