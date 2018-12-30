package org.sireum.aadl.osate.act

import java.io.File
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.sireum.aadl.ir.Aadl
import org.sireum.{F, T, ISZ, None, Option, Some, String, Z}
import org.osate.aadl2.Element
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import org.eclipse.ui.console.MessageConsole
import org.sireum.aadl.osate.act.handlers.ActPrompt

object ActUtil {
  val ACT_CLASS_NAME = "org.sireum.aadl.act.Act"
  
  def launchAct(prompt: ActPrompt, model: Aadl, ms: MessageConsole, workspaceRoot: File): Int = {

    var ret = -1

    val out = new PrintStream(ms.newMessageStream())
    val outOld = System.out
    val errOld = System.err
    
    System.setOut(out)
    System.setErr(out)
    
    Console.withOut(out) {
       Console.withErr(out) {
         try {
           val c = Class.forName(ACT_CLASS_NAME)
           val m = c.getDeclaredMethod("run", classOf[File], classOf[Aadl], classOf[ISZ[String]], classOf[Option[File]])

           val outDir: File = new File(prompt.getOptionOutputDirectory());
           val auxDirs : ISZ[String] = if(prompt.getOptionCSourceDirectory != "") ISZ(prompt.getOptionCSourceDirectory()) else ISZ()
           val aadlRootDir: Option[File] = Some(workspaceRoot)
           
          ret = m.invoke(null, outDir, model, auxDirs, aadlRootDir).asInstanceOf[Int].intValue()

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
