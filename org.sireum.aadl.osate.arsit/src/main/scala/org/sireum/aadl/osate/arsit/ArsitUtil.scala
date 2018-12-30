package org.sireum.aadl.osate.arsit

import java.io.File
import java.io.PrintStream
import org.sireum.aadl.ir.Aadl
import org.sireum.aadl.osate.arsit.handlers.ArsitPrompt
import org.sireum.aadl.osate.util.Util.Tool
import org.sireum.{F, T, ISZ, None, Option, Some, String, Z}
import org.eclipse.ui.console.MessageConsole

object ArsitUtil {

  val className = "org.sireum.aadl.arsit.Runner"
  
  // Separating this out as it refers to the Sireum v3 artifact Cli.ArsitOption which may not be 
  // present in other builds (e.g. ACT)
  def launchArsit(prompt: ArsitPrompt, model: Aadl, ms: MessageConsole): Int = {

    import org.sireum.cli.Cli
    
    val c = Class.forName(className)
    val m = c.getDeclaredMethod("run", classOf[File], classOf[Aadl], classOf[Cli.ArsitOption])

    val outDir: File = new File(prompt.getOptionOutputDirectory());

    val ipcmech = prompt.getOptionIPCMechanism match {
      case "Message Queue" => Cli.Ipcmech.MessageQueue
      case "Shared Memory" => Cli.Ipcmech.SharedMemory
      case _ => Cli.Ipcmech.MessageQueue
    }

    var ret = -1

    val out = new PrintStream(ms.newMessageStream())
    val outOld = System.out
    val errOld = System.err
    
    System.setOut(out)
    System.setErr(out)
    
    Console.withOut(out) {
    Console.withErr(out) {
      try {
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
          ret = m.invoke(null, outDir, model, opts).asInstanceOf[Int].intValue()
  
        } catch {
          case e: Exception =>  
            out.println("Exception raised when invoking ACT")
            e.printStackTrace(out)
        } finally {
          out.flush
      
          try { if (out != null) out.close() }
          catch { case e: Exception => e.printStackTrace() }
        }
      }
    }
    System.setOut(outOld)
    System.setErr(errOld)
    
    ret
  }
}