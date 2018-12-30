package org.sireum.aadl.osate.architecture

import java.util.List

import org.osate.aadl2._
import org.osate.aadl2.impl._
import org.osate.aadl2.instance._
import scala.collection.JavaConversions._

import org.osate.xtext.aadl2.properties.util.AadlProject;
import org.osate.xtext.aadl2.properties.util.GetProperties;
import org.osate.xtext.aadl2.properties.util.PropertyUtils;
import org.osate.xtext.aadl2.properties.util.TimingProperties;

import scala.collection.mutable.ListBuffer

trait Report {
  def component: NamedElement
  def message : String
}

case class WarningReport(component: NamedElement,
                         message: String) extends Report {} 
    
case class ErrorReport(component: NamedElement,
                       message: String) extends Report {}

object Check {

  def check(root: ComponentInstance): List[Report] = {

    val reports = ListBuffer[Report]()
    
    val allThreads = root.getAllComponentInstances.filter(_.getCategory == ComponentCategory.THREAD)
    val allDevices = root.getAllComponentInstances.filter(_.getCategory == ComponentCategory.DEVICE)
    val allConnections = root.getAllComponentInstances.flatMap(_.getConnectionInstances)
      
    reports ++=
      allThreads.filter({ c =>
        val protocol = GetProperties.getDispatchProtocol(c)
        protocol == null ||
          !(protocol.getName.equalsIgnoreCase(AadlProject.PERIODIC_LITERAL) || protocol.getName.equalsIgnoreCase(AadlProject.SPORADIC_LITERAL))
      }).map(ErrorReport(_, "Thread must be periodic or sporadic"))

    /*
    // Threads or devices with 'in event' ports must be sporadic"
    reports ++= 
      (allThreads ++ allDevices).filter(c => {
        val protocol = GetProperties.getDispatchProtocol(c)
        c.getFeatureInstances.exists(f =>
          (protocol == null || protocol.getName.equalsIgnoreCase(AadlProject.PERIODIC_LITERAL)) &&
          (f.getDirection == DirectionType.IN || f.getDirection == DirectionType.IN_OUT) &&
          (f.getCategory == FeatureCategory.EVENT_DATA_PORT || f.getCategory == FeatureCategory.EVENT_PORT))
      }).map(ErrorReport(_, "Threads or devices with 'in event' ports must be sporadic"))
    */
      
    // FIXME: how to determine inherited properties
    reports ++= allThreads.filter(GetProperties.lookupPropertyDefinition(_, TimingProperties._NAME, TimingProperties.PERIOD) == null)
      .map(ErrorReport(_, "Thread must define the property Timing_Properties::Period"))

    reports ++= allConnections.filter({ c =>
      def getBaseType(f: Feature) : Option[String] = {
          return if(f.getFeatureClassifier != null && f.getFeatureClassifier.isInstanceOf[DataTypeImpl])
            Some(f.getFeatureClassifier.asInstanceOf[DataTypeImpl].getQualifiedName)
          else None
      }

      (c.getSource, c.getDestination) match {
        case (s:FeatureInstance, d:FeatureInstance) =>
          getBaseType(s.getFeature) != getBaseType(d.getFeature)
        case _ => {
          //println(s"${c.getName} connects ${c.getSource.getClass.getSimpleName} to ${c.getDestination.getClass.getSimpleName}")
          false
        }
      }
    }).map(ErrorReport(_, "Data types for ports must match"))
    
    //errorReports ++= allConnections.filter({ c => c.getSource.getComponentInstance == c.getDestination.getComponentInstance})
    //.map(ErrorReport(_, "Source and destination ports identical"))
    
    /* emit warning if connected port category types are not identical
     *   event_port -> event_port
     *   event_data_port -> event_data_port
     *   data_port -> data_port
     */
    reports ++= allConnections.filter({c => 
      (c.getSource, c.getDestination) match {
        case (s:FeatureInstance, d:FeatureInstance) =>
          //println(s"${s.getCategory} -> ${d.getCategory}   ${s.getCategory != d.getCategory}    ${c.getName}   ")
          s.getCategory != d.getCategory
        case _ => {
          //println(s"${c.getName} connects ${c.getSource.getClass.getSimpleName} to ${c.getDestination.getClass.getSimpleName}")
          false
        }
      }      
    }).map(WarningReport(_, "Connected ports should have the same feature category"))
    
    reports ++= root.getAllComponentInstances.flatMap(_.getAllFeatureInstances)
    .filter(p => (p.getCategory == FeatureCategory.DATA_PORT || 
        p.getCategory == FeatureCategory.EVENT_DATA_PORT) && p.getFeature.getFeatureClassifier == null)
        .map(ErrorReport(_, "Data and Event Data ports must be typed"))
    
    return reports
  }
}