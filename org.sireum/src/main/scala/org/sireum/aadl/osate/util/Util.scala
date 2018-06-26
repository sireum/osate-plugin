package org.sireum.aadl.osate.util

import java.io.File
import org.sireum.aadl.ir.Aadl
import org.sireum.cli.Cli
import org.sireum.{ISZ, Some, String, Z}

object Util {
  def launchArsit(out: File, model: Aadl): Int = {
    val c = Class.forName("org.sireum.aadl.arsit.Runner")
		val m = c.getDeclaredMethod("run", classOf[File], classOf[Aadl], classOf[Cli.ArsitOption])
    
		// using sireum's cli parser to populate ArsitOption.  To see the available arsit options, you can run
		// "java -jar bin/sireum.jar x aadl arsit" from the command line, or just uncomment the '--fakeOpt' entry
    val args : ISZ[String] = ISZ(
        // "--fakeOpt" // uncomment this entry to see the available arsit options
        "--output-dir", out.getAbsolutePath,
        //"--package-name", "foo_bar",
        //"--noart",
        //"--bless",
        "--trans",
        //"--ipc", "shared_memory"
        )

    val cli = Cli(File.pathSeparatorChar)
    cli.parseArsit(ISZ(args.elements: _*), Z(0)) match {
      case Some(o: Cli.ArsitOption) =>
        m.invoke(null, out, model, o).asInstanceOf[Int].intValue()
      case _ => 
        println(cli.parseArsit(ISZ(), 0).get.asInstanceOf[Cli.ArsitOption].help)
        -1
    }
  }
}