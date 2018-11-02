package org.sireum.aadl.osate.util

import java.io.File
import org.sireum.aadl.ir.Aadl
import org.sireum.aadl.osate.MenuContributions
import org.sireum.aadl.osate.util.Util.Tool
import org.sireum.{F, T, ISZ, None, Option, Some, String, Z}

object ArsitUtil {

  // Separating this out as it refers to the Sireum v3 artifact Cli.ArsitOption which may not be 
  // present in other builds (e.g. ACT)
  def launchArsit(prompt: ArsitPrompt, model: Aadl): Int = {
    import org.sireum.cli.Cli
    
    val c = Class.forName(Tool.ARSIT.className)
    val m = c.getDeclaredMethod("run", classOf[File], classOf[Aadl], classOf[Cli.ArsitOption])

    val out: File = new File(prompt.getOptionOutputDirectory());

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

    m.invoke(null, out, model, opts).asInstanceOf[Int].intValue()
  }
}