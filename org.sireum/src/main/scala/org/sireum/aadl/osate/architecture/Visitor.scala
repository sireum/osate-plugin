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
import org.osate.aadl2.modelsupport.util._

object Visitor {
  def apply(root: Element): Option[ir.Aadl] = {
    new Visitor().convert(root)
  }
}

class Visitor {

  var seenSet: ISZ[AadlPackageImpl] = ISZ()
  var dataTypes: ISZ[ir.Component] = ISZ()
  var errorLibs: HashSMap[String, (ISZ[String], ISZ[String], ISZ[(String, String)])] = HashSMap.empty[String, (ISZ[String], ISZ[String], ISZ[(String, String)])]()
  var compConnMap: HashSMap[ISZ[String], HashSSet[Connection]] = HashSMap.empty[ISZ[String], HashSSet[Connection]]()

  def convert(root: Element): Option[ir.Aadl] = {
    val t = visit(root)
    if (t.nonEmpty) {
      //errorLibs.entries.map(f => ir.Emv2Library(ir.Name(ISZ[String](f._1.value)), f._2._1, HashMap.empty ++ f._2._2))

      return Some[ir.Aadl](ir.Aadl(components = ISZ(t.get) ++ dataTypes, errorLibs.entries.map(f =>
        ir.Emv2Library(ir.Name(ISZ[String](f._1.value)), f._2._1, f._2._2, HashMap.empty ++ f._2._3))))
    } else {
      None[ir.Aadl]
    }
  }

