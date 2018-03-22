package org.sireum.aadl.ostate.tests

import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.osate.aadl2.AadlPackage
import org.osate.aadl2.SystemImplementation
import org.osate.aadl2.instantiation.InstantiateModel
import org.osate.core.test.Aadl2UiInjectorProvider
import org.osate.core.test.OsateTest
import org.sireum.aadl.osate.util.TestUtil
import java.io.File
import java.nio.file.Files
import java.util.ArrayList
import java.nio.file.Paths
import org.osate.aadl2.Classifier
import org.eclipse.emf.common.util.EList

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(Aadl2UiInjectorProvider))
class AirTests extends OsateTest {

  val root = new File("./projects/org/sireum/aadl/ostate/tests/")

  @Test
  def void pca_pump_chasis(){
    execute("pca-pump-chasis-gen", "Chassis.aadl", "Chassis_System.i")
  }


  // helper methods  
  def execute(String dirName, String sysFilename, String sysImplName ) {
    val r = new File(root, dirName)
    val l = new ArrayList<Pair<String, String>> ()
    var expected = ""
    for(File _f : r.listFiles()) {
      if(_f.getName().endsWith(".aadl"))
        l.add(_f.getName() -> readFile(_f))
      else if(_f.getName() == "expected")
        expected = readFile(_f)
    	}
    createFiles(l)
    	   
    suppressSerialization
    val result = testFile(sysFilename)

    val pkg = result.resource.contents.head as AadlPackage
    assertAllCrossReferencesResolvable(pkg)

    // instantiate
    val sysImpl = getResourceByName(sysImplName, pkg.ownedPublicSection.ownedClassifiers) as SystemImplementation
    val instance = InstantiateModel::buildInstanceModelFile(sysImpl)

    val ir = TestUtil::getAir(instance) as String
    
    println("ir:       " + ir)
    println("expected: " + expected)
    
    Assert.assertEquals(ir, expected)
  }

  def String readFile(File f) {
    return new String(Files.readAllBytes(Paths.get(f.toURI()))) 
  }

  def Classifier getResourceByName(String name, EList<Classifier> l) {
    for(oc : l) {
      if(oc.getName() == name) {
        return oc
      }
    }
    return null
  }
}