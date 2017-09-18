package org.sireum.architecture

import org.sireum._
import org.osate.aadl2._
import org.osate.aadl2.instance._
import scala.collection.JavaConversions._
import org.sireum.lang.ast
import scala.collection.mutable.ListBuffer

class Visitor {
  def visit(root : Element) : ast.MyTop = {
     val metaId = root.eClass.getClassifierID
     
     metaId match {
       case InstancePackage.SYSTEM_INSTANCE |
            InstancePackage.COMPONENT_INSTANCE =>
         
         val o = root.asInstanceOf[ComponentInstance]

         val id = o.getFullName;

         println("it's a component instance " + id)
         
         var isFeatures = ISZ[ast.Feature]()
         for(fi <- o.getFeatureInstances)
           isFeatures :+= visit(fi).asInstanceOf[ast.Feature]
         
         var connections = ListBuffer[ast.Connection]()
         for(ci <- o.getConnectionInstances)
           connections += visit(ci).asInstanceOf[ast.Connection]
         val isConnections = ISZ[ast.Connection](connections:_*)
         
         var properties = ListBuffer[ast.Property]()
         for(pa <- o.getOwnedPropertyAssociations){
           properties += visit(pa).asInstanceOf[ast.Property]
         }
         val isProperties = ISZ[ast.Property](properties:_*)
         
         var flows = ListBuffer[ast.Flow]()
         for(fs <- o.getFlowSpecifications)
           flows += visit(fs).asInstanceOf[ast.Flow]
         val isFlows = ISZ[ast.Flow](flows:_*)

         var components = ListBuffer[ast.Component]()
         for(ci <- o.getComponentInstances)
           components += visit(ci).asInstanceOf[ast.Component]
         val isComponents = ISZ[ast.Component](components:_*)
         
         import org.osate.aadl2.ComponentCategory._
         val cat = o.getCategory match{
           case SYSTEM => ast.Category.System
           case THREAD => ast.Category.Thread
           case PROCESSOR => ast.Category.Processor
           case BUS => ast.Category.Bus
           case DEVICE => ast.Category.Device
         }

         val comp = 
           ast.Component(
             identifier = o.getFullName,
             category = cat,
             
             classifier = ast.Classifier(),
             features = isFeatures,
             subComponents = isComponents,
             connections = isConnections,
             properties = isProperties,
             flows = isFlows,
             modes = ISZ[ast.Mode](),
             annexes = ISZ[ast.Annex]())

         return comp
         
       case InstancePackage.CONNECTION_INSTANCE =>
         println("It's a connection instance " + root)

         val o = root.asInstanceOf[ConnectionInstance]
         
         return ast.Connection(
             name = o.getQualifiedName,
             src = ast.EndPoint(component = o.getSource.getQualifiedName,
                                feature = "??"),
             dst = ast.EndPoint(component = o.getDestination.getQualifiedName,
                                feature = "??"),
             properties = ISZ[ast.Property]())
             
         
       case InstancePackage.FEATURE_INSTANCE =>
         //println("It's a feature instance " + root)
         val o = root.asInstanceOf[FeatureInstance]
         
         return ast.Feature(
             identifier = o.getQualifiedName,
             direction = ast.Direction.In,
             typ = ast.Typ.Data,
             classifier = ast.Classifier(),
             properties = ISZ[ast.Property]()) 

       case InstancePackage.FLOW_SPECIFICATION_INSTANCE =>
         //println("It's a flow specification instance " + root)
         val o = root.asInstanceOf[FlowSpecificationInstance]
         return ast.Flow(name = o.getQualifiedName)

       case InstancePackage.PROPERTY_ASSOCIATION_INSTANCE =>
         //println("It's a property association instance " + root)
         
         return ast.Property(name = "fix property",
             propertyValues = ISZ[ast.PropertyValue]())
         
       case _ => 
         println("default case " + metaId)

         println(root)
         
         return ast.Flow("This won't do")
     }
  }
}