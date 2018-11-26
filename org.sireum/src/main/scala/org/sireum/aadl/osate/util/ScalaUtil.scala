package org.sireum.aadl.osate.util

import java.io.File
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.sireum.aadl.ir.Aadl
import org.sireum.aadl.osate.MenuContributions
import org.sireum.aadl.osate.util.Util.Tool
import org.sireum.{F, T, ISZ, None, Option, Some, String, Z}
import org.osate.aadl2.Element;
import org.osate.utils.Aadl2Utils
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import org.eclipse.ui.console.MessageConsole

object ScalaUtil {
 
  def launchAct(prompt: ActPrompt, model: Aadl, ms: MessageConsole): Int = {
    var ret = -1

    val out = new PrintStream(ms.newMessageStream())
    val outOld = System.out
    val errOld = System.err
    
    System.setOut(out)
    System.setErr(out)
    
    Console.withOut(out) {
       Console.withErr(out) {
         try {
           val c = Class.forName(Tool.ACT.className)
           val m = c.getDeclaredMethod("run", classOf[File], classOf[Aadl], classOf[ISZ[org.sireum.String]])

           val outDir: File = new File(prompt.getOptionOutputDirectory());
           val auxDirs : ISZ[org.sireum.String] = if(prompt.getOptionCSourceDirectory != "") ISZ(prompt.getOptionCSourceDirectory()) else ISZ()

          ret = m.invoke(null, outDir, model, auxDirs).asInstanceOf[Int].intValue()

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
  
  def getLocationReference(e: Element): Option[LocationReference] = {
    //import org.osate.aadl2.parsesupport.LocationReference
    var ret: Option[LocationReference] = None[LocationReference]()
    
    if(e == null)
      return ret
      
    var lr = e.getLocationReference
    if(lr == null){
      val node = NodeModelUtils.findActualNodeFor(e)

      if(node != null && e.eResource() != null){
        val lineCol = NodeModelUtils.getLineAndColumn(node, node.getOffset)         
        ret = Some(LocationReference(e.eResource().getURI.lastSegment(), node.getLength, lineCol.getLine, lineCol.getColumn, node.getOffset))
      }
    } else {
      ret = Some(LocationReference(lr.getFilename, lr.getLength, lr.getLine, -1, lr.getOffset))
    }
    ret
  }
}
  
case class LocationReference(filename: String,
                             length: Int = -1,
                             line: Int = -1,
                             col: Int = -1,
                             offset: Int = -1)