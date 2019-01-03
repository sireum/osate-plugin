package org.sireum.aadl.osate.arsit

import java.io.File
import org.sireum.aadl.ir.Aadl
import org.sireum.aadl.osate.arsit.handlers.ArsitPrompt
import org.sireum.{F, ISZ, None, Option, Some, String}
import org.eclipse.ui.console.MessageConsole
import org.sireum.cli.Cli
import org.sireum.aadl.osate.util.ScalaUtil


object ArsitUtil {

  def launchArsit(prompt: ArsitPrompt, model: Aadl, ms: MessageConsole): Int = {
    ScalaUtil.callWrapper("Arsit", ms, () => {
      val outDir: File = new File(prompt.getOptionOutputDirectory())
  
      val ipcmech = prompt.getOptionIPCMechanism match {
        case "Message Queue" => Cli.Ipcmech.MessageQueue
        case "Shared Memory" => Cli.Ipcmech.SharedMemory
        case _ => Cli.Ipcmech.MessageQueue
      }
        
      val opts = Cli.ArsitOption(
        help = "",
        args = ISZ(),
        json = F, // irrelevant since passing the aadl model directly
        outputDir = Some(prompt.getOptionOutputDirectory),
        packageName = if(prompt.getOptionBasePackageName == "") None[String] else Some(prompt.getOptionBasePackageName()),
        noart = !prompt.getOptionEmbedArt,
        bless = prompt.getOptionGenerateBlessEntryPoints,
        genTrans = prompt.getOptionGenerateTranspilerArtifacts,
        ipc = ipcmech
      )
      
      org.sireum.aadl.arsit.Runner.run(outDir, model, opts)
    })
  }
}