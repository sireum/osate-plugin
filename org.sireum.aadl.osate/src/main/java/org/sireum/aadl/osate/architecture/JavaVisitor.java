package org.sireum.aadl.osate.architecture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osate.aadl2.Aadl2Package;
import org.osate.aadl2.AbstractNamedValue;
import org.osate.aadl2.AccessType;
import org.osate.aadl2.BooleanLiteral;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.ClassifierValue;
import org.osate.aadl2.Connection;
import org.osate.aadl2.DataClassifier;
import org.osate.aadl2.DataImplementation;
import org.osate.aadl2.DataSubcomponent;
import org.osate.aadl2.DirectionType;
import org.osate.aadl2.Element;
import org.osate.aadl2.EnumerationLiteral;
import org.osate.aadl2.Feature;
import org.osate.aadl2.ListValue;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.NamedValue;
import org.osate.aadl2.NumberValue;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PropertyConstant;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.RangeValue;
import org.osate.aadl2.RecordValue;
import org.osate.aadl2.ReferenceValue;
import org.osate.aadl2.StringLiteral;
import org.osate.aadl2.UnitLiteral;
import org.osate.aadl2.impl.AccessImpl;
import org.osate.aadl2.impl.DataImplementationImpl;
import org.osate.aadl2.impl.DataPortImpl;
import org.osate.aadl2.impl.DataTypeImpl;
import org.osate.aadl2.impl.DirectedFeatureImpl;
import org.osate.aadl2.impl.EventDataPortImpl;
import org.osate.aadl2.impl.FeatureGroupImpl;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.FeatureInstance;
import org.osate.aadl2.instance.FlowSpecificationInstance;
import org.osate.aadl2.instance.InstancePackage;
import org.osate.aadl2.instance.InstanceReferenceValue;
import org.osate.aadl2.instance.impl.FeatureInstanceImpl;
import org.osate.aadl2.instance.util.InstanceUtil;
import org.osate.xtext.aadl2.properties.util.PropertyUtils;
import org.sireum.Option;
import org.sireum.Some;
import org.sireum.aadl.ir.AadlASTJavaFactory;

public class JavaVisitor {

	org.sireum.aadl.ir.AadlASTFactory factory = new org.sireum.aadl.ir.AadlASTFactory();

	Map<String, org.sireum.aadl.ir.Component> datamap = new LinkedHashMap<String, org.sireum.aadl.ir.Component>();

	public Option<org.sireum.aadl.ir.Aadl> convert(Element root, boolean includeDataComponents) {
		Option<org.sireum.aadl.ir.Component> t = visit(root);
		if(t.nonEmpty()) {
			List<org.sireum.aadl.ir.Component> dataComponents =
					includeDataComponents ? new ArrayList<>(datamap.values()) : ilist();
			return new Some<org.sireum.aadl.ir.Aadl>(
					factory.aadl(toList(t.get()),
							ilist(), //errorLib // FIXME
							dataComponents));
		} else {
			return org.sireum.None.apply();
		}
	}

	private Option<org.sireum.aadl.ir.Component> visit(Element root) {
		switch (root.eClass().getClassifierID()) {
		case InstancePackage.SYSTEM_INSTANCE:
		case InstancePackage.COMPONENT_INSTANCE: {
			org.sireum.aadl.ir.Component c = buildComponent((ComponentInstance) root, mlist());
			return new Some<org.sireum.aadl.ir.Component>(c);
		}
		default:
			return org.sireum.None.apply();
		}
	}

