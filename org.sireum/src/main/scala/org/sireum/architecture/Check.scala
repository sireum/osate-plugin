package org.sireum.architecture

import java.util.List

import org.osate.aadl2._
import org.osate.aadl2.impl._
import org.osate.aadl2.instance._
import org.sireum.aadl.ast
import scala.collection.JavaConversions._

import org.osate.xtext.aadl2.properties.util.AadlProject;
import org.osate.xtext.aadl2.properties.util.GetProperties;
import org.osate.xtext.aadl2.properties.util.PropertyUtils;
import org.osate.xtext.aadl2.properties.util.TimingProperties;

import scala.collection.mutable.ListBuffer

case class ErrorReport(component: NamedElement,
                       message: String)

object Check {

  def check(root: ComponentInstance): List[ErrorReport] = {

    val errorReports = ListBuffer[ErrorReport]()

    val allThreads = root.getAllComponentInstances.filter(_.getCategory == ComponentCategory.THREAD)

    errorReports ++=
      allThreads.filter({ c =>
        val protocol = GetProperties.getDispatchProtocol(c)
        protocol == null ||
          !(protocol.toString.equalsIgnoreCase(AadlProject.PERIODIC_LITERAL)
            || protocol.toString.equalsIgnoreCase(AadlProject.SPORADIC_LITERAL))
      }).map(ErrorReport(_, "Thread needs a Thread_Properties::Dispatch_Protocol property of 'Periodic' or 'Sporadic'"))

    // FIXME: how to determine inherited properties
    errorReports ++= allThreads.filter(GetProperties.getPeriodinMS(_) == 0.0)
      .map(ErrorReport(_, "Thread must define the property Timing_Properties::Period"))

    errorReports ++= root.getAllComponentInstances.flatMap(_.getAllFeatureInstances)
    .filter(p => (p.getCategory == FeatureCategory.DATA_PORT || 
        p.getCategory == FeatureCategory.EVENT_DATA_PORT) && p.getFeature.getFeatureClassifier == null)
        .map(ErrorReport(_, "Data and Event Data ports must be typed"))
    
    return errorReports
  }
}