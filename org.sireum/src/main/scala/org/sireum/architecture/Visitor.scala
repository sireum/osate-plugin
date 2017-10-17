package org.sireum.architecture

import org.sireum._
import org.osate.aadl2._
import org.osate.aadl2.impl._
import org.osate.contribution.sei.names._
import org.osate.xtext.aadl2.properties.util.GetProperties;
import org.osate.xtext.aadl2.properties.util.PropertyUtils;
import org.osate.aadl2.instance.util._
import org.osate.aadl2.instance._
import scala.collection.JavaConversions._
import org.sireum.aadl.ast
import org.osate.xtext.aadl2.serializer.InstanceEnabledSerializer

object Visitor {

  implicit def _convert[T](a: ast.MyTop): T = a.asInstanceOf[T]

  var seenSet:ISZ[AadlPackageImpl] = ISZ()
  var typePackages: ISZ[ast.Component] = ISZ()
  
  def displayTypePackages() : Unit = {
    for(t <- typePackages){
      println(ast.JSON.fromComponent(t, false))
    }
  }
  
  def handlePackageTypes(e: AadlPackageImpl) : Unit = {
    if(!seenSet.elements.contains(e)){
      val p1 = e.getPublicSection

      var features = ISZ[ast.Feature]()
      for(c <- p1.getOwnedClassifiers){
        c.eClass.getClassifierID match {
          case Aadl2Package.DATA_TYPE =>
            val dt = c.asInstanceOf[DataType]
            
            var properties = ISZ[ast.Property]()
            for (pa <- dt.getOwnedPropertyAssociations)
              properties :+= visit(pa)
              
            val f = ast.Feature(
              identifier = dt.getQualifiedName,
              classifier = ast.Classifier("??"),
              direction = ast.Direction.None,
              category = ast.FeatureCategory.AbstractFeature,
              properties = properties)
              
            features :+= f  
          case x =>
            println(s"not handling $e with type $x") 
        }
      }

      val comp =
          ast.Component(
            identifier = e.getFullName,
            category = ast.ComponentCategory.Abstract,

            classifier = ast.Classifier(""),
            features = features,
            subComponents = ISZ(),
            connections = ISZ(),
            properties = ISZ(),
            flows = ISZ(),
            modes = ISZ[ast.Mode](),
            annexes = ISZ[ast.Annex]())
            
        typePackages :+= comp
      seenSet :+= e
    }
  }
  
  def handleDirection(d: DirectionType): ast.Direction.Type = {
    import org.osate.aadl2.DirectionType._
    return d match {
      case IN => ast.Direction.In
      case OUT => ast.Direction.Out
      case IN_OUT => ast.Direction.InOut
      case _ => ast.Direction.None
    }
  }