	private org.sireum.aadl.ir.Component buildComponent(ComponentInstance compInst, List<String> path) {
		List<String> currentPath = add(path, compInst.getName());

		List<org.sireum.aadl.ir.Feature> features = compInst.getFeatureInstances().stream()
				.map(fi -> buildFeature(fi, currentPath)).collect(Collectors.toList());

		List<org.sireum.aadl.ir.ConnectionInstance> connectionInstances = compInst.getConnectionInstances().stream()
				.map(ci -> buildConnectionInst(ci, currentPath)).collect(Collectors.toList());

		List<org.sireum.aadl.ir.Property> properties = compInst.getOwnedPropertyAssociations().stream()
				.map(pa -> buildProperty(pa, currentPath)).collect(Collectors.toList());

		List<org.sireum.aadl.ir.Flow> flows = compInst.getFlowSpecifications().stream()
				.map(fs -> buildFlow(fs, currentPath)).collect(Collectors.toList());

		List<org.sireum.aadl.ir.Component> subComponents = compInst.getComponentInstances().stream()
				.map(ci -> buildComponent(ci, currentPath)).collect(Collectors.toList());

		AadlASTJavaFactory.ComponentCategory category = null;

		switch(compInst.getCategory()) {
		case ABSTRACT: category = AadlASTJavaFactory.ComponentCategory.Abstract; break;
		case BUS: category = AadlASTJavaFactory.ComponentCategory.Bus; break;
		case DATA: category = AadlASTJavaFactory.ComponentCategory.Data; break;
		case DEVICE: category = AadlASTJavaFactory.ComponentCategory.Device; break;
		case MEMORY: category = AadlASTJavaFactory.ComponentCategory.Memory; break;
		case PROCESS: category = AadlASTJavaFactory.ComponentCategory.Process; break;
		case PROCESSOR: category = AadlASTJavaFactory.ComponentCategory.Processor; break;
		case SUBPROGRAM: category = AadlASTJavaFactory.ComponentCategory.Subprogram; break;
		case SUBPROGRAM_GROUP: category = AadlASTJavaFactory.ComponentCategory.SubprogramGroup; break;
		case SYSTEM: category = AadlASTJavaFactory.ComponentCategory.System; break;
		case THREAD: category = AadlASTJavaFactory.ComponentCategory.Thread; break;
		case THREAD_GROUP: category = AadlASTJavaFactory.ComponentCategory.ThreadGroup; break;
		case VIRTUAL_BUS: category = AadlASTJavaFactory.ComponentCategory.VirtualBus; break;
		case VIRTUAL_PROCESSOR: category = AadlASTJavaFactory.ComponentCategory.VirtualProcessor; break;
		default: throw new RuntimeException("Unexpected");
		}

		org.sireum.aadl.ir.Name identifier = factory.name(currentPath);

		org.sireum.aadl.ir.Classifier classifier = compInst.getClassifier() != null ?
				factory.classifier(compInst.getClassifier().getQualifiedName()) : null;

		List<org.sireum.aadl.ir.Mode> modes = ilist(); // TODO

		List<org.sireum.aadl.ir.Annex> annexes = ilist(); // FIXME

		List<org.sireum.aadl.ir.Connection> connections = ilist(); // FIXME

		return factory.component(
				identifier, category, classifier, features, subComponents,
				connections, connectionInstances, properties, flows, modes, annexes);
	}

