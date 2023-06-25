//HamrOptionsProvider.java

//allows Eclipse plugins to HAMR osate-plugin to add "experimental" options of their own
//these are listed in org.sireum.hamr.codegen.common.util.ExperimentalOptions

//each option is a (Slang) string

//for now the only option used is a string like "ADD_PORT_IDS=X" 
//where X is the literal of the number of additional port ids used for timeout dispatch triggers.

package org.sireum.aadl.osate.extensions;

import java.util.ArrayList;
import java.util.List;

public abstract interface HamrOptionsProvider extends org.sireum.MSigTrait
  {
  
  void addOption(org.sireum.String o);

  List<org.sireum.String> getOptions();


  }
