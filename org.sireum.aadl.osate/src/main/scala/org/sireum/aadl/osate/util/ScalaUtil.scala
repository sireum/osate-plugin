package org.sireum.aadl.osate.util

import org.sireum.{None, Option, Some}
import org.osate.aadl2.Element
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import java.io.PrintStream
import org.eclipse.ui.console.MessageConsole

object ScalaUtil {

  def callWrapper(toolName: String, ms: MessageConsole, f: () => Int): Int = {
    var ret = -1

    val out = new PrintStream(ms.newMessageStream())
    val outOld = System.out
    val errOld = System.err
    
    System.setOut(out)
    System.setErr(out)
    
    Console.withOut(out) {
    Console.withErr(out) {
      
      try {
        ret = f()
      } catch {
        case e: Exception =>  
          out.println(s"Exception raised when invoking ${toolName}")
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