package org.sireum.architecture

import org.sireum._
import org.osate.aadl2._
import org.osate.aadl2.impl._
import org.osate.aadl2.util.Aadl2InstanceUtil
import org.osate.contribution.sei.names._
import org.osate.xtext.aadl2.properties.util.GetProperties;
import org.osate.xtext.aadl2.properties.util.PropertyUtils;
import org.osate.xtext.aadl2.properties.util.TimingProperties;
import org.osate.aadl2.instance.util._
import org.osate.aadl2.instance._
import scala.collection.JavaConversions._
import org.sireum.aadl.skema._
import org.osate.xtext.aadl2.serializer.InstanceEnabledSerializer
import org.osate.xtext.aadl2.errormodel.errorModel._
import org.osate.xtext.aadl2.errormodel.util.EMV2Util
import org.osate.xtext.aadl2.errormodel.errorModel.impl._
import org.osate.aadl2.util.Aadl2Util
import org.osate.aadl2.ComponentCategory._
 

object Visitor {

  var seenSet: ISZ[AadlPackageImpl] = ISZ()
  var dataTypes: ISZ[ast.Component] = ISZ()
  var errorLibs: HashMap[String, ISZ[String]] = HashMap.empty[String, ISZ[String]]()
  var compConnMap: HashMap[ISZ[String], HashSet[ConnectionReference]] = HashMap.empty[ISZ[String], HashSet[ConnectionReference]]()

  def convert(root: Element): Option[ast.Aadl] = {
    val t = visit(root)
    if (t.nonEmpty) {
      return Some[ast.Aadl](ast.Aadl(components = ISZ(t.get) ++ dataTypes, errorLibs.entries.map(f =>
        ast.Emv2Library(ast.Name(ISZ[String](f._1.value)), f._2))))
    } else {
      None[ast.Aadl]
    }
  }

  /*
  private def handlePackageTypes(e: AadlPackageImpl): Unit = {
    if (!seenSet.elements.contains(e)) {
      val p1 = e.getPublicSection

      var components = ISZ[ast.Component]()
      for (c <- p1.getOwnedClassifiers) {
        c.eClass.getClassifierID match {
          case Aadl2Package.DATA_TYPE =>
            val dt = c.asInstanceOf[DataType]

            var properties = ISZ[ast.Property]()
            for (pa <- dt.getOwnedPropertyAssociations)
              properties :+= visit(pa)

            val comp = ast.Component(
              identifier = Some(dt.getName),
              category = ast.ComponentCategory.Data,
              classifier = Some(ast.Classifier(e.getQualifiedName)),
              features = ISZ(),
              subComponents = ISZ(),
              connections = ISZ(),
              properties = properties,
              flows = ISZ(),
              modes = ISZ[ast.Mode](),
              annexes = ISZ[ast.Annex]())

            dataTypes :+= comp
          case x =>
            println(s"not handling $e with type $x")
        }
      }
      seenSet :+= e
    }
  }
*/

  private def visit(root: Element): Option[ast.Component] = {
    val metaId = root.eClass.getClassifierID

    metaId match {
      case InstancePackage.SYSTEM_INSTANCE |
        InstancePackage.COMPONENT_INSTANCE =>

        val o = root.asInstanceOf[ComponentInstance]

        Some[ast.Component](buildComponent(o, ISZ[String]()))
      case _ => None[ast.Component]
    }
  }

  private def handleDirection(d: DirectionType): ast.Direction.Type = {
    import org.osate.aadl2.DirectionType._
    return d match {
      case IN => ast.Direction.In
      case OUT => ast.Direction.Out
      case IN_OUT => ast.Direction.InOut
      case _ => ast.Direction.None
    }
  }