  private def visit(root: Element): Option[ir.Component] = {
    val metaId = root.eClass.getClassifierID
    compConnMap = HashSMap.empty[ISZ[String], HashSSet[Connection]]()
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
          if (x.getFeatureorPPRef == null) (path :+ x.getKind) else path :+ getFeatureString(x.getFeatureorPPRef),
          inErrorTokens.elements)
      }
      prop
    }

    def getFeatureString(fpp: FeatureorPPReference): String = {
      var res = fpp.getFeatureorPP.getName
      var next = fpp.getNext
      while (next != null) {
        res = res + "_" + next.getFeatureorPP.getName
        next = next.getNext
      }
      res
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
        val temp = (String(EMV2Util.getLibraryName(e)), (
          ISZ((e.getUseTypes.map(EMV2Util.getLibraryName).map(x => String(x)) ++
            e.getExtends.map(EMV2Util.getLibraryName).map(x => String(x))).toSeq: _*),
          ISZ(e.getTypes.map(s => String(s.getName)).toSeq: _*),
          ISZ(e.getTypes.toSeq.flatMap(s => if (s.getAliasedType != null) { scala.Some((String(s.getName), String(s.getAliasedType.getName))) }
          else { scala.None }).toSeq: _*)))

        errorLibs = errorLibs + temp

        libNames :+= EMV2Util.getLibraryName(e)
      }
    }

    var sources = ISZ[ir.Emv2Flow]()
    EMV2Util.getAllErrorSources(root.getComponentClassifier).foreach { src =>
      val name = src.getName
      if (src.getSourceModelElement.isInstanceOf[ErrorPropagation]) {
        val s = src.getSourceModelElement.asInstanceOf[ErrorPropagation]
        val featureName = if (s.getFeatureorPPRef == null) String(s.getKind) else getFeatureString(s.getFeatureorPPRef)
        var prop: Option[ir.Emv2Propagation] = None[ir.Emv2Propagation]
        if (src.getTypeTokenConstraint != null) {
          val errorP = src.getTypeTokenConstraint.getTypeTokens.flatMap(tt =>
            tt.getType.map(et => String(et.getName)))
          prop = Some[ir.Emv2Propagation](ir.Emv2Propagation(ir.PropagationDirection.Out, path :+ featureName.value, ISZ(errorP.toSeq: _*)))
        } else {
          prop = Some[ir.Emv2Propagation](errorProp2Map(Seq(s), false)(0))
        }
        sources :+= ir.Emv2Flow(ir.Name(path :+ name), ir.FlowKind.Source, None[ir.Emv2Propagation], prop)
      }
    }

    var sinks = ISZ[ir.Emv2Flow]()
    EMV2Util.getAllErrorSinks(root.getComponentClassifier).foreach { snk =>
      val name = snk.getName
      val featureName = if (snk.getIncoming.getFeatureorPPRef == null) String(snk.getIncoming.getKind) else getFeatureString(snk.getIncoming.getFeatureorPPRef)
      var prop: Option[ir.Emv2Propagation] = None[ir.Emv2Propagation]
      if (snk.getTypeTokenConstraint != null) {
        val errorP = snk.getTypeTokenConstraint.getTypeTokens.flatMap(tt =>
          tt.getType.map(et => String(et.getName)))
        prop = Some[ir.Emv2Propagation](ir.Emv2Propagation(ir.PropagationDirection.In, path :+ featureName.value, ISZ(errorP.toSeq: _*)))
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
        val pp = if (pth.getIncoming.getFeatureorPPRef == null) String(pth.getIncoming.getKind) else getFeatureString(pth.getIncoming.getFeatureorPPRef)
        inerror = Some[ir.Emv2Propagation](ir.Emv2Propagation(
          ir.PropagationDirection.In,
          path :+ pp.value,
          ISZ(pth.getTypeTokenConstraint.getTypeTokens.flatMap(tt =>
            tt.getType.map(et => String(et.getName))).toSeq: _*)))
      } else {
        inerror = Some[ir.Emv2Propagation](errorProp2Map(Seq(pth.getIncoming), true)(0))
      }

      if (pth.getTargetToken != null) {
        val pp = if (pth.getOutgoing.getFeatureorPPRef == null) String(pth.getOutgoing.getKind) else getFeatureString(pth.getOutgoing.getFeatureorPPRef)
        outerror = Some[ir.Emv2Propagation](ir.Emv2Propagation(
          ir.PropagationDirection.Out,
          path :+ pp.value,
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
    val t = connRef.getConnection.getRefined
    if (compConnMap.get(context).nonEmpty) {
      compConnMap = compConnMap + (context, compConnMap.get(context).get + connRef.getConnection)
    } else {
      compConnMap = compConnMap + (context, HashSSet.empty + connRef.getConnection)
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
    val srcDirection = if (connInst.getSource.isInstanceOf[FeatureInstanceImpl]) {
      org.sireum.Option.some[DirectionType](connInst.getSource.asInstanceOf[FeatureInstanceImpl].getDirection)
    } else {
      org.sireum.Option.none[DirectionType]()
    }

    val dstComponent = connInst.getDestination.getComponentInstance.getInstanceObjectPath.split('.').map(String(_)).toSeq
    val dstFeature = connInst.getDestination.getInstanceObjectPath.split('.').map(String(_)).toSeq
    val dstDirection = if (connInst.getDestination.isInstanceOf[FeatureInstanceImpl]) {
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
        feature = Some(ir.Name(ISZ[String](srcFeature: _*))),
        direction = srcDirection.map(getAIRDire)),
      dst = ir.EndPoint(
        component = ir.Name(ISZ[String](dstComponent: _*)),
        feature = Some(ir.Name(ISZ[String](dstFeature: _*))),
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

  private def buildConnection(conn: Connection, path: ISZ[String], compInst: ComponentInstance): ISZ[ir.Connection] = {
    val name = path :+ conn.getName
    val srcComp = conn.getSource.getContext
    val dstComp = conn.getDestination.getContext
    val source = buildEndPoint(conn.getSource, path)
    val destination = buildEndPoint(conn.getDestination, path)
    val isBidirect = B(conn.isBidirectional())
    val connInst = compInst.findConnectionInstance(conn)
    val connInst2 = if (connInst.nonEmpty) ISZ(connInst.map(ci =>
      ir.Name(ISZ(ci.getInstanceObjectPath.split('.').map(String(_)).toSeq: _*))).toSeq: _*)
    else ISZ(ir.Name(ISZ()))

    val kind = conn match {
      case x: AccessConnection => ir.ConnectionKind.Access
      case x: FeatureGroupConnection => ir.ConnectionKind.FeatureGroup
      case x: FeatureConnection => ir.ConnectionKind.Feature
      case x: ParameterConnection => ir.ConnectionKind.Parameter
      case x: PortConnection => ir.ConnectionKind.Port
    }

    val properties = ISZ[ir.Property](conn.getOwnedPropertyAssociations.map(op =>
      buildProperty(op, name)).toSeq: _*)
    if (source.length != destination.length) {
      println("incorrect translation")
    }
    ISZ(ir.Connection(ir.Name(name), source, destination, kind, isBidirect, connInst2, properties))

  }

  /**
   * @param connElem
   * @param path
   * @return
   */
  private def buildEndPoint(connElem: ConnectedElement, path: ISZ[String]): ISZ[ir.EndPoint] = {
    var result = ISZ[ir.EndPoint]()
    val component = if (connElem.getContext != null) {
      path :+ connElem.getContext.getFullName
    } else path
    val feature = component :+ connElem.getConnectionEnd.getName
    var dir = org.sireum.Option.none[ir.Direction.Type]()
    if (connElem.getConnectionEnd.isInstanceOf[DirectedFeatureImpl]) {
      val inFeature = connElem.getConnectionEnd.asInstanceOf[DirectedFeatureImpl]
      dir = if (inFeature.isIn() && inFeature.isOut()) {
        org.sireum.Option.some[ir.Direction.Type](ir.Direction.InOut)
      } else if (inFeature.isIn() && !inFeature.isOut()) {
        org.sireum.Option.some[ir.Direction.Type](ir.Direction.In)
      } else {
        org.sireum.Option.some[ir.Direction.Type](ir.Direction.Out)
      }
    }
    if (connElem.getConnectionEnd.isInstanceOf[FeatureGroupImpl]) {
      val fgce = connElem.getConnectionEnd.asInstanceOf[FeatureGroupImpl]
      result = result ++ flattenFeatureGroup(component, String(fgce.getFullName), fgce, connElem)
    } else if (connElem.getConnectionEnd.isInstanceOf[BusSubcomponentImpl]) {
      result = result :+ (ir.EndPoint(
        ir.Name(feature), None(),
        org.sireum.Option.some[ir.Direction.Type](ir.Direction.InOut)))
    } else if (connElem.getConnectionEnd.isInstanceOf[BusAccessImpl]) {
      result = result :+ (ir.EndPoint(
        ir.Name(component),
        Some(ir.Name(feature)), org.sireum.Option.some[ir.Direction.Type](ir.Direction.InOut)))
    } else {
      result = result :+ (ir.EndPoint(
        ir.Name(component),
        Some(ir.Name(feature)), dir))
    }
    result
  }

  private def flattenFeatureGroup(
    component: ISZ[String],
    parentName: String,
    fgi: FeatureGroupImpl,
    connElem: ConnectedElement): ISZ[ir.EndPoint] = {
    var res = ISZ[ir.EndPoint]()
    var fgt = fgi.getFeatureGroupType
    if (fgt == null) {
      val fgpt = fgi.getFeatureGroupPrototype
      fgt = ResolvePrototypeUtil.resolveFeatureGroupPrototype(
        fgpt,
        if (connElem.getContext == null) connElem.getContainingComponentImpl else connElem.getContext);
    }
    fgt.getAllFeatures.toSeq.foreach { f =>
      var rf = f.getRefined
      if (rf == null) { rf = f }
      if (rf.isInstanceOf[FeatureGroupImpl]) {

        res = res ++ flattenFeatureGroup(component, String(parentName.value + "_" + rf.getFullName), rf.asInstanceOf[FeatureGroupImpl], connElem)
      } else {
        res = res ++ ISZ(ir.EndPoint(
          ir.Name(component),
          Some(ir.Name(component :+ String(parentName.value + "_" + rf.getFullName))),
          org.sireum.Option.some[ir.Direction.Type](if (AadlUtil.isIncomingFeature(rf) && AadlUtil.isOutgoingFeature(rf)) {
            ir.Direction.InOut
          } else if (AadlUtil.isIncomingFeature(rf)) {
            if (fgi.isInverse()) ir.Direction.Out else ir.Direction.In
          } else {
            if (fgi.isInverse()) ir.Direction.In else ir.Direction.Out
          })))

      }
    }
    res
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
      val res = compConnMap.get(currentPath).get.elements.flatMap { c =>
        val xx = c
        buildConnection(c, currentPath, compInst)
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

  /*  private def buildFeatureGroup(featureGroup : FeatureGroup, path : ISZ[String]): ir.Feature = {
    val currentPath = path :+ featureGroup.getName
    ir.FeatureGroup(ir.Name(currentPath), featureGroup.get )
  }
  */

  private def buildFeature(featureInst: FeatureInstance, path: ISZ[String]): ir.Feature = {
    val f = featureInst.getFeature
    val currentPath = path :+ featureInst.getName
    val featureInstances = featureInst.getFeatureInstances
    val classifier = if (f.getFeatureClassifier != null) {
      val name =
        if (f.getFeatureClassifier.isInstanceOf[NamedElement]) {
          f.getFeatureClassifier.asInstanceOf[NamedElement].getQualifiedName
        } else {
          throw new Exception(s"Unexpected classsifier ${f.getFeatureClassifier} for feature $currentPath")
        }
      Some(ir.Classifier(name))
    } else {
      None[ir.Classifier]
    }
    var properties = ISZ[ir.Property](featureInst.getOwnedPropertyAssociations.map(op =>
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
    if(typ == ir.FeatureCategory.SubprogramAccess){
      val sai = f.asInstanceOf[SubprogramAccessImpl]
      val kind = sai.getKind.getName
      
      properties = properties :+ ir.Property(name = ir.Name(path :+ "AccessType"), 
          propertyValues = ISZ(ir.ValueProp(value = kind)))
    }
    
    if (featureInstances.isEmpty()) {
      return ir.FeatureEnd(
        identifier = ir.Name(currentPath),
        classifier = classifier,
        direction = handleDirection(featureInst.getDirection),
        category = typ,
        properties = properties)
    } else {
      return ir.FeatureGroup(
        identifier = ir.Name(currentPath),
        features = ISZ(featureInstances.map(fi => buildFeature(fi, currentPath)): _*),
        f.asInstanceOf[FeatureGroupImpl].isInverse(),
        typ,
        //classifier,
        properties)
    }
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
      if (s != null) {
        val us = ISZ(s.getInstanceObjectPath.split('.').map(String(_)).toSeq: _*) -- path
        //Some(us.elements.map(_.value).mkString("_"))
        Some(buildFeature(s, currentPath))
      } else None[ir.Feature],
      if (d != null) {
        val ud = ISZ(d.getInstanceObjectPath.split('.').map(String(_)).toSeq: _*) -- (path)
        //Some(ud.elements.map(_.value).mkString("_"))
        Some(buildFeature(d, currentPath))
      } else None[ir.Feature])

  }

  private def getPropertyExpressionValue(pe: PropertyExpression, path: ISZ[String]): ISZ[ir.PropertyValue] = {
    def getUnitProp(nv: NumberValue): ir.UnitProp = {
      if (nv == null) ir.UnitProp(value = "??", unit = None[String]())
      else {
        val v: Double = org.osate.aadl2.operations.NumberValueOperations.getScaledValue(nv)
        val u: UnitLiteral = org.osate.aadl2.operations.UnitLiteralOperations.getAbsoluteUnit(nv.getUnit)
        ir.UnitProp(value = v.toString, unit = if (u == null) None[String]() else Some(u.getName))
      }
    }

    //val eName = pe.eClass().getName
    pe.eClass().getClassifierID match {
      case Aadl2Package.BOOLEAN_LITERAL =>
        val b = pe.asInstanceOf[BooleanLiteral].getValue.toString
        return ISZ(ir.ValueProp(b))
      case Aadl2Package.INTEGER_LITERAL | Aadl2Package.REAL_LITERAL =>
        return ISZ(getUnitProp(pe.asInstanceOf[NumberValue]))
      case Aadl2Package.STRING_LITERAL =>
        val v = pe.asInstanceOf[StringLiteral].getValue
        return ISZ(ir.ValueProp(v))
      case Aadl2Package.RANGE_VALUE =>
        val rv = pe.asInstanceOf[RangeValue]
        return ISZ(ir.RangeProp(
          low = getUnitProp(rv.getMinimumValue),
          high = getUnitProp(rv.getMaximumValue)))
      case Aadl2Package.CLASSIFIER_VALUE =>
        val cv = pe.asInstanceOf[ClassifierValue].getClassifier
        return ISZ(ir.ClassifierProp(cv.getQualifiedName))
      case Aadl2Package.LIST_VALUE =>
        val lv = pe.asInstanceOf[ListValue]
        var elems = ISZ[ir.PropertyValue]()
        for (e <- lv.getOwnedListElements)
          elems ++= getPropertyExpressionValue(e, path)
        return elems
      case Aadl2Package.NAMED_VALUE =>
        val nv = pe.asInstanceOf[NamedValue]
        val nv2 = nv.getNamedValue
        nv2.eClass.getClassifierID match {
          case Aadl2Package.ENUMERATION_LITERAL =>
            val el = nv2.asInstanceOf[EnumerationLiteral]
            return ISZ(ir.ValueProp(el.getFullName))
          case Aadl2Package.PROPERTY =>
            val _p = nv2.asInstanceOf[Property]
            if (_p.getDefaultValue != null)
              return getPropertyExpressionValue(_p.getDefaultValue, path)
            else
              return ISZ(ir.ValueProp(_p.getQualifiedName))
          case Aadl2Package.PROPERTY_CONSTANT =>
            val pc = nv2.asInstanceOf[PropertyConstant]
            return ISZ(ir.ValueProp(pc.getConstantValue.toString()))
          case xf =>
            println(s"Not handling $xf $nv2")
            return ISZ()
        }
      case Aadl2Package.RECORD_VALUE =>
        val rv = pe.asInstanceOf[RecordValue]
        return ISZ(ir.RecordProp(
          ISZ[ir.Property](
            rv.getOwnedFieldValues.map(fv => ir.Property(
              name = ir.Name(path :+ fv.getProperty.getQualifiedName),
              propertyValues = getPropertyExpressionValue(fv.getOwnedValue, path))).toSeq: _*)))
      case Aadl2Package.REFERENCE_VALUE =>
        val rv = pe.asInstanceOf[ReferenceValue]
        return ISZ(ir.ReferenceProp(ir.Name(ISZ(rv.toString))))
      case InstancePackage.INSTANCE_REFERENCE_VALUE =>
        // FIXME: id is coming from InstancePackage rather than Aadl2Package.  Might cause the
        // following cast to fail if there is an id clash
        val rv = pe.asInstanceOf[InstanceReferenceValue]
        val t = rv.getReferencedInstanceObject().getInstanceObjectPath()
        val res = ISZ(t.split('.').map(String(_)).toSeq: _*) //.map(String(_)).toSeq: _*)
        return ISZ(ir.ReferenceProp(ir.Name(res)))
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
        getPropertyExpressionValue(pe, path)
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