	private org.sireum.aadl.ir.Feature buildFeature(FeatureInstance featureInst, List<String> path) {

		Feature f = featureInst.getFeature();

		List<String> currentPath = add(path,featureInst.getName());

		org.sireum.aadl.ir.Classifier classifier = null;
		if(f.getFeatureClassifier() != null) {
			if(f.getFeatureClassifier() instanceof NamedElement) {
				classifier = factory.classifier(((NamedElement)f.getFeatureClassifier()).getQualifiedName());
			} else {
				throw new RuntimeException("Unexepcted classifier " + f.getFeatureClassifier() +
						" for feature " + featureInst.getQualifiedName());
			}
		}

		List<org.sireum.aadl.ir.Property> properties = featureInst.getOwnedPropertyAssociations().stream()
				.map(pa -> buildProperty(pa, currentPath)).collect(Collectors.toList());

		AadlASTJavaFactory.FeatureCategory category = null;
		switch(featureInst.getCategory()) {
		case ABSTRACT_FEATURE: category = AadlASTJavaFactory.FeatureCategory.AbstractFeature; break;
		case BUS_ACCESS: category = AadlASTJavaFactory.FeatureCategory.BusAccess; break;
		case DATA_ACCESS: category = AadlASTJavaFactory.FeatureCategory.DataAccess; break;
		case DATA_PORT: category = AadlASTJavaFactory.FeatureCategory.DataPort; break;
		case EVENT_PORT: category = AadlASTJavaFactory.FeatureCategory.EventPort; break;
		case EVENT_DATA_PORT: category = AadlASTJavaFactory.FeatureCategory.EventDataPort; break;
		case FEATURE_GROUP: category = AadlASTJavaFactory.FeatureCategory.FeatureGroup; break;
		case PARAMETER:  category = AadlASTJavaFactory.FeatureCategory.Parameter; break;
		case SUBPROGRAM_ACCESS: category = AadlASTJavaFactory.FeatureCategory.SubprogramAccess; break;
		case SUBPROGRAM_GROUP_ACCESS: category = AadlASTJavaFactory.FeatureCategory.SubprogramAccessGroup; break;
		default: throw new RuntimeException("Unexpected category: " + featureInst.getCategory());
		}

		org.sireum.aadl.ir.Name identifier = factory.name(currentPath);

		if((f instanceof DataPortImpl || f instanceof EventDataPortImpl) &&
				(f.getClassifier() instanceof DataTypeImpl || f.getClassifier() instanceof DataImplementationImpl)) {
			processDataType((DataClassifier) f.getClassifier());
		}

		List<FeatureInstance> featureInstances = featureInst.getFeatureInstances();
		if(featureInstances.isEmpty()) {
			if(f instanceof AccessImpl) {
				AccessImpl accessImpl = (AccessImpl) f;
				AadlASTJavaFactory.AccessType accessType = accessImpl.getKind() == AccessType.PROVIDES
						? AadlASTJavaFactory.AccessType.Provides
						: AadlASTJavaFactory.AccessType.Requires;
				AadlASTJavaFactory.AccessCategory accessCategory = null;
				switch (accessImpl.getCategory()) {
				case BUS: accessCategory = AadlASTJavaFactory.AccessCategory.Bus; break;
				case DATA: accessCategory = AadlASTJavaFactory.AccessCategory.Data;	break;
				case SUBPROGRAM: accessCategory = AadlASTJavaFactory.AccessCategory.Subprogram; break;
				case SUBPROGRAM_GROUP: accessCategory = AadlASTJavaFactory.AccessCategory.SubprogramGroup; break;
				case VIRTUAL_BUS: accessCategory = AadlASTJavaFactory.AccessCategory.VirtualBus; break;
				}
				return factory.featureAccess(identifier, category, classifier, accessType, accessCategory, properties);
			} else if (f instanceof DirectedFeatureImpl) {
				AadlASTJavaFactory.Direction direction = handleDirection(featureInst.getDirection());
				return factory.featureEnd(identifier, direction, category, classifier, properties);
			} else {
				throw new RuntimeException("Not expecting feature: " + featureInst);
			}
		} else {
			boolean isInverse = ((FeatureGroupImpl) f).isInverse();
			List<org.sireum.aadl.ir.Feature> features = featureInstances.stream()
					.map(fi -> buildFeature(fi, currentPath)).collect(Collectors.toList());
			return factory.featureGroup(identifier, features, isInverse, category, properties);
		}
	}

