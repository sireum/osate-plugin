package org.sireum.aadl.osate.util

import java.io.PrintStream
import org.eclipse.ui.console.MessageConsole

object ScalaUtil {

  def callWrapper(toolName: String, ms: MessageConsole, f: () => Int): Int = {
    var ret = -1

    val out = new PrintStream(ms.newMessageStream())
    val outOld = System.out
    val errOld = System.err

    Console.withOut(out) {
    Console.withErr(out) {

      try {
        ret = f()
      } catch {
        case e: Throwable =>  
          out.println(s"Exception raised when invoking ${toolName}")
          e.printStackTrace(out)
      } finally {
        out.flush
      
        try { if (out != null) out.close() }
          catch { case e: Throwable => e.printStackTrace() }
        }
      }
    }
    System.setOut(outOld)
    System.setErr(errOld)
    
    ret
  }
}