  private def visitEmv2(root: ComponentInstance, path: ISZ[String]): ast.Annex = {

    def errorProp2Map(errorProp: Seq[ErrorPropagation], isIn: Boolean): ISZ[ast.Emv2Propagation] = {
      var prop = ISZ[ast.Emv2Propagation]()
      errorProp.forEach { x =>
        var inErrorTokens = Set.empty[String]
        x.getTypeSet.getTypeTokens.forEach { y =>
          y.getType.forEach { z =>
            inErrorTokens = inErrorTokens.add(z.getName)
          }
        }
        prop :+= ast.Emv2Propagation(
          if (isIn) ast.PropagationDirection.In else ast.PropagationDirection.Out,
          path :+ x.getFeatureorPPRef.getFeatureorPP.getName, inErrorTokens.elements)
      }
      prop
    }

    val inprop = errorProp2Map(EMV2Util.getAllIncomingErrorPropagations(root)
      .filter(_.isInstanceOf[ErrorPropagationImpl])
      .map(_.asInstanceOf[ErrorPropagationImpl]).toSeq, true)

    val outprop = errorProp2Map(EMV2Util.getAllOutgoingErrorPropagations(root.getComponentClassifier)
      .filter(_.isInstanceOf[ErrorPropagationImpl])
      .map(_.asInstanceOf[ErrorPropagationImpl]).toSeq, false)

    var libNames = ISZ[String]()

    val el = EMV2Util.getErrorModelSubclauseWithUseTypes(root.getComponentClassifier)
    if (el != null) {
      el.foreach { e =>
        errorLibs = errorLibs.put(EMV2Util.getLibraryName(e), ISZ(e.getTypes.map(s => String(s.getName)).toSeq: _*))
        libNames :+= EMV2Util.getLibraryName(e)
      }
    }

    var sources = ISZ[ast.Emv2Flow]()
    EMV2Util.getAllErrorSources(root.getComponentClassifier).foreach { src =>
      val name = src.getName
      if (src.getSourceModelElement.isInstanceOf[ErrorPropagation]) {
        val s = src.getSourceModelElement.asInstanceOf[ErrorPropagation]
        val featureName = s.getFeatureorPPRef.getFeatureorPP.getName
        var prop: Option[ast.Emv2Propagation] = None[ast.Emv2Propagation]
        if (src.getTypeTokenConstraint != null) {
          val errorP = src.getTypeTokenConstraint.getTypeTokens.flatMap(tt =>
            tt.getType.map(et => String(et.getName)))
          prop = Some[ast.Emv2Propagation](ast.Emv2Propagation(ast.PropagationDirection.Out, path :+ featureName, ISZ(errorP.toSeq: _*)))
        } else {
          prop = Some[ast.Emv2Propagation](errorProp2Map(Seq(s), false)(0))
        }
        sources :+= ast.Emv2Flow(ast.Name(path :+ name), ast.FlowKind.Source, None[ast.Emv2Propagation], prop)
      }
    }

    var sinks = ISZ[ast.Emv2Flow]()
    EMV2Util.getAllErrorSinks(root.getComponentClassifier).foreach { snk =>
      val name = snk.getName
      val featureName = snk.getIncoming.getFeatureorPPRef.getFeatureorPP.getName
      var prop: Option[ast.Emv2Propagation] = None[ast.Emv2Propagation]
      if (snk.getTypeTokenConstraint != null) {
        val errorP = snk.getTypeTokenConstraint.getTypeTokens.flatMap(tt =>
          tt.getType.map(et => String(et.getName)))
        prop = Some[ast.Emv2Propagation](ast.Emv2Propagation(ast.PropagationDirection.In, path :+ featureName, ISZ(errorP.toSeq: _*)))
      } else {
        prop = Some[ast.Emv2Propagation](errorProp2Map(Seq(snk.getIncoming), true)(0))
      }
      sinks :+= ast.Emv2Flow(ast.Name(path :+ name), ast.FlowKind.Sink, prop, None[ast.Emv2Propagation])
    }

    var paths = ISZ[ast.Emv2Flow]()
    EMV2Util.getAllErrorPaths(root.getComponentClassifier).foreach { pth =>
      val name = pth.getName
      var inerror: Option[ast.Emv2Propagation] = None[ast.Emv2Propagation]
      var outerror: Option[ast.Emv2Propagation] = None[ast.Emv2Propagation]
      if (pth.getTypeTokenConstraint != null) {
        inerror = Some[ast.Emv2Propagation](ast.Emv2Propagation(
          ast.PropagationDirection.In,
          path :+ pth.getIncoming.getFeatureorPPRef.getFeatureorPP.getName,
          ISZ(pth.getTypeTokenConstraint.getTypeTokens.flatMap(tt =>
            tt.getType.map(et => String(et.getName))).toSeq: _*)))
      } else {
        inerror = Some[ast.Emv2Propagation](errorProp2Map(Seq(pth.getIncoming), true)(0))
      }

      if (pth.getTargetToken != null) {
        outerror = Some[ast.Emv2Propagation](ast.Emv2Propagation(
          ast.PropagationDirection.Out,
          path :+ pth.getOutgoing.getFeatureorPPRef.getFeatureorPP.getName,
          ISZ(pth.getTargetToken.getTypeTokens.flatMap(tt =>
            tt.getType.map(et => String(et.getName))).toSeq: _*)))
      } else {
        outerror = Some[ast.Emv2Propagation](errorProp2Map(Seq(pth.getOutgoing), false)(0))
      }

      paths :+= ast.Emv2Flow(ast.Name(path :+ name), ast.FlowKind.Path, inerror, outerror)
    }

    ast.Annex("Emv2", ast.Emv2Clause(libNames, inprop ++ outprop, sources ++ sinks ++ paths))
  }
  