	private org.sireum.aadl.ir.ConnectionInstance buildConnectionInst(ConnectionInstance connInst, List<String> path) {
		List<String> currentPath = add(path, connInst.getName());

		List<String> srcComponent = Arrays.asList(connInst.getSource().getComponentInstance().getInstanceObjectPath().split("\\."));
		List<String> srcFeature = Arrays.asList(connInst.getSource().getInstanceObjectPath().split("\\."));
		AadlASTJavaFactory.Direction srcDirection =
				connInst.getSource() instanceof FeatureInstanceImpl ?
						handleDirection(((FeatureInstanceImpl) connInst.getSource()).getDirection()) :
							null;

		List<String> dstComponent = Arrays.asList(connInst.getDestination().getComponentInstance().getInstanceObjectPath().split("\\."));
		List<String> dstFeature = Arrays.asList(connInst.getDestination().getInstanceObjectPath().split("\\."));
		AadlASTJavaFactory.Direction dstDirection =
				connInst.getDestination() instanceof FeatureInstanceImpl ?
						handleDirection(((FeatureInstanceImpl) connInst.getDestination()).getDirection()) :
							null;

    	org.sireum.aadl.ir.Name name = factory.name(currentPath);

		List<org.sireum.aadl.ir.Property> properties = connInst.getOwnedPropertyAssociations().stream()
				.map(pa -> buildProperty(pa, currentPath)).collect(Collectors.toList());

		AadlASTJavaFactory.ConnectionKind kind = null;
		switch(connInst.getKind()) {
		case ACCESS_CONNECTION: kind = AadlASTJavaFactory.ConnectionKind.Access; break;
		case FEATURE_CONNECTION: kind = AadlASTJavaFactory.ConnectionKind.Feature; break;
		case FEATURE_GROUP_CONNECTION: kind = AadlASTJavaFactory.ConnectionKind.FeatureGroup; break;
		case MODE_TRANSITION_CONNECTION: kind = AadlASTJavaFactory.ConnectionKind.ModeTransition; break;
		case PARAMETER_CONNECTION: kind = AadlASTJavaFactory.ConnectionKind.Parameter; break;
		case PORT_CONNECTION: kind = AadlASTJavaFactory.ConnectionKind.Port; break;
		}

		Connection conn = InstanceUtil.getCrossConnection(connInst);
		List<org.sireum.aadl.ir.ConnectionReference> connectionRefs = ilist(); // FIXME:

		org.sireum.aadl.ir.EndPoint src = factory.endPoint(
				factory.name(srcComponent), factory.name(srcFeature), srcDirection);

		org.sireum.aadl.ir.EndPoint dst = factory.endPoint(
				factory.name(dstComponent), factory.name(dstFeature), dstDirection);

    	return factory.connectionInstance(name, src, dst, kind, connectionRefs, properties);
	}

	private org.sireum.aadl.ir.Flow buildFlow(FlowSpecificationInstance flowInst, List<String> path) {

		List<String> currentPath = add(path, flowInst.getQualifiedName());
		org.sireum.aadl.ir.Name name = factory.name(currentPath);

		AadlASTJavaFactory.FlowKind kind = null;
		switch(flowInst.getFlowSpecification().getKind()) {
		case SOURCE: kind = AadlASTJavaFactory.FlowKind.Source; break;
		case SINK: kind = AadlASTJavaFactory.FlowKind.Sink; break;
		case PATH: kind = AadlASTJavaFactory.FlowKind.Path; break;
		}

		org.sireum.aadl.ir.Feature source = null;
		if(flowInst.getSource() != null) {
			//List<String> us = Arrays.asList(flowInst.getSource().getInstanceObjectPath().split("\\."));
			source = buildFeature(flowInst.getSource(), currentPath);
		}

		org.sireum.aadl.ir.Feature sink = null;
		if(flowInst.getDestination() != null) {
			//List<String> ud = Arrays.asList(flowInst.getDestination().getInstanceObjectPath().split("\\."));
			sink = buildFeature(flowInst.getDestination(), currentPath);
		}

		return factory.flow(name, kind, source, sink);
	}

	private org.sireum.aadl.ir.Property buildProperty(PropertyAssociation pa, List<String> path) {
		Property prop = pa.getProperty();
		List<String> currentPath = add(path, prop.getQualifiedName());
		NamedElement cont = (NamedElement) pa.eContainer();

		List<org.sireum.aadl.ir.PropertyValue> propertyValues = mlist();

		try {
			PropertyExpression pe = PropertyUtils.getSimplePropertyValue(cont, prop);
			propertyValues = getPropertyExpressionValue(pe, path);
		} catch (Throwable t) {
			java.lang.System.err.println("Error encountered while trying to fetch property value for " +
		      prop.getQualifiedName() + " from " + cont.getQualifiedName() + " : " + t.getMessage());
		}

		return factory.property(factory.name(currentPath), propertyValues);
	}

