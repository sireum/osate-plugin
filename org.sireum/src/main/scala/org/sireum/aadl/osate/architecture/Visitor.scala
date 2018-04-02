package org.sireum.aadl.osate.architecture

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
import org.sireum.aadl.ir
import org.osate.xtext.aadl2.serializer.InstanceEnabledSerializer
import org.osate.xtext.aadl2.errormodel.errorModel._
import org.osate.xtext.aadl2.errormodel.util.EMV2Util
import org.osate.xtext.aadl2.errormodel.errorModel.impl._
import org.osate.aadl2.util.Aadl2Util
import org.osate.aadl2.ComponentCategory._
import org.osate.aadl2.instance.impl._

object Visitor {

  var seenSet: ISZ[AadlPackageImpl] = ISZ()
  var dataTypes: ISZ[ir.Component] = ISZ()
  var errorLibs: HashSMap[String, ISZ[String]] = HashSMap.empty[String, ISZ[String]]()
  var compConnMap: HashSMap[ISZ[String], HashSSet[ConnectionReference]] = HashSMap.empty[ISZ[String], HashSSet[ConnectionReference]]()

  def convert(root: Element): Option[ir.Aadl] = {
    val t = visit(root)
    if (t.nonEmpty) {
      return Some[ir.Aadl](ir.Aadl(components = ISZ(t.get) ++ dataTypes, errorLibs.entries.map(f =>
        ir.Emv2Library(ir.Name(ISZ[String](f._1.value)), f._2))))
    } else {
      None[ir.Aadl]
    }
  }

  /*
  private def handlePackageTypes(e: AadlPackageImpl): Unit = {
    if (!seenSet.elements.contains(e)) {
      val p1 = e.getPublicSection

      var components = ISZ[ir.Component]()
      for (c <- p1.getOwnedClassifiers) {
        c.eClass.getClassifierID match {
          case Aadl2Package.DATA_TYPE =>
            val dt = c.asInstanceOf[DataType]

            var properties = ISZ[ir.Property]()
            for (pa <- dt.getOwnedPropertyAssociations)
              properties :+= visit(pa)

            val comp = ir.Component(
              identifier = Some(dt.getName),
              category = ir.ComponentCategory.Data,
              classifier = Some(ir.Classifier(e.getQualifiedName)),
              features = ISZ(),
              subComponents = ISZ(),
              connections = ISZ(),
              properties = properties,
              flows = ISZ(),
              modes = ISZ[ir.Mode](),
              annexes = ISZ[ir.Annex]())

            dataTypes :+= comp
          case x =>
            println(s"not handling $e with type $x")
        }
      }
      seenSet :+= e
    }
  }
*/

  private def visit(root: Element): Option[ir.Component] = {
    val metaId = root.eClass.getClassifierID
    compConnMap = HashSMap.empty[ISZ[String], HashSSet[ConnectionReference]]()
    metaId match {
      case InstancePackage.SYSTEM_INSTANCE |
        InstancePackage.COMPONENT_INSTANCE =>

        val o = root.asInstanceOf[ComponentInstance]

        Some[ir.Component](buildComponent(o, ISZ[String]()))
      case _ => None[ir.Component]
    }
  }

  private def handleDirection(d: DirectionType): ir.Direction.Type = {
    import org.osate.aadl2.DirectionType._
    return d match {
      case IN => ir.Direction.In
      case OUT => ir.Direction.Out
      case IN_OUT => ir.Direction.InOut
      case _ => ir.Direction.None
    }
  }

