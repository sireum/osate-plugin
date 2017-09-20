package org.sireum.architecture

import org.sireum._
import org.osate.aadl2._
import org.osate.aadl2.instance._
import scala.collection.JavaConversions._
import org.sireum.aadl.ast

object Visitor{
  
  implicit def _convert[T](a : ast.MyTop) : T = a.asInstanceOf[T] 

  def visit(root : Element) : ast.MyTop = {
     val metaId = root.eClass.getClassifierID
     
     metaId match {
       case InstancePackage.SYSTEM_INSTANCE |
            InstancePackage.COMPONENT_INSTANCE =>
         
         val o = root.asInstanceOf[ComponentInstance]
         
         var features = ISZ[ast.Feature]()
         for(fi <- o.getFeatureInstances)
           features :+= visit(fi)
         
         var connections = ISZ[ast.Connection]()
         for(ci <- o.getConnectionInstances)
           connections :+= visit(ci)
         
         var properties = ISZ[ast.Property]()
         for(pa <- o.getOwnedPropertyAssociations)
           properties :+= visit(pa)
         
         var flows = ISZ[ast.Flow]()
         for(fs <- o.getFlowSpecifications)
           flows :+= visit(fs)

         var components = ISZ[ast.Component]()
         for(ci <- o.getComponentInstances)
           components :+= visit(ci)
         
         import org.osate.aadl2.ComponentCategory._
         val cat = o.getCategory match{
           case ABSTRACT => ast.Category.Abstract
           case BUS => ast.Category.Bus
           case DATA => ast.Category.Data
           case DEVICE => ast.Category.Device
           case MEMORY => ast.Category.Memory
           case PROCESS => ast.Category.Process           
           case PROCESSOR => ast.Category.Processor
           case SUBPROGRAM => ast.Category.Subprogram
           case SUBPROGRAM_GROUP => ast.Category.SubprogramGroup
           case SYSTEM => ast.Category.System
           case THREAD => ast.Category.Thread
           case THREAD_GROUP => ast.Category.ThreadGroup
           case VIRTUAL_BUS => ast.Category.VirtualBus
           case VIRTUAL_PROCESSOR => ast.Category.VirtualProcessor
         }

         val comp = 
           ast.Component(
             identifier = o.getFullName,
             category = cat,
             
             classifier = ast.Classifier(),
             features = features,
             subComponents = components,
             connections = connections,
             properties = properties,
             flows = flows,
             modes = ISZ[ast.Mode](),
             annexes = ISZ[ast.Annex]())

         return comp
         
       case InstancePackage.CONNECTION_INSTANCE =>
         //println("It's a connection instance " + root)

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