	private org.sireum.aadl.ir.UnitProp getUnitProp (NumberValue nv) {
		if(nv == null) {
			return factory.unitProp("??", null);
		} else {
			double v = org.osate.aadl2.operations.NumberValueOperations.getScaledValue(nv);
			UnitLiteral u = org.osate.aadl2.operations.UnitLiteralOperations.getAbsoluteUnit(nv.getUnit());
			return factory.unitProp(Double.toString(v), u == null ? null : u.getName());
		}
	}

	private List<org.sireum.aadl.ir.PropertyValue> getPropertyExpressionValue(PropertyExpression pe, List<String> path) {

		switch(pe.eClass().getClassifierID()) {
		case Aadl2Package.BOOLEAN_LITERAL:
			String b = Boolean.toString(((BooleanLiteral) pe).getValue());
			return toList(factory.valueProp(b));
		case Aadl2Package.INTEGER_LITERAL:
		case Aadl2Package.REAL_LITERAL:
			return toList(getUnitProp((NumberValue) pe));
		 case Aadl2Package.STRING_LITERAL:
			 String v = ((StringLiteral) pe).getValue();
			 return toList(factory.valueProp(v));
		 case Aadl2Package.RANGE_VALUE:
			 RangeValue rv = (RangeValue) pe;
			 return toList(factory.rangeProp(
					 getUnitProp(rv.getMinimumValue()),
					 getUnitProp(rv.getMaximumValue())));
         case Aadl2Package.CLASSIFIER_VALUE:
        	 Classifier cv = ((ClassifierValue) pe).getClassifier();
        	 return toList(factory.classifierProp(cv.getQualifiedName()));
         case Aadl2Package.LIST_VALUE:
        	 ListValue lv = (ListValue) pe;
        	 List<org.sireum.aadl.ir.PropertyValue> elems = mlist();
        	 for(PropertyExpression e : lv.getOwnedListElements()) {
				elems.addAll(getPropertyExpressionValue(e, path));
			}
        	 return elems;
         case Aadl2Package.NAMED_VALUE:
        	 NamedValue nv = (NamedValue) pe;
        	 AbstractNamedValue nv2 = nv.getNamedValue();

        	 switch(nv2.eClass().getClassifierID()) {
        	 case Aadl2Package.ENUMERATION_LITERAL:
        		 EnumerationLiteral el = (EnumerationLiteral) nv2;
        		 return toList(factory.valueProp(el.getFullName()));
        	 case Aadl2Package.PROPERTY:
        		 Property _p = (Property) nv2;
        		 if(_p.getDefaultValue() != null) {
        			 return getPropertyExpressionValue(_p.getDefaultValue(), path);
        		 } else {
        			 return toList(factory.valueProp(_p.getQualifiedName()));
        		 }
        	 case Aadl2Package.PROPERTY_CONSTANT:
        		 PropertyConstant pc = (PropertyConstant) nv2;
        		 return toList(factory.valueProp(pc.getConstantValue().toString()));
        	 default:
        		 java.lang.System.err.println("Not handling " + pe.eClass().getClassifierID() + " " + nv2);
        		 return ilist();
        	 }
         case Aadl2Package.RECORD_VALUE:
        	 RecordValue rvy = (RecordValue) pe;
        	 List<org.sireum.aadl.ir.Property> properties = rvy.getOwnedFieldValues().stream().map(fv ->
        	     factory.property(
        	    		 factory.name(add(path, fv.getProperty().getQualifiedName())),
        	    		 getPropertyExpressionValue(fv.getOwnedValue(), path))
        	 ).collect(Collectors.toList());
        	 return toList(factory.recordProp(properties));
         case Aadl2Package.REFERENCE_VALUE:
        	 ReferenceValue rvx = (ReferenceValue) pe;
        	 org.sireum.aadl.ir.Name refName = factory.name(toList(rvx.toString()));
        	 return toList(factory.referenceProp(refName));
         case InstancePackage.INSTANCE_REFERENCE_VALUE:
             // FIXME: id is coming from InstancePackage rather than Aadl2Package.  Might cause the
             // following cast to fail if there is an id clash

        	 InstanceReferenceValue irv = (InstanceReferenceValue) pe;
        	 String t = irv.getReferencedInstanceObject().getInstanceObjectPath();

        	 return toList(factory.referenceProp(factory.name(Arrays.asList(t.split("\\.")))));
         default:
        	 java.lang.System.err.println("Need to handle " + pe + " " + pe.eClass().getClassifierID());
        	 return toList(factory.classifierProp(pe.getClass().getName()));
		}
	}