  def visit(root: Element): ast.MyTop = {
    val metaId = root.eClass.getClassifierID

    metaId match {
      case InstancePackage.SYSTEM_INSTANCE |
        InstancePackage.COMPONENT_INSTANCE =>

        val o = root.asInstanceOf[ComponentInstance]

        var features = ISZ[ast.Feature]()
        for (fi <- o.getFeatureInstances)
          features :+= visit(fi)

        var connections = ISZ[ast.Connection]()
        for (ci <- o.getConnectionInstances)
          connections :+= visit(ci)

        var properties = ISZ[ast.Property]()
        for (pa <- o.getOwnedPropertyAssociations)
          properties :+= visit(pa)

        var flows = ISZ[ast.Flow]()
        for (fs <- o.getFlowSpecifications)
          flows :+= visit(fs)

        var components = ISZ[ast.Component]()
        for (ci <- o.getComponentInstances)
          components :+= visit(ci)
          
        import org.osate.aadl2.ComponentCategory._
        val cat = o.getCategory match {
          case ABSTRACT => ast.ComponentCategory.Abstract
          case BUS => ast.ComponentCategory.Bus
          case DATA => ast.ComponentCategory.Data
          case DEVICE => ast.ComponentCategory.Device
          case MEMORY => ast.ComponentCategory.Memory
          case PROCESS => ast.ComponentCategory.Process
          case PROCESSOR => ast.ComponentCategory.Processor
          case SUBPROGRAM => ast.ComponentCategory.Subprogram
          case SUBPROGRAM_GROUP => ast.ComponentCategory.SubprogramGroup
          case SYSTEM => ast.ComponentCategory.System
          case THREAD => ast.ComponentCategory.Thread
          case THREAD_GROUP => ast.ComponentCategory.ThreadGroup
          case VIRTUAL_BUS => ast.ComponentCategory.VirtualBus
          case VIRTUAL_PROCESSOR => ast.ComponentCategory.VirtualProcessor
        }

        var classifier = 
          if(o.getClassifier != null) o.getClassifier.getQualifiedName
          else ""

        val comp =
          ast.Component(
            identifier = o.getFullName,
            category = cat,

            classifier = ast.Classifier(classifier),
            features = features,
            subComponents = components,
            connections = connections,
            properties = properties,
            flows = flows,
            modes = ISZ[ast.Mode](),
            annexes = ISZ[ast.Annex]())

        return comp

      case InstancePackage.CONNECTION_INSTANCE =>
        val o = root.asInstanceOf[ConnectionInstance]
            
        val srcComponent = o.getSource.getComponentInstance.getQualifiedName
        val srcFeature = o.getSource.getQualifiedName
        
        val dstComponent = o.getDestination.getComponentInstance.getQualifiedName
        val dstFeature = o.getDestination.getQualifiedName
        
        var properties = ISZ[ast.Property]()
        for (p <- o.getOwnedPropertyAssociations)
          properties +:= visit(p)

        return ast.Connection(
          name = o.getQualifiedName,
          src = ast.EndPoint(
            component = srcComponent,
            feature = srcFeature),
          dst = ast.EndPoint(
            component = dstComponent,
            feature = dstFeature),
          properties = properties)

      case InstancePackage.FEATURE_INSTANCE =>
        import org.osate.aadl2.instance.FeatureCategory._

        val o = root.asInstanceOf[FeatureInstance]

        val f = o.getFeature
        val classifier = if(f.getFeatureClassifier != null && f.getFeatureClassifier.isInstanceOf[DataTypeImpl]){ 
          val dt = f.getFeatureClassifier.asInstanceOf[DataTypeImpl]
          val er = dt.getElementRoot.asInstanceOf[AadlPackageImpl]
          
          handlePackageTypes(er)

          ast.Classifier(dt.getQualifiedName)
        } else {
          ast.Classifier("")
        }
        
        var properties = ISZ[ast.Property]()
        for (p <- o.getOwnedPropertyAssociations)
          properties +:= visit(p)

        val typ = o.getCategory match {
          case ABSTRACT_FEATURE => ast.FeatureCategory.AbstractFeature
          case BUS_ACCESS => ast.FeatureCategory.BusAccess
          case DATA_ACCESS => ast.FeatureCategory.DataAccess
          case DATA_PORT => ast.FeatureCategory.DataPort
          case EVENT_PORT => ast.FeatureCategory.EventPort
          case EVENT_DATA_PORT => ast.FeatureCategory.EventDataPort
          case FEATURE_GROUP => ast.FeatureCategory.FeatureGroup
          case PARAMETER => ast.FeatureCategory.Parameter
          case SUBPROGRAM_ACCESS => ast.FeatureCategory.SubprogramAccess
          case SUBPROGRAM_GROUP_ACCESS => ast.FeatureCategory.SubprogramAccessGroup
        }

        return ast.Feature(
          identifier = o.getQualifiedName,
          classifier = classifier,
          direction = handleDirection(o.getDirection),
          category = typ,
          properties = properties)

      case InstancePackage.FLOW_SPECIFICATION_INSTANCE =>
        val o = root.asInstanceOf[FlowSpecificationInstance]
        return ast.Flow(name = o.getQualifiedName)

      case InstancePackage.PROPERTY_ASSOCIATION_INSTANCE | 
           Aadl2Package.PROPERTY_ASSOCIATION =>
        val o = root.asInstanceOf[PropertyAssociation]

        def getPropertyExpressionValue(pe:PropertyExpression) : ISZ[ast.PropertyValue] = {
          val eName = pe.eClass().getName        
          pe.eClass().getClassifierID match {
            case Aadl2Package.BOOLEAN_LITERAL =>
              val b = pe.asInstanceOf[BooleanLiteral].getValue.toString
              return ISZ(ast.UnitProp(unit = eName, value = b))
            case Aadl2Package.INTEGER_LITERAL =>
              val i = pe.asInstanceOf[IntegerLiteral].getValue.toString
              return ISZ(ast.UnitProp(unit = eName, value = i))
            case Aadl2Package.REAL_LITERAL =>
              val r = pe.asInstanceOf[RealLiteral].getValue.toString
              return ISZ(ast.UnitProp(unit = eName, value = r))
            case Aadl2Package.STRING_LITERAL =>
              val v = pe.asInstanceOf[StringLiteral].getValue
              return ISZ(ast.UnitProp(unit = eName, value = v))
              
            case Aadl2Package.RANGE_VALUE =>
              val rv = pe.asInstanceOf[RangeValue]
              return ISZ(ast.RangeProp(
                  Unit = rv.getMinimum.eClass().getName,
                  ValueLow = if(rv.getMinimumValue != null) rv.getMinimumValue.toString.trim else "IT'S NULL", 
                  ValueHigh = if(rv.getMaximumValue != null) rv.getMaximumValue.toString.trim else "IT'S NULL"))
            case Aadl2Package.CLASSIFIER_VALUE =>
              val cv = pe.asInstanceOf[ClassifierValue].getClassifier
              return ISZ(ast.ClassifierProp(cv.getQualifiedName))
            case Aadl2Package.LIST_VALUE =>
              val lv = pe.asInstanceOf[ListValue]
              var elems = ISZ[ast.PropertyValue]()
              for(e <- lv.getOwnedListElements)
                elems ++= getPropertyExpressionValue(e)
              return elems
            case Aadl2Package.NAMED_VALUE =>
              val nv = pe.asInstanceOf[NamedValue]
              val nv2 = nv.getNamedValue
              val nv2Name = nv2.eClass.getName
              nv2.eClass.getClassifierID match {
                case Aadl2Package.ENUMERATION_LITERAL =>
                  val el = nv2.asInstanceOf[EnumerationLiteral]
                  return ISZ(ast.UnitProp(unit = nv2Name, value = el.getFullName))
                case Aadl2Package.PROPERTY =>
                  val _p = nv2.asInstanceOf[Property]
                  return ISZ(ast.UnitProp(unit = nv2Name, value = _p.getQualifiedName))
                case Aadl2Package.PROPERTY_CONSTANT =>
                  val pc = nv2.asInstanceOf[PropertyConstant]
                  return ISZ(ast.UnitProp(unit = nv2Name, value=pc.getConstantValue.toString()))
                case xf =>
                  println(s"Not handling $xf $nv2")
                  return ISZ()
              }
            case Aadl2Package.REFERENCE_VALUE =>
              val rv = pe.asInstanceOf[ReferenceValue]
              println(s"Need to handle ReferenceValue $rv")
              return ISZ(ast.UnitProp(unit = eName, value= ""))
            case e => 
              println("Need to handle " + pe + " " +  pe.eClass().getClassifierID)
              return ISZ(ast.ClassifierProp(pe.getClass.getName))
          }
        }
        
        val prop = o.getProperty
        val cont = o.eContainer.asInstanceOf[NamedElement]

        val pe = PropertyUtils.getSimplePropertyValue(cont, prop)
        val propValList = getPropertyExpressionValue(pe)
 
        return ast.Property(
          name = prop.getQualifiedName,
          propertyValues = propValList)

      case _ =>
        return ast.Flow(s"Need to handle $metaId $root")
    }
  }
}