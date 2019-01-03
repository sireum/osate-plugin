package org.sireum.aadl.osate.act

import java.io.File
import org.sireum.aadl.ir.Aadl
import org.sireum.{ISZ, Option, Some, String}
import org.eclipse.ui.console.MessageConsole
import org.sireum.aadl.osate.act.handlers.ActPrompt
import org.sireum.aadl.osate.util.ScalaUtil

object ActUtil {  
  def launchAct(prompt: ActPrompt, model: Aadl, ms: MessageConsole, workspaceRoot: File): Int = {

    ScalaUtil.callWrapper("ACT", ms, () => {
      val outDir: File = new File(prompt.getOptionOutputDirectory());
      val auxDirs : ISZ[String] = if(prompt.getOptionCSourceDirectory != "") ISZ(prompt.getOptionCSourceDirectory()) else ISZ()
      val aadlRootDir: Option[File] = Some(workspaceRoot)
      
      org.sireum.aadl.act.Act.run(outDir, model, auxDirs, aadlRootDir)
    })
  }
}