  private def visitEmv2(root: ComponentInstance, path: ISZ[String]): ir.Annex = {

    def errorProp2Map(errorProp: Seq[ErrorPropagation], isIn: Boolean): ISZ[ir.Emv2Propagation] = {
      var prop = ISZ[ir.Emv2Propagation]()
      errorProp.forEach { x =>
        var inErrorTokens = Set.empty[String]
        x.getTypeSet.getTypeTokens.forEach { y =>
          y.getType.forEach { z =>
            inErrorTokens = inErrorTokens + z.getName
          }
        }
        prop :+= ir.Emv2Propagation(
          if (isIn) ir.PropagationDirection.In else ir.PropagationDirection.Out,
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
        errorLibs = errorLibs + (EMV2Util.getLibraryName(e), ISZ(e.getTypes.map(s => String(s.getName)).toSeq: _*))
        libNames :+= EMV2Util.getLibraryName(e)
      }
    }

    var sources = ISZ[ir.Emv2Flow]()
    EMV2Util.getAllErrorSources(root.getComponentClassifier).foreach { src =>
      val name = src.getName
      if (src.getSourceModelElement.isInstanceOf[ErrorPropagation]) {
        val s = src.getSourceModelElement.asInstanceOf[ErrorPropagation]
        val featureName = s.getFeatureorPPRef.getFeatureorPP.getName
        var prop: Option[ir.Emv2Propagation] = None[ir.Emv2Propagation]
        if (src.getTypeTokenConstraint != null) {
          val errorP = src.getTypeTokenConstraint.getTypeTokens.flatMap(tt =>
            tt.getType.map(et => String(et.getName)))
          prop = Some[ir.Emv2Propagation](ir.Emv2Propagation(ir.PropagationDirection.Out, path :+ featureName, ISZ(errorP.toSeq: _*)))
        } else {
          prop = Some[ir.Emv2Propagation](errorProp2Map(Seq(s), false)(0))
        }
        sources :+= ir.Emv2Flow(ir.Name(path :+ name), ir.FlowKind.Source, None[ir.Emv2Propagation], prop)
      }
    }

    var sinks = ISZ[ir.Emv2Flow]()
    EMV2Util.getAllErrorSinks(root.getComponentClassifier).foreach { snk =>
      val name = snk.getName
      val featureName = snk.getIncoming.getFeatureorPPRef.getFeatureorPP.getName
      var prop: Option[ir.Emv2Propagation] = None[ir.Emv2Propagation]
      if (snk.getTypeTokenConstraint != null) {
        val errorP = snk.getTypeTokenConstraint.getTypeTokens.flatMap(tt =>
          tt.getType.map(et => String(et.getName)))
        prop = Some[ir.Emv2Propagation](ir.Emv2Propagation(ir.PropagationDirection.In, path :+ featureName, ISZ(errorP.toSeq: _*)))
      } else {
        prop = Some[ir.Emv2Propagation](errorProp2Map(Seq(snk.getIncoming), true)(0))
      }
      sinks :+= ir.Emv2Flow(ir.Name(path :+ name), ir.FlowKind.Sink, prop, None[ir.Emv2Propagation])
    }

    var paths = ISZ[ir.Emv2Flow]()
    EMV2Util.getAllErrorPaths(root.getComponentClassifier).foreach { pth =>
      val name = pth.getName
      var inerror: Option[ir.Emv2Propagation] = None[ir.Emv2Propagation]
      var outerror: Option[ir.Emv2Propagation] = None[ir.Emv2Propagation]
      if (pth.getTypeTokenConstraint != null) {
        inerror = Some[ir.Emv2Propagation](ir.Emv2Propagation(
          ir.PropagationDirection.In,
          path :+ pth.getIncoming.getFeatureorPPRef.getFeatureorPP.getName,
          ISZ(pth.getTypeTokenConstraint.getTypeTokens.flatMap(tt =>
            tt.getType.map(et => String(et.getName))).toSeq: _*)))
      } else {
        inerror = Some[ir.Emv2Propagation](errorProp2Map(Seq(pth.getIncoming), true)(0))
      }

      if (pth.getTargetToken != null) {
        outerror = Some[ir.Emv2Propagation](ir.Emv2Propagation(
          ir.PropagationDirection.Out,
          path :+ pth.getOutgoing.getFeatureorPPRef.getFeatureorPP.getName,
          ISZ(pth.getTargetToken.getTypeTokens.flatMap(tt =>
            tt.getType.map(et => String(et.getName))).toSeq: _*)))
      } else {
        outerror = Some[ir.Emv2Propagation](errorProp2Map(Seq(pth.getOutgoing), false)(0))
      }

      paths :+= ir.Emv2Flow(ir.Name(path :+ name), ir.FlowKind.Path, inerror, outerror)
    }

    ir.Annex("Emv2", ir.Emv2Clause(libNames, inprop ++ outprop, sources ++ sinks ++ paths))
  }

  private def buildConnectionRef(connRef: ConnectionReference, path: ISZ[String]): ir.ConnectionReference = {
    val context = ISZ(connRef.getContext.getInstanceObjectPath.split('.').map(String(_)).toSeq: _*)
    val name = context :+ String(connRef.getConnection.getName)
    if (compConnMap.get(context).nonEmpty) {
      compConnMap = compConnMap + (context, compConnMap.get(context).get + connRef)
    } else {
      compConnMap = compConnMap + (context, HashSSet.empty + connRef)
    }
    ir.ConnectionReference(
      name = ir.Name(name),
      context = ir.Name(context),
      if (path == context) B(true) else B(false))
  }

  private def buildConnectionInst(connInst: ConnectionInstance, path: ISZ[String]): ir.ConnectionInstance = {

    val currentPath = path :+ connInst.getName

    val srcComponent = connInst.getSource.getComponentInstance.getInstanceObjectPath.split('.').map(String(_)).toSeq
    val srcFeature = connInst.getSource.getInstanceObjectPath.split('.').map(String(_)).toSeq
    val srcDirection = if(connInst.getSource.isInstanceOf[FeatureInstanceImpl]) {
      org.sireum.Option.some[DirectionType](connInst.getSource.asInstanceOf[FeatureInstanceImpl].getDirection)
    } else {
      org.sireum.Option.none[DirectionType]()
    }

    val dstComponent = connInst.getDestination.getComponentInstance.getInstanceObjectPath.split('.').map(String(_)).toSeq
    val dstFeature = connInst.getDestination.getInstanceObjectPath.split('.').map(String(_)).toSeq
    val dstDirection = if(connInst.getDestination.isInstanceOf[FeatureInstanceImpl]) {
      org.sireum.Option.some[DirectionType](connInst.getDestination.asInstanceOf[FeatureInstanceImpl].getDirection)
    } else {
      org.sireum.Option.none[DirectionType]()
    }

    val temp = connInst.getSource
    val properties = ISZ[ir.Property](connInst.getOwnedPropertyAssociations.map(op =>
      buildProperty(op, currentPath)).toSeq: _*)

    import org.osate.aadl2.instance.ConnectionKind
    val kind = connInst.getKind match {
      case ConnectionKind.ACCESS_CONNECTION => ir.ConnectionKind.Access
      case ConnectionKind.FEATURE_CONNECTION => ir.ConnectionKind.Feature
      case ConnectionKind.FEATURE_GROUP_CONNECTION => ir.ConnectionKind.FeatureGroup
      case ConnectionKind.MODE_TRANSITION_CONNECTION => ir.ConnectionKind.ModeTransition
      case ConnectionKind.PARAMETER_CONNECTION => ir.ConnectionKind.Parameter
      case ConnectionKind.PORT_CONNECTION => ir.ConnectionKind.Port
    }
    val conn = InstanceUtil.getCrossConnection(connInst)
    val connRefs = ISZ(connInst.getConnectionReferences.map(ci => buildConnectionRef(ci, path)): _*)

    return ir.ConnectionInstance(
      name = ir.Name(currentPath),
      src = ir.EndPoint(
        component = ir.Name(ISZ(srcComponent: _*)),
        feature = ir.Name(ISZ[String](srcFeature: _*)),
        direction = srcDirection.map(getAIRDire)),
      dst = ir.EndPoint(
        component = ir.Name(ISZ[String](dstComponent: _*)),
        feature = ir.Name(ISZ[String](dstFeature: _*)),
        direction = dstDirection.map(getAIRDire)),
      kind = kind,
      connectionRefs = connRefs,
      properties = properties)

  }

  private def getAIRDire(dirType: DirectionType): ir.Direction.Type = {
    dirType match {
      case DirectionType.IN => ir.Direction.In
      case DirectionType.OUT => ir.Direction.Out
      case DirectionType.IN_OUT => ir.Direction.InOut
    }
  }

  private def buildConnection(connRef: ConnectionReference, path: ISZ[String]): ir.Connection = {
    val conn = connRef.getConnection
    val name = path :+ conn.getName
    val srcComp = conn.getSource.getContext
    val dstComp = conn.getDestination.getContext
    val source = buildEndPoint(conn.getSource, path)
    val destination = buildEndPoint(conn.getDestination, path)
    val isBidirect = B(conn.isBidirectional())
    val sysInst = connRef.getContext
    val connInst = sysInst.findConnectionInstance(conn)
    val connInst2 = if (connInst.nonEmpty) ISZ(connInst.map(ci =>
      ir.Name(ISZ(ci.getInstanceObjectPath.split('.').map(String(_)).toSeq: _*))).toSeq: _*)
    else ISZ(ir.Name(ISZ()))

    val properties = ISZ[ir.Property](connRef.getOwnedPropertyAssociations.map(op =>
      buildProperty(op, name)).toSeq: _*)

    ir.Connection(ir.Name(name), source, destination, isBidirect, connInst2, properties)
  }

  private def buildEndPoint(connElem: ConnectedElement, path: ISZ[String]): ir.EndPoint = {
    val component = if (connElem.getContext != null) {
      path :+ connElem.getContext.getFullName
      // ISZ[String]()
    } else path
    val feature = component :+ connElem.getConnectionEnd.getName
    if(connElem.getConnectionEnd.isInstanceOf[DirectedFeatureImpl]) {
    val inFeature = connElem.getConnectionEnd.asInstanceOf[DirectedFeatureImpl]
    val dir = if (inFeature.isIn() && inFeature.isOut()) {
      org.sireum.Option.some[ir.Direction.Type](ir.Direction.InOut)
    } else if (inFeature.isIn() && !inFeature.isOut()) {
      org.sireum.Option.some[ir.Direction.Type](ir.Direction.In)
    } else {
      org.sireum.Option.some[ir.Direction.Type](ir.Direction.Out)
    }
     ir.EndPoint(
      ir.Name(component),
      ir.Name(feature), dir)
    } else {ir.EndPoint(
      ir.Name(component),
      ir.Name(feature), org.sireum.Option.none[ir.Direction.Type]()) }

  }

  private def buildComponent(compInst: ComponentInstance, path: ISZ[String]): ir.Component = {

    val currentPath = path :+ compInst.getName

    val features = ISZ[ir.Feature](compInst.getFeatureInstances.map(fi =>
      buildFeature(fi, currentPath)).toSeq: _*)

    val connectionInstances = ISZ[ir.ConnectionInstance](compInst.getConnectionInstances.map(ci =>
      buildConnectionInst(ci, currentPath)).toSeq: _*)

    val properties = ISZ[ir.Property](compInst.getOwnedPropertyAssociations.map(pa =>
      buildProperty(pa, currentPath)).toSeq: _*)

    val flows = ISZ[ir.Flow](compInst.getFlowSpecifications.map(fs =>
      buildFlow(fs, currentPath)).toSeq: _*)

    val components = ISZ[ir.Component](compInst.getComponentInstances.map(ci =>
      buildComponent(ci, currentPath)).toSeq: _*)

    val classifier = if (compInst.getClassifier != null) {
      Some(ir.Classifier(compInst.getClassifier.getQualifiedName))
    } else {
      None[ir.Classifier]
    }

    var connections = ISZ[ir.Connection]()

    if (compConnMap.get(currentPath).nonEmpty) {
      val res = compConnMap.get(currentPath).get.elements.map { c =>
        val xx = c
        buildConnection(c.asInstanceOf[ConnectionReference], currentPath)
      }
      connections = ISZ[ir.Connection](res.elements.toSeq: _*)
    }

    val cat = compInst.getCategory match {
      case ABSTRACT => ir.ComponentCategory.Abstract
      case BUS => ir.ComponentCategory.Bus
      case DATA => ir.ComponentCategory.Data
      case DEVICE => ir.ComponentCategory.Device
      case MEMORY => ir.ComponentCategory.Memory
      case PROCESS => ir.ComponentCategory.Process
      case PROCESSOR => ir.ComponentCategory.Processor
      case SUBPROGRAM => ir.ComponentCategory.Subprogram
      case SUBPROGRAM_GROUP => ir.ComponentCategory.SubprogramGroup
      case SYSTEM => ir.ComponentCategory.System
      case THREAD => ir.ComponentCategory.Thread
      case THREAD_GROUP => ir.ComponentCategory.ThreadGroup
      case VIRTUAL_BUS => ir.ComponentCategory.VirtualBus
      case VIRTUAL_PROCESSOR => ir.ComponentCategory.VirtualProcessor
    }

    val comp =
      ir.Component(
        identifier = ir.Name(currentPath),
        category = cat,
        classifier = classifier,
        features = features,
        subComponents = components,
        connections = connections,
        connectionInstances = connectionInstances,
        properties = properties,
        flows = flows,
        modes = ISZ[ir.Mode](),
        annexes = ISZ[ir.Annex](visitEmv2(compInst, currentPath)))

    return comp

  }

  private def buildFeature(featureInst: FeatureInstance, path: ISZ[String]): ir.Feature = {
    val f = featureInst.getFeature
    val currentPath = path :+ f.getName

    val classifier = if (f.getFeatureClassifier != null && f.getFeatureClassifier.isInstanceOf[DataTypeImpl]) {
      val dt = f.getFeatureClassifier.asInstanceOf[DataTypeImpl]

      // uncomment the following to include all the data type defs declared
      // within the package that contains dt
      // val er = dt.getElementRoot.asInstanceOf[AadlPackageImpl]
      // handlePackageTypes(er)

      Some(ir.Classifier(dt.getQualifiedName))
    } else {
      None[ir.Classifier]
    }

    val properties = ISZ[ir.Property](featureInst.getOwnedPropertyAssociations.map(op =>
      buildProperty(op, currentPath)).toSeq: _*)

    import org.osate.aadl2.instance.FeatureCategory._

    val typ = featureInst.getCategory match {
      case ABSTRACT_FEATURE => ir.FeatureCategory.AbstractFeature
      case BUS_ACCESS => ir.FeatureCategory.BusAccess
      case DATA_ACCESS => ir.FeatureCategory.DataAccess
      case DATA_PORT => ir.FeatureCategory.DataPort
      case EVENT_PORT => ir.FeatureCategory.EventPort
      case EVENT_DATA_PORT => ir.FeatureCategory.EventDataPort
      case FEATURE_GROUP => ir.FeatureCategory.FeatureGroup
      case PARAMETER => ir.FeatureCategory.Parameter
      case SUBPROGRAM_ACCESS => ir.FeatureCategory.SubprogramAccess
      case SUBPROGRAM_GROUP_ACCESS => ir.FeatureCategory.SubprogramAccessGroup
    }

    return ir.Feature(
      identifier = ir.Name(currentPath),
      classifier = classifier,
      direction = handleDirection(featureInst.getDirection),
      category = typ,
      properties = properties)
  }

  private def buildFlow(flowInst: FlowSpecificationInstance, path: ISZ[String]): ir.Flow = {
    val currentPath = path :+ flowInst.getQualifiedName
    val d = flowInst.getDestination
    val s = flowInst.getSource
    val flowKind = flowInst.getFlowSpecification.getKind match {
      case FlowKind.SOURCE => ir.FlowKind.Source
      case FlowKind.SINK => ir.FlowKind.Sink
      case FlowKind.PATH => ir.FlowKind.Path
    }
    return ir.Flow(name = ir.Name(currentPath), kind = flowKind,
      if (s != null) Some(s.getQualifiedName) else None[String],
      if (d != null) Some(d.getQualifiedName) else None[String])

  }

  private def getPropertyExpressionValue(pe: PropertyExpression): ISZ[ir.PropertyValue] = {
    val eName = pe.eClass().getName
    pe.eClass().getClassifierID match {
      case Aadl2Package.BOOLEAN_LITERAL =>
        val b = pe.asInstanceOf[BooleanLiteral].getValue.toString
        return ISZ(ir.UnitProp(unit = eName, value = b))
      case Aadl2Package.INTEGER_LITERAL =>
        val i = pe.asInstanceOf[IntegerLiteral].getValue.toString
        return ISZ(ir.UnitProp(unit = eName, value = i))
      case Aadl2Package.REAL_LITERAL =>
        val r = pe.asInstanceOf[RealLiteral].getValue.toString
        return ISZ(ir.UnitProp(unit = eName, value = r))
      case Aadl2Package.STRING_LITERAL =>
        val v = pe.asInstanceOf[StringLiteral].getValue
        return ISZ(ir.UnitProp(unit = eName, value = v))

      case Aadl2Package.RANGE_VALUE =>
        val rv = pe.asInstanceOf[RangeValue]
        return ISZ(ir.RangeProp(
          Unit = Some(rv.getMinimum.eClass().getName),
          ValueLow = if (rv.getMinimumValue != null) rv.getMinimumValue.toString.trim else "IT'S NULL",
          ValueHigh = if (rv.getMaximumValue != null) rv.getMaximumValue.toString.trim else "IT'S NULL"))
      case Aadl2Package.CLASSIFIER_VALUE =>
        val cv = pe.asInstanceOf[ClassifierValue].getClassifier
        return ISZ(ir.ClassifierProp(cv.getQualifiedName))
      case Aadl2Package.LIST_VALUE =>
        val lv = pe.asInstanceOf[ListValue]
        var elems = ISZ[ir.PropertyValue]()
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
            return ISZ(ir.UnitProp(unit = nv2Name, value = el.getFullName))
          case Aadl2Package.PROPERTY =>
            val _p = nv2.asInstanceOf[Property]
            if (_p.getDefaultValue != null)
              return getPropertyExpressionValue(_p.getDefaultValue)
            else
              return ISZ(ir.UnitProp(unit = nv2Name, value = _p.getQualifiedName))
          case Aadl2Package.PROPERTY_CONSTANT =>
            val pc = nv2.asInstanceOf[PropertyConstant]
            return ISZ(ir.UnitProp(unit = nv2Name, value = pc.getConstantValue.toString()))
          case xf =>
            println(s"Not handling $xf $nv2")
            return ISZ()
        }
      case Aadl2Package.REFERENCE_VALUE =>
        val rv = pe.asInstanceOf[ReferenceValue]
        println(s"Need to handle ReferenceValue $rv")
        return ISZ(ir.UnitProp(unit = eName, value = rv.toString))
      case InstancePackage.INSTANCE_REFERENCE_VALUE =>
        // FIXME: id is coming from InstancePackage rather than Aadl2Package.  Might cause the
        // following cast to fail if there is an id clash
        val rv = pe.asInstanceOf[InstanceReferenceValue]
        return ISZ(ir.ReferenceProp(rv.getReferencedInstanceObject.getQualifiedName))
      case e =>
        println("Need to handle " + pe + " " + pe.eClass().getClassifierID)
        return ISZ(ir.ClassifierProp(pe.getClass.getName))
    }
  }

  private def buildProperty(pa: PropertyAssociation, path: ISZ[String]): ir.Property = {
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
          ISZ[ir.PropertyValue]()
      }
    }

    return ir.Property(
      name = ir.Name(currentPath),
      propertyValues = propValList)
  }

}