	private org.sireum.aadl.ir.Component processDataType(DataClassifier f) {
		String name = f.getQualifiedName();
		if(datamap.containsKey(name)) {
			return datamap.get(name);
		}

		List<org.sireum.aadl.ir.Property> properties = f.getOwnedPropertyAssociations().stream().map(op ->
		  buildProperty(op, mlist())).collect(Collectors.toList());

		List<org.sireum.aadl.ir.Component> subComponents = mlist();
		if(f instanceof DataTypeImpl) {
			// do nothing as component types can't have subcomponents
		} else if(f instanceof DataImplementation) {
			DataImplementation di = (DataImplementation) f;
			List<org.sireum.aadl.ir.Property> subProps = di.getType().getOwnedPropertyAssociations().stream().map(op ->
    			buildProperty(op, mlist())).collect(Collectors.toList());
			properties.addAll(subProps);

			for(DataSubcomponent dsc : di.getOwnedDataSubcomponents()) {
				DataClassifier sct = (DataClassifier) dsc.getDataSubcomponentType();
				org.sireum.aadl.ir.Component c = processDataType(sct);
				List<org.sireum.aadl.ir.Property> fProperties = dsc.getOwnedPropertyAssociations().stream().map(op ->
				  buildProperty(op, mlist())).collect(Collectors.toList());

				List<org.sireum.aadl.ir.Property> cProps = isz2List(c.properties());
				cProps.addAll(fProperties);

				AadlASTJavaFactory.ComponentCategory category =	AadlASTJavaFactory.ComponentCategory.valueOf(c.category().name());

				org.sireum.aadl.ir.Classifier classifier = c.classifier().nonEmpty() ? c.classifier().get() : null;

				org.sireum.aadl.ir.Component subby =
					factory.component(factory.name(toList(f.getName())),
						category, classifier,
						isz2List(c.features()), isz2List(c.subComponents()),
						isz2List(c.connections()), isz2List(c.connectionInstances()),
						cProps,
						isz2List(c.flows()), isz2List(c.modes()), isz2List(c.annexes()));

				subComponents.add(subby);
			}
		} else {
			throw new RuntimeException("Unexpected data type: " + f);
		}

		org.sireum.aadl.ir.Component c = factory.component(
				factory.name(ilist()), //identifier
				AadlASTJavaFactory.ComponentCategory.Data, // category
				factory.classifier(name),
				ilist(), // features
				subComponents,
				ilist(), // connections
				ilist(), // connectionInstances
				properties,
				ilist(), //flows
				ilist(), // modes
				ilist() // annexes
				);
		datamap.put(name, c);
		return c;
	}

	private AadlASTJavaFactory.Direction handleDirection(DirectionType d) {
		AadlASTJavaFactory.Direction direction = null;
		switch(d) {
		case IN: direction = AadlASTJavaFactory.Direction.In; break;
		case OUT: direction = AadlASTJavaFactory.Direction.Out; break;
		case IN_OUT: direction = AadlASTJavaFactory.Direction.InOut; break;
		}
		return direction;
	}

	private <T> List<T> isz2List(org.sireum.IS<org.sireum.Z, T> isz) {
		return scala.collection.JavaConverters.seqAsJavaList(isz.elements());
	}

	private <T> List<T> toList(T e) {
	  List<T> ret = new ArrayList<T>();
	  ret.add(e);
	  return Collections.unmodifiableList(ret);
	}

	private <T> List<T> add(List<T> l, T e) {
		List<T> ret = new ArrayList<T>(l);
		ret.add(e);
		return Collections.unmodifiableList(ret);
	}

	private <T> List<T> ilist() {
		return Collections.emptyList();
	}

	private <T> List<T> mlist() {
		return new ArrayList<T>();
	}
}