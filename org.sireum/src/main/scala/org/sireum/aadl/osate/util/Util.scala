package org.sireum.aadl.osate.util

import java.io.File
import org.sireum.aadl.ir.Aadl
import org.sireum.cli.Cli
import org.sireum.{F, T, ISZ, None, Some, String, Z}

object Util {
  def launchArsit(prompt: ArsitPrompt, model: Aadl): Int = {
    val c = Class.forName("org.sireum.aadl.arsit.Runner")
    val m = c.getDeclaredMethod("run", classOf[File], classOf[Aadl], classOf[Cli.ArsitOption])

    val out: File = new File(prompt.getOptionOutputDirectory());

    val ipcmech = prompt.getOptionIPCMechanism match {
      case "Message Queue" => Cli.Ipcmech.Message_queue
      case "Shared Memory" => Cli.Ipcmech.Shared_memory
      case _ => Cli.Ipcmech.Message_queue
    }

    val opts = Cli.ArsitOption(
        help = "",
        args = ISZ(),
        json = F, // irrelevant since passing the aadl model directly
        inputFile = None[String],
        outputDir = Some(prompt.getOptionOutputDirectory),
        packagename = if(prompt.getOptionBasePackageName == "") None[String] else Some(prompt.getOptionBasePackageName()),
        noart = !prompt.getOptionEmbedArt,
        bless = prompt.getOptionGenerateBlessEntryPoints,
        genTrans = prompt.getOptionGenerateTranspilerArtifacts,
        ipc = ipcmech
    )

    m.invoke(null, out, model, opts).asInstanceOf[Int].intValue()
  }
}