  private def buildConnectionRef(connRef: ConnectionReference, path: ISZ[String]) : ast.ConnectionReference = {
    val context = ISZ(connRef.getContext.getInstanceObjectPath.split('.').map(String(_)).toSeq: _*)
    val name = context :+ String(connRef.getConnection.getName)
    if(compConnMap.get(context).nonEmpty) {
          compConnMap = compConnMap.put(context, compConnMap.get(context).
              get.add(connRef))
    } else {
      compConnMap = compConnMap.put(context, HashSet.empty.add(connRef))
    }
    ast.ConnectionReference(
    name = ast.Name(name),
    context = ast.Name(context),
    if(path == context) B(true) else B(false)
    )
  }

  private def buildConnectionInst(connInst: ConnectionInstance, path: ISZ[String]): ast.ConnectionInstance = {

    val currentPath = path :+ connInst.getName

    val srcComponent = connInst.getSource.getComponentInstance.getInstanceObjectPath.split('.').map(String(_)).toSeq

    val srcFeature = connInst.getSource.getInstanceObjectPath.split('.').map(String(_)).toSeq

    val dstComponent = connInst.getDestination.getComponentInstance.getInstanceObjectPath.split('.').map(String(_)).toSeq
    val dstFeature = connInst.getDestination.getInstanceObjectPath.split('.').map(String(_)).toSeq

    val properties = ISZ[ast.Property](connInst.getOwnedPropertyAssociations.map(op =>
      buildProperty(op, currentPath)).toSeq: _*)

    import org.osate.aadl2.instance.ConnectionKind
    val kind = connInst.getKind match {
      case ConnectionKind.ACCESS_CONNECTION => ast.ConnectionKind.Access
      case ConnectionKind.FEATURE_CONNECTION => ast.ConnectionKind.Feature
      case ConnectionKind.FEATURE_GROUP_CONNECTION => ast.ConnectionKind.FeatureGroup
      case ConnectionKind.MODE_TRANSITION_CONNECTION => ast.ConnectionKind.ModeTransition
      case ConnectionKind.PARAMETER_CONNECTION => ast.ConnectionKind.Parameter
      case ConnectionKind.PORT_CONNECTION => ast.ConnectionKind.Port
    }
    val conn = InstanceUtil.getCrossConnection(connInst)
    val connRefs = ISZ(connInst.getConnectionReferences.map(ci => buildConnectionRef(ci, path)): _*)

    return ast.ConnectionInstance(
      name = ast.Name(currentPath),
      src = ast.EndPoint(
        component = ast.Name(ISZ(srcComponent: _*)),
        feature = ast.Name(ISZ[String](srcFeature: _*))),
      dst = ast.EndPoint(
        component = ast.Name(ISZ[String](dstComponent: _*)),
        feature = ast.Name(ISZ[String](dstFeature: _*))),
      kind = kind,
      connectionRefs = connRefs,
      properties = properties)

  }
  
