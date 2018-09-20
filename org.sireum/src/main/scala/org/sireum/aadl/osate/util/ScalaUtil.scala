package org.sireum.aadl.osate.util

import java.io.File
import org.sireum.aadl.ir.Aadl
import org.sireum.aadl.osate.MenuContributions
import org.sireum.aadl.osate.util.Util.Tool
import org.sireum.{F, T, ISZ, None, Option, Some, String, Z}
import org.osate.aadl2.Element;
import org.osate.utils.Aadl2Utils
import org.eclipse.xtext.nodemodel.util.NodeModelUtils

object ScalaUtil {
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
        inputFile = None[String],
        outputDir = Some(prompt.getOptionOutputDirectory),
        packageName = if(prompt.getOptionBasePackageName == "") None[String] else Some(prompt.getOptionBasePackageName()),
        noart = !prompt.getOptionEmbedArt,
        bless = prompt.getOptionGenerateBlessEntryPoints,
        genTrans = prompt.getOptionGenerateTranspilerArtifacts,
        ipc = ipcmech
    )

    m.invoke(null, out, model, opts).asInstanceOf[Int].intValue()
  }
  
  def launchAct(prompt: ActPrompt, model: Aadl): Int = {
    val c = Class.forName(Tool.ACT.className)
    val m = c.getDeclaredMethod("run", classOf[File], classOf[Aadl])

    val out: File = new File(prompt.getOptionOutputDirectory());

    m.invoke(null, out, model).asInstanceOf[Int].intValue()
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