package org.sireum.aadl.osate.hamr.handlers;

import java.util.List;

import org.osate.aadl2.instance.SystemInstance;
import org.sireum.IS;
import org.sireum.ST;
import org.sireum.Z;
import org.sireum.aadl.osate.util.SlangUtil;
import org.sireum.aadl.osate.util.VisitorUtil;
import org.sireum.hamr.arsit.Port;
import org.sireum.hamr.arsit.plugin.*;
import org.sireum.hamr.arsit.plugin.EntryPointContributions;
import org.sireum.hamr.arsit.templates.EntryPointTemplate;
import org.sireum.hamr.codegen.common.symbols.AadlThreadOrDevice;
import org.sireum.hamr.codegen.common.symbols.AnnexClauseInfo;
import org.sireum.hamr.codegen.common.symbols.SymbolTable;
import org.sireum.hamr.codegen.common.types.AadlTypes;
import org.sireum.hamr.codegen.common.util.NameUtil.NameProvider;
import org.sireum.hamr.ir.Annex;
import org.sireum.message.Reporter;

public class EP implements org.sireum.hamr.arsit.plugin.EntryPointProviderPlugin
  {
SystemInstance si;

  public EP(SystemInstance si) {
    this.si = si;
  }
  @Override
  public String name()
    {
    return "plugin name";
    }

  @Override
  public String string()
    {
    return this.toString();
    }

  @Override
  public boolean canHandle(AadlThreadOrDevice component, IS<Z, AnnexClauseInfo> resolvedAnnexSubclauses)
    {
      List<Annex> annexes = scala.collection.JavaConverters.seqAsJavaListConverter(component.annexes().elements()).asJava();
      for(Annex a : annexes) {
        if(a.name().equalsIgnoreCase("gumbo")) {
          return true;
        }
      }
    return false;
    }

  @Override
  public EntryPointContributions handle(AadlThreadOrDevice component, NameProvider nameProvider, IS<Z, Port> ports,
      EntryPointTemplate entryPointTemplate, SymbolTable symbolTable, AadlTypes aadlTypes, Reporter reporter)
    {

    
    IS<Z, org.sireum.String> imports = VisitorUtil.toISZ(new org.sireum.String("org.sireum.S64._"));
    IS<Z, org.sireum.String> blocks = VisitorUtil.toISZ(new org.sireum.String("// I'm a companion object block")
        //new org.sireum.String(si.getComponentInstancePath()),
        //new org.sireum.String(component.pathAsString("___"))
        );
    
    IS<Z, org.sireum.String> epblocks = VisitorUtil.toISZ(new org.sireum.String("val x = s64\"3\""));
    
    org.sireum.Option<org.sireum.String> none = SlangUtil.toNone();
    org.sireum.Option<org.sireum.String> computeBody = SlangUtil.toSome(new org.sireum.String("// i'm a compute body"));
    ST ep = entryPointTemplate.generateCustom(epblocks, none, none, none, computeBody, none, none, none, none);
    
    EntryPointContributions ret = EntryPointContributions$.MODULE$.apply(imports, blocks, ep, VisitorUtil.toISZ());
    
    return ret;
    }

  }