  private def buildConnection(connRef : ConnectionReference, path: ISZ[String]): ast.Connection = {
    val conn = connRef.getConnection
    val name = path :+ conn.getName
    val srcComp = conn.getSource.getContext
    val dstComp = conn.getDestination.getContext
    val source = buildEndPoint(conn.getSource, path)
    val destination = buildEndPoint(conn.getDestination, path)
    val isBidirect = B(conn.isBidirectional())
    val connInst = ast.Name(ISZ(InstanceUtil.findConnectionInstance(connRef.getSystemInstance, conn)
    .getInstanceObjectPath.split('.').map(String(_)).toSeq: _*))
    val properties =ISZ[ast.Property](connRef.getOwnedPropertyAssociations.map(op =>
      buildProperty(op, name)).toSeq: _*)
      
    ast.Connection(ast.Name(name), source, destination, isBidirect, connInst, properties)
  }
  
  private def buildEndPoint(connElem : ConnectedElement, path :  ISZ[String]) : ast.EndPoint = {
    val component = if(connElem.getContext != null) {
      path :+ connElem.getContext.getFullName
      // ISZ[String]()
    } else path
    val feature = component :+ connElem.getConnectionEnd.getName
    ast.EndPoint(ast.Name(component), ast.Name(feature))
  }

  private def buildComponent(compInst: ComponentInstance, path: ISZ[String]): ast.Component = {

    val currentPath = path :+ compInst.getName

    val features = ISZ[ast.Feature](compInst.getFeatureInstances.map(fi =>
      buildFeature(fi, currentPath)).toSeq: _*)

    val connectionInstances = ISZ[ast.ConnectionInstance](compInst.getConnectionInstances.map(ci =>
      buildConnectionInst(ci, currentPath)).toSeq: _*)

    val properties = ISZ[ast.Property](compInst.getOwnedPropertyAssociations.map(pa =>
      buildProperty(pa, currentPath)).toSeq: _*)

    val flows = ISZ[ast.Flow](compInst.getFlowSpecifications.map(fs =>
      buildFlow(fs, currentPath)).toSeq: _*)

    val components = ISZ[ast.Component](compInst.getComponentInstances.map(ci =>
      buildComponent(ci, currentPath)).toSeq: _*)

    val classifier = if (compInst.getClassifier != null) {
      Some(ast.Classifier(compInst.getClassifier.getQualifiedName))
    } else {
      None[ast.Classifier]
    }
    
    var connections = ISZ[ast.Connection]()
      
      if(compConnMap.get(currentPath).nonEmpty) {
        val res = compConnMap.get(currentPath).get.elements.map{c =>
          val xx= c
          buildConnection(c.asInstanceOf[ConnectionReference], currentPath)
       }
       connections = ISZ[ast.Connection](res.elements.toSeq: _*)
      } 
    


    val cat = compInst.getCategory match {
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

    val comp =
      ast.Component(
        identifier = ast.Name(currentPath),
        category = cat,
        classifier = classifier,
        features = features,
        subComponents = components,
        connections = connections,
        connectionInstances = connectionInstances,
        properties = properties,
        flows = flows,
        modes = ISZ[ast.Mode](),
        annexes = ISZ[ast.Annex](visitEmv2(compInst, currentPath)))

    return comp

  }

  private def buildFeature(featureInst: FeatureInstance, path: ISZ[String]): ast.Feature = {
    val f = featureInst.getFeature
    val currentPath = path :+ f.getName

    val classifier = if (f.getFeatureClassifier != null && f.getFeatureClassifier.isInstanceOf[DataTypeImpl]) {
      val dt = f.getFeatureClassifier.asInstanceOf[DataTypeImpl]

      // uncomment the following to include all the data type defs declared
      // within the package that contains dt
      // val er = dt.getElementRoot.asInstanceOf[AadlPackageImpl]
      // handlePackageTypes(er)

      Some(ast.Classifier(dt.getQualifiedName))
    } else {
      None[ast.Classifier]
    }

    val properties = ISZ[ast.Property](featureInst.getOwnedPropertyAssociations.map(op =>
      buildProperty(op, currentPath)).toSeq: _*)

    import org.osate.aadl2.instance.FeatureCategory._

    val typ = featureInst.getCategory match {
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
      identifier = ast.Name(currentPath),
      classifier = classifier,
      direction = handleDirection(featureInst.getDirection),
      category = typ,
      properties = properties)
  }

  private def buildFlow(flowInst: FlowSpecificationInstance, path: ISZ[String]): ast.Flow = {
    val currentPath = path :+ flowInst.getQualifiedName
    val d = flowInst.getDestination
    val s = flowInst.getSource
    val flowKind = flowInst.getFlowSpecification.getKind match {
      case FlowKind.SOURCE => ast.FlowKind.Source
      case FlowKind.SINK => ast.FlowKind.Sink
      case FlowKind.PATH => ast.FlowKind.Path
    }
    return ast.Flow(name = ast.Name(currentPath), kind = flowKind,
      if (s != null) Some(s.getQualifiedName) else None[String],
      if (d != null) Some(d.getQualifiedName) else None[String])

  }

  private def getPropertyExpressionValue(pe: PropertyExpression): ISZ[ast.PropertyValue] = {
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
          Unit = Some(rv.getMinimum.eClass().getName),
          ValueLow = if (rv.getMinimumValue != null) rv.getMinimumValue.toString.trim else "IT'S NULL",
          ValueHigh = if (rv.getMaximumValue != null) rv.getMaximumValue.toString.trim else "IT'S NULL"))
      case Aadl2Package.CLASSIFIER_VALUE =>
        val cv = pe.asInstanceOf[ClassifierValue].getClassifier
        return ISZ(ast.ClassifierProp(cv.getQualifiedName))
      case Aadl2Package.LIST_VALUE =>
        val lv = pe.asInstanceOf[ListValue]
        var elems = ISZ[ast.PropertyValue]()
        for (e <- lv.getOwnedListElements)
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
            if (_p.getDefaultValue != null)
              return getPropertyExpressionValue(_p.getDefaultValue)
            else
              return ISZ(ast.UnitProp(unit = nv2Name, value = _p.getQualifiedName))
          case Aadl2Package.PROPERTY_CONSTANT =>
            val pc = nv2.asInstanceOf[PropertyConstant]
            return ISZ(ast.UnitProp(unit = nv2Name, value = pc.getConstantValue.toString()))
          case xf =>
            println(s"Not handling $xf $nv2")
            return ISZ()
        }
      case Aadl2Package.REFERENCE_VALUE =>
        val rv = pe.asInstanceOf[ReferenceValue]
        println(s"Need to handle ReferenceValue $rv")
        return ISZ(ast.UnitProp(unit = eName, value = rv.toString))
      case InstancePackage.INSTANCE_REFERENCE_VALUE =>
        // FIXME: id is coming from InstancePackage rather than Aadl2Package.  Might cause the
        // following cast to fail if there is an id clash
        val rv = pe.asInstanceOf[InstanceReferenceValue]
        return ISZ(ast.ReferenceProp(rv.getReferencedInstanceObject.getQualifiedName))
      case e =>
        println("Need to handle " + pe + " " + pe.eClass().getClassifierID)
        return ISZ(ast.ClassifierProp(pe.getClass.getName))
    }
  }

  private def buildProperty(pa: PropertyAssociation, path: ISZ[String]): ast.Property = {
    val prop = pa.getProperty
    val cont = pa.eContainer.asInstanceOf[NamedElement]
    val currentPath = path :+ prop.getQualifiedName

    val propValList = {
      try {
        val pe = PropertyUtils.getSimplePropertyValue(cont, prop)
        getPropertyExpressionValue(pe)
      } catch {
        case e: Throwable =>
          println(s"Error encountered while trying to fetch property value for ${prop.getQualifiedName} from ${cont.getQualifiedName} : ${e.getMessage}")
          ISZ[ast.PropertyValue]()
      }
    }

    return ast.Property(
      name = ast.Name(currentPath),
      propertyValues = propValList)
  }

}