package org.sireum.aadl.osate.architecture;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.AbstractNamedValue;
import org.osate.aadl2.AccessConnection;
import org.osate.aadl2.AccessType;
import org.osate.aadl2.BooleanLiteral;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.ClassifierValue;
import org.osate.aadl2.ConnectedElement;
import org.osate.aadl2.Connection;
import org.osate.aadl2.ConnectionEnd;
import org.osate.aadl2.DataClassifier;
import org.osate.aadl2.DataImplementation;
import org.osate.aadl2.DataSubcomponent;
import org.osate.aadl2.DirectionType;
import org.osate.aadl2.Element;
import org.osate.aadl2.EnumerationLiteral;
import org.osate.aadl2.Feature;
import org.osate.aadl2.FeatureConnection;
import org.osate.aadl2.FeatureGroup;
import org.osate.aadl2.FeatureGroupConnection;
import org.osate.aadl2.FeatureGroupType;
import org.osate.aadl2.ListValue;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.NamedValue;
import org.osate.aadl2.NumberValue;
import org.osate.aadl2.ParameterConnection;
import org.osate.aadl2.PortConnection;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PropertyConstant;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.RangeValue;
import org.osate.aadl2.RecordValue;
import org.osate.aadl2.ReferenceValue;
import org.osate.aadl2.StringLiteral;
import org.osate.aadl2.Subcomponent;
import org.osate.aadl2.UnitLiteral;
import org.osate.aadl2.impl.AccessImpl;
import org.osate.aadl2.impl.BusAccessImpl;
import org.osate.aadl2.impl.BusSubcomponentImpl;
import org.osate.aadl2.impl.DataTypeImpl;
import org.osate.aadl2.impl.DirectedFeatureImpl;
import org.osate.aadl2.impl.FeatureGroupImpl;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.ConnectionInstanceEnd;
import org.osate.aadl2.instance.ConnectionReference;
import org.osate.aadl2.instance.FeatureCategory;
import org.osate.aadl2.instance.FeatureInstance;
import org.osate.aadl2.instance.FlowSpecificationInstance;
import org.osate.aadl2.instance.InstancePackage;
import org.osate.aadl2.instance.InstanceReferenceValue;
import org.osate.aadl2.instance.ModeTransitionInstance;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.xtext.aadl2.properties.util.PropertyUtils;
import org.osgi.framework.Bundle;
import org.sireum.Option;
import org.sireum.Some;
import org.sireum.aadl.osate.PreferenceValues;
import org.sireum.hamr.ir.AadlASTJavaFactory;
import org.sireum.hamr.ir.Annex;
import org.sireum.hamr.ir.AnnexLib;
import org.sireum.message.Position;


public class Visitor {
	protected final org.sireum.hamr.ir.AadlASTFactory factory = new org.sireum.hamr.ir.AadlASTFactory();

	protected final Map<String, org.sireum.hamr.ir.Component> datamap = new LinkedHashMap<>();
	final Map<List<String>, Set<ConnectionReference>> compConnMap = new HashMap<>();
	List<AnnexVisitor> annexVisitors = new ArrayList<>();

	/** allowing this to throw exceptions as they will be caught by {@link org.sireum.aadl.osate.util.Util#getAir }
	 * and printed to the passed in output stream
	 *
	 * @throws Exception
	 */
	public Visitor() throws Exception {
		annexVisitors.add(new Emv2Visitor(this));

		Bundle sm = Platform.getBundle("org.sireum.aadl.osate.securitymodel");
		if (sm != null) {
			annexVisitors.add(new SmfVisitor(this));
		}

		Bundle ba = Platform.getBundle("org.osate.ba");
		if (ba != null && PreferenceValues.getPROCESS_BA_OPT()) {
			annexVisitors.add(new BAVisitor(this));
		}

		Bundle gumbo = Platform.getBundle("org.sireum.aadl.gumbo");
		if (gumbo != null) {
			annexVisitors.add(new GumboVisitor(this));
		}

		Bundle bless = Platform.getBundle("com.multitude.aadl.bless");
		Bundle bless2Air = Platform.getBundle("org.sireum.aadl.osate.bless2Air");
		if (bless != null && bless2Air != null && PreferenceValues.getPROCESS_BA_OPT()) {

			// Bless is closed source so a Sireum OSATE plugin developer may not have
			// access to its source code. Therefore the BlessVisitor was placed in a separate
			// plugin which has the Bless plugin dependencies. Below we'll reflexively construct
			// BlessVisitor so that there are no hard-coded dependencies to anything Bless related

			Class<?> cls = bless2Air.loadClass("org.sireum.aadl.osate.architecture.BlessVisitor");
			if (AnnexVisitor.class.isAssignableFrom(cls)) {
				Constructor<?> cons = cls.getConstructor(new Class[] { Visitor.class });
				annexVisitors.add((AnnexVisitor) cons.newInstance(this));
			} else {
				throw new RuntimeException("Could not load Bless to AIR plugin: " + cls.getCanonicalName()
						+ " doesn't implement AnnexVisitor");
			}
		}
	}

	public Map<String, org.sireum.hamr.ir.Component> getDataComponents() {
		// TODO should be an immutable copy
		return this.datamap;
	}

	public Option<org.sireum.hamr.ir.Aadl> convert(Element root, boolean includeDataComponents) {
		final Option<org.sireum.hamr.ir.Component> t = visit(root);
		if (t.nonEmpty()) {
			final List<org.sireum.hamr.ir.Component> dataComponents = includeDataComponents
					? new ArrayList<>(datamap.values())
					: VisitorUtil.iList();

			List<AnnexLib> libs = VisitorUtil.iList();
			for (AnnexVisitor av : annexVisitors) {
				libs = VisitorUtil.addAll(libs, av.buildAnnexLibraries(root));
			}

			return new Some<>(factory.aadl(VisitorUtil.toIList(t.get()), libs, dataComponents));
		} else {
			return org.sireum.None.apply();
		}
	}

	private Option<org.sireum.hamr.ir.Component> visit(Element root) {
		switch (root.eClass().getClassifierID()) {
		case InstancePackage.SYSTEM_INSTANCE:
		case InstancePackage.COMPONENT_INSTANCE: {
			org.sireum.hamr.ir.Component c = buildComponent((ComponentInstance) root, VisitorUtil.iList());
			return new Some<>(c);
		}
		default:
			return org.sireum.None.apply();
		}
	}

	private List<org.sireum.hamr.ir.Connection> buildConnection(
			ConnectionReference connRef,
			List<String> path,
			ComponentInstance compInst) {
		final Connection conn = connRef.getConnection();
		List<String> name = VisitorUtil.add(path, conn.getName());
		final ConnectionInstanceEnd scie = connRef.getSource();
		final ConnectionInstanceEnd dcie = connRef.getDestination();


//		List<org.sireum.hamr.ir.EndPoint> sep = buildEndPoint(sfii, path);
//		List<org.sireum.hamr.ir.EndPoint> dep = buildEndPoint(dcie, path);

		List<org.sireum.hamr.ir.EndPoint> src = VisitorUtil.iList();
		List<org.sireum.hamr.ir.EndPoint> dst = VisitorUtil.iList();
		// List<org.sireum.hamr.ir.EndPoint> src1 = VisitorUtil.iList();
		// List<org.sireum.hamr.ir.EndPoint> dst1 = VisitorUtil.iList();

		if ((scie instanceof FeatureInstance) && (dcie instanceof FeatureInstance)
				&& (((FeatureInstance) scie).getCategory() == ((FeatureInstance) dcie).getCategory())) {
//			src1 = buildEndPoint(conn.getSource(), path);
//			dst1 = buildEndPoint(conn.getDestination(), path);

			src = buildEndPoint(scie, path);
			dst = buildEndPoint(dcie, path);
		} else {
			src = buildEndPoint(conn.getSource(), path);
			dst = buildEndPoint(conn.getDestination(), path);
		}

		final boolean isBiDirectional = conn.isBidirectional();
		final List<ConnectionInstance> connInst = compInst.findConnectionInstance(conn);

		List<org.sireum.hamr.ir.Name> connectionInstances = VisitorUtil.iList();
		if (!connInst.isEmpty()) {
			connectionInstances = connInst.stream().map(ci -> factory
					.name(Arrays.asList(ci.getInstanceObjectPath().split("\\.")), VisitorUtil.buildPosInfo(ci)))
					.collect(Collectors.toList());
		}

		AadlASTJavaFactory.ConnectionKind kind = null;
		if (conn instanceof AccessConnection) {
			kind = AadlASTJavaFactory.ConnectionKind.Access;
		} else if (conn instanceof FeatureGroupConnection) {
			kind = AadlASTJavaFactory.ConnectionKind.FeatureGroup;
		} else if (conn instanceof FeatureConnection) {
			kind = AadlASTJavaFactory.ConnectionKind.Feature;
		} else if (conn instanceof ParameterConnection) {
			kind = AadlASTJavaFactory.ConnectionKind.Parameter;
		} else if (conn instanceof PortConnection) {
			kind = AadlASTJavaFactory.ConnectionKind.Port;
		} else {
			throw new RuntimeException("Unexpected connection kind: " + conn);
		}
		if (src.size() == 1 && dst.size() == 1 && src.get(0).getFeature().nonEmpty()
				&& dst.get(0).getFeature().nonEmpty() && (conn instanceof FeatureGroupConnection)) {

			scala.collection.immutable.Seq<org.sireum.String> srcNameSeq = src.get(0).getFeature().get().name()
					.elements();
			scala.collection.immutable.Seq<org.sireum.String> dstNameSeq = dst.get(0).getFeature().get().name()
					.elements();

			// eclipse jdt hack

			// String srcName = src.get(0).getFeature().get().name().elements().toList().last().string();
			// String dstName = dst.get(0).getFeature().get().name().elements().toList().last().string();

			String srcName = ((scala.collection.IterableOnceOps<?, ?, ?>) srcNameSeq).toList().last().toString();
			String dstName = ((scala.collection.IterableOnceOps<?, ?, ?>) dstNameSeq).toList().last().toString();

			name = VisitorUtil.add(path, conn.getName() + "-" + srcName + "_" + dstName);
			// System.out.println(conn.getName());
		}

		final List<String> na = name;

		final List<org.sireum.hamr.ir.Property> properties = conn.getOwnedPropertyAssociations().stream()
				.map(pa -> buildProperty(pa, na)).collect(Collectors.toList());

		if (src.size() != dst.size()) {
			throw new RuntimeException("Incorrect translation!");
		}

		if (src.equals(dst)) {
			// System.out.println(scie.getComponentInstancePath() + " -> " + dcie.getComponentInstancePath());
		}

		if (!src.equals(dst)) {
			return VisitorUtil.toIList(factory.connection(factory.name(na, VisitorUtil.buildPosInfo(conn)),
					src,
					dst,
					kind, isBiDirectional, connectionInstances, properties, VisitorUtil.getUriFragment(connRef)));
		} else if (dst.isEmpty() && src.isEmpty()) {
			// System.out.println(conn.getName());
			return VisitorUtil.iList();
		} else {
			return VisitorUtil.iList();
		}
	}

	private org.sireum.hamr.ir.EndPoint buildEndPoint(ConnectionInstanceEnd cie) {
		final List<String> component = Arrays.asList(cie.getComponentInstance().getInstanceObjectPath().split("\\."));
		final Position componentPos = VisitorUtil
				.buildPosInfo(cie.getComponentInstance().getInstantiatedObjects().get(0));

		if (cie instanceof FeatureInstance) {
			final List<String> feature = Arrays.asList(cie.getInstanceObjectPath().split("\\."));
			final Position featurePos = VisitorUtil.buildPosInfo(cie.getInstantiatedObjects().get(0));

			final AadlASTJavaFactory.Direction direction = handleDirection(((FeatureInstance) cie).getDirection());

			return factory.endPoint(factory.name(component, componentPos), factory.name(feature, featurePos),
					direction);

		} else if (cie instanceof ComponentInstance) {
			return factory.endPoint(factory.name(component, componentPos), null, null);
		} else if (cie instanceof ModeTransitionInstance) {
			throw new RuntimeException("Need to handle ModeTransitionInstanceImpl: " + cie);
		} else {
			throw new RuntimeException("Unexpected: " + cie);
		}
	}

	private List<org.sireum.hamr.ir.EndPoint> flattenFeatureGroupInstance(
			FeatureInstance fii,
			String featurePre,
			List<String> component,
			Position componentPos, Boolean isInverse) {
		List<org.sireum.hamr.ir.EndPoint> result = VisitorUtil.iList();
		if (fii.getCategory() == FeatureCategory.FEATURE_GROUP && !fii.getFeatureInstances().isEmpty()) {
			result = fii
					.getFeatureInstances().stream().flatMap(fisl -> flattenFeatureGroupInstance(fisl,
							featurePre + "_" + fii.getName(), component, componentPos, isInverse).stream())
					.collect(Collectors.toList());

		} else {
			String fname = featurePre + "_" + fii.getName();
			List<String> feature = VisitorUtil.add(component, fname);
			final Position featurePos = VisitorUtil.buildPosInfo(fii.getInstantiatedObjects().get(0));
			AadlASTJavaFactory.Direction dir = null;
			if(isInverse) {
				dir = (handleDirection(fii.getDirection()) == AadlASTJavaFactory.Direction.In) ? AadlASTJavaFactory.Direction.Out : AadlASTJavaFactory.Direction.In;
			} else {
				dir = handleDirection(fii.getDirection());
			}

			result = VisitorUtil.add(result, factory.endPoint(factory.name(component, componentPos),
					factory.name(feature, featurePos), dir));
		}
		if (result.size() > 1) {
//			System.out.println("");
		}
		return result;
	}

	private List<org.sireum.hamr.ir.EndPoint> buildEndPoint(ConnectionInstanceEnd connInstEnd, List<String> path) {
		List<org.sireum.hamr.ir.EndPoint> result = VisitorUtil.iList();

		final List<String> component = Arrays
				.asList(connInstEnd.getComponentInstance().getInstanceObjectPath().split("\\."));
		final Position componentPos = VisitorUtil
				.buildPosInfo(connInstEnd.getComponentInstance().getInstantiatedObjects().get(0));
		if (connInstEnd instanceof FeatureInstance) {
			FeatureInstance connElem = (FeatureInstance) connInstEnd;
			String featurePre = connElem.getFeature().getName();
			FeatureInstance temp = connElem;
			while (temp.eContainer() instanceof FeatureInstance) {
				featurePre = ((FeatureInstance) temp.eContainer()).getName() + "_" + featurePre;
				temp = (FeatureInstance) temp.eContainer();
			}

			if (connElem.getCategory() == FeatureCategory.FEATURE_GROUP && !connElem.getFeatureInstances().isEmpty()) {

				// Feature ff = connElem.getFeature().getRefined();
				final String fp = featurePre;

				result = VisitorUtil.addAll(
						result,
						connElem.getFeatureInstances().stream().flatMap(fii -> flattenFeatureGroupInstance(fii, fp,
								component, componentPos, false).stream())
								.collect(Collectors.toList()));

			} else if (connElem.getCategory() == FeatureCategory.BUS_ACCESS) {
				final List<String> feature = VisitorUtil.add(component, featurePre);
				final Position featurePos = VisitorUtil.buildPosInfo(connElem.getInstantiatedObjects().get(0));
				final AadlASTJavaFactory.Direction direction = AadlASTJavaFactory.Direction.InOut;
				result = VisitorUtil.add(result, factory.endPoint(factory.name(component, componentPos),
						factory.name(feature, featurePos), direction));
			} else {
				final List<String> feature = VisitorUtil.add(component, featurePre);
				final Position featurePos = VisitorUtil.buildPosInfo(connElem.getInstantiatedObjects().get(0));
				final AadlASTJavaFactory.Direction direction = handleDirection(connElem.getDirection());
				result = VisitorUtil.add(result, factory.endPoint(factory.name(component, componentPos),
						factory.name(feature, featurePos), direction));
			}

			// org.sireum.hamr.ir.Feature f = buildFeature(connElem, component);

//			if(connElem.getCategory() == FeatureCategory.FEATURE_GROUP) {
//				connElem.getFeatureInstances().forEach(fi -> {
//
//				});
//			}


//			final List<String> component = (connElem.getgetContext() != null) && (connElem
//			.getContext() instanceof Subcomponent)
//			? VisitorUtil.add(path, connElem.getContext().getName())
//			: path;
		} else if (connInstEnd instanceof ComponentInstance) {
			result = VisitorUtil.toIList(factory.endPoint(factory.name(component, componentPos), null, null));
		} else if (connInstEnd instanceof ModeTransitionInstance) {
			throw new RuntimeException("Need to handle ModeTransitionInstanceImpl: " + connInstEnd);
		} else {
			throw new RuntimeException("Unexpected: " + connInstEnd);
		}
		if (result.size() > 1) {
//			System.out.println("");
		}
		return result;
	}

	private List<org.sireum.hamr.ir.EndPoint> buildEndPoint(ConnectedElement connElem, List<String> path) {
		List<org.sireum.hamr.ir.EndPoint> result = VisitorUtil.iList();
		final List<String> component = (connElem.getContext() != null) && (connElem
				.getContext() instanceof Subcomponent)
				? VisitorUtil.add(path, connElem.getContext().getName())
				: path;
		final List<String> feature = (connElem.getContext() instanceof FeatureGroup)
				? VisitorUtil.add(component,
						connElem.getContext().getName() + "_" + connElem.getConnectionEnd().getName())
				: VisitorUtil.add(component, connElem.getConnectionEnd().getName());
		AadlASTJavaFactory.Direction dir = null;
		if (connElem.getConnectionEnd() instanceof DirectedFeatureImpl) {
			final DirectedFeatureImpl inFeature = (DirectedFeatureImpl) connElem.getConnectionEnd();
			if (inFeature.isIn() && inFeature.isOut()) {
				dir = AadlASTJavaFactory.Direction.InOut;
			} else if (inFeature.isIn() && !inFeature.isOut()) {
				dir = AadlASTJavaFactory.Direction.In;
			} else {
				dir = AadlASTJavaFactory.Direction.Out;
			}
		}
		final ConnectionEnd ce = connElem.getConnectionEnd();
		// String cname = AadlUtil.getConnectionEndName(connElem);
		if (ce instanceof FeatureGroupImpl) {
			final FeatureGroupImpl fgce = (FeatureGroupImpl) ce;
			result = VisitorUtil.addAll(result, flattenFeatureGroup(component, fgce.getFullName(), fgce, connElem));
		} else if (ce instanceof BusSubcomponentImpl) {
			result = VisitorUtil.add(result,
					factory.endPoint(factory.name(feature, VisitorUtil.buildPosInfo(connElem.getConnectionEnd())), null,
							AadlASTJavaFactory.Direction.InOut));
		} else if (ce instanceof BusAccessImpl) {
			result = VisitorUtil.add(result,
					factory.endPoint(
							factory.name(component,
									(connElem.getContext() != null) ? VisitorUtil.buildPosInfo(connElem.getContext())
											: null),
							factory.name(feature, VisitorUtil.buildPosInfo(connElem.getConnectionEnd())),
							AadlASTJavaFactory.Direction.InOut));
		} else {
			result = VisitorUtil.add(result,
					factory.endPoint(
							factory.name(component,
									(connElem.getContext() != null) ? VisitorUtil.buildPosInfo(connElem.getContext())
											: null),
							factory.name(feature, VisitorUtil.buildPosInfo(connElem.getConnectionEnd())), dir));
		}
		if (result.size() > 1) {
//			System.out.println("");
		}
		return result;
	}

	private List<org.sireum.hamr.ir.EndPoint> flattenFeatureGroup(List<String> component, String parentName,
			FeatureGroupImpl fgi, ConnectedElement connElem) {

		List<org.sireum.hamr.ir.EndPoint> res = VisitorUtil.iList();
		FeatureGroupType fgt = fgi.getFeatureGroupType();

		/**
		// if (fgt == null) {
			final FeatureGroupPrototype fgpt = fgi.basicGetFeatureGroupPrototype();
			if (fgpt != null) {
				fgt = ResolvePrototypeUtil.resolveFeatureGroupPrototype(
						fgpt,
					connElem.getContext() == null ? connElem.getContainingComponentImpl() : connElem.getContext());
			}
			// }
		*/
		if (fgt != null) {
		for (Feature f : fgt.getAllFeatures()) {
			Feature rf = f;// .getRefined();
//			if (rf == null) {
//				rf = f;
//			}
			if (rf instanceof FeatureGroupImpl) {
				res = VisitorUtil.addAll(res, flattenFeatureGroup(component, parentName + "_" + rf.getFullName(),
						(FeatureGroupImpl) rf, connElem));
			} else {
				AadlASTJavaFactory.Direction dir = null;
				if (AadlUtil.isIncomingFeature(rf) && AadlUtil.isOutgoingFeature(rf)) {
					dir = AadlASTJavaFactory.Direction.InOut;
				} else if (AadlUtil.isIncomingFeature(rf)) {
					dir = fgi.isInverse() ? AadlASTJavaFactory.Direction.Out : AadlASTJavaFactory.Direction.In;
				} else {
					dir = fgi.isInverse() ? AadlASTJavaFactory.Direction.In : AadlASTJavaFactory.Direction.Out;
				}

				res = VisitorUtil.add(res,
						factory.endPoint(factory.name(component, null),
								factory.name(VisitorUtil.add(component, parentName + "_" + rf.getFullName()),
										VisitorUtil.buildPosInfo(rf)),
								dir));
			}
		}
	}
		return res;
	}

	private org.sireum.hamr.ir.Component buildComponent(ComponentInstance compInst, List<String> path) {
		final List<String> currentPath = VisitorUtil.add(path, compInst.getName());

		final List<org.sireum.hamr.ir.Feature> features = compInst.getFeatureInstances().stream()
				.map(fi -> buildFeature(fi, currentPath)).collect(Collectors.toList());

		final List<org.sireum.hamr.ir.ConnectionInstance> connectionInstances = compInst.getConnectionInstances()
				.stream().map(ci -> buildConnectionInst(ci, currentPath)).collect(Collectors.toList());

		final List<org.sireum.hamr.ir.Property> properties = compInst.getOwnedPropertyAssociations().stream()
				.map(pa -> buildProperty(pa, currentPath)).collect(Collectors.toList());

		final List<org.sireum.hamr.ir.Flow> flows = compInst.getFlowSpecifications().stream()
				.map(fs -> buildFlow(fs, currentPath)).collect(Collectors.toList());

		final List<org.sireum.hamr.ir.Component> subComponents = compInst.getComponentInstances().stream()
				.map(ci -> buildComponent(ci, currentPath)).collect(Collectors.toList());

		List<org.sireum.hamr.ir.Connection> connections = VisitorUtil.iList();
		if (compConnMap.containsKey(currentPath)) {
			connections = compConnMap.get(currentPath).stream()
					.flatMap(c -> buildConnection(c, currentPath, compInst).stream()).collect(Collectors.toList());
		}

		AadlASTJavaFactory.ComponentCategory category = null;
		switch (compInst.getCategory()) {
		case ABSTRACT:
			category = AadlASTJavaFactory.ComponentCategory.Abstract;
			break;
		case BUS:
			category = AadlASTJavaFactory.ComponentCategory.Bus;
			break;
		case DATA:
			category = AadlASTJavaFactory.ComponentCategory.Data;
			break;
		case DEVICE:
			category = AadlASTJavaFactory.ComponentCategory.Device;
			break;
		case MEMORY:
			category = AadlASTJavaFactory.ComponentCategory.Memory;
			break;
		case PROCESS:
			category = AadlASTJavaFactory.ComponentCategory.Process;
			break;
		case PROCESSOR:
			category = AadlASTJavaFactory.ComponentCategory.Processor;
			break;
		case SUBPROGRAM:
			category = AadlASTJavaFactory.ComponentCategory.Subprogram;
			break;
		case SUBPROGRAM_GROUP:
			category = AadlASTJavaFactory.ComponentCategory.SubprogramGroup;
			break;
		case SYSTEM:
			category = AadlASTJavaFactory.ComponentCategory.System;
			break;
		case THREAD:
			category = AadlASTJavaFactory.ComponentCategory.Thread;
			break;
		case THREAD_GROUP:
			category = AadlASTJavaFactory.ComponentCategory.ThreadGroup;
			break;
		case VIRTUAL_BUS:
			category = AadlASTJavaFactory.ComponentCategory.VirtualBus;
			break;
		case VIRTUAL_PROCESSOR:
			category = AadlASTJavaFactory.ComponentCategory.VirtualProcessor;
			break;
		default:
			throw new RuntimeException("Unexpected");
		}

		final org.sireum.hamr.ir.Name identifier = factory.name(currentPath,
				VisitorUtil.buildPosInfo(compInst.getInstantiatedObjects().get(0)));

		final org.sireum.hamr.ir.Classifier classifier = compInst.getClassifier() != null
				? factory.classifier(compInst.getClassifier().getQualifiedName())
				: null;

		final List<org.sireum.hamr.ir.Mode> modes = VisitorUtil.iList(); // TODO

		List<org.sireum.hamr.ir.Annex> annexes = VisitorUtil.iList();

		for (AnnexVisitor av : annexVisitors) {
			annexes = VisitorUtil.addAll(annexes, av.visit(compInst, currentPath));
		}

		return factory.component(identifier, category, classifier, features, subComponents, connections,
				connectionInstances, properties, flows, modes, annexes, VisitorUtil.getUriFragment(compInst));
	}

	private org.sireum.hamr.ir.Feature buildFeature(FeatureInstance featureInst, List<String> path) {

		final Feature f = featureInst.getFeature();

		final List<String> currentPath = VisitorUtil.add(path, featureInst.getName());

		org.sireum.hamr.ir.Classifier classifier = null;
		if (f.getFeatureClassifier() != null) {
			if (f.getFeatureClassifier() instanceof NamedElement) {
				if (((NamedElement) f.getFeatureClassifier()).getQualifiedName() != null) {
					classifier = factory
						.classifier(((NamedElement) f.getFeatureClassifier()).getQualifiedName().toString());
				} else {
					System.out.println("failing here");
				}
			} else {
				throw new RuntimeException("Unexepcted classifier " + f.getFeatureClassifier() + " for feature "
						+ featureInst.getQualifiedName());
			}
		}

		final List<org.sireum.hamr.ir.Property> properties = featureInst.getOwnedPropertyAssociations().stream()
				.map(pa -> buildProperty(pa, currentPath)).collect(Collectors.toList());

		AadlASTJavaFactory.FeatureCategory category = null;
		switch (featureInst.getCategory()) {
		case ABSTRACT_FEATURE:
			category = AadlASTJavaFactory.FeatureCategory.AbstractFeature;
			break;
		case BUS_ACCESS:
			category = AadlASTJavaFactory.FeatureCategory.BusAccess;
			break;
		case DATA_ACCESS:
			category = AadlASTJavaFactory.FeatureCategory.DataAccess;
			break;
		case DATA_PORT:
			category = AadlASTJavaFactory.FeatureCategory.DataPort;
			break;
		case EVENT_PORT:
			category = AadlASTJavaFactory.FeatureCategory.EventPort;
			break;
		case EVENT_DATA_PORT:
			category = AadlASTJavaFactory.FeatureCategory.EventDataPort;
			break;
		case FEATURE_GROUP:
			category = AadlASTJavaFactory.FeatureCategory.FeatureGroup;
			break;
		case PARAMETER:
			category = AadlASTJavaFactory.FeatureCategory.Parameter;
			break;
		case SUBPROGRAM_ACCESS:
			category = AadlASTJavaFactory.FeatureCategory.SubprogramAccess;
			break;
		case SUBPROGRAM_GROUP_ACCESS:
			category = AadlASTJavaFactory.FeatureCategory.SubprogramAccessGroup;
			break;
		default:
			throw new RuntimeException("Unexpected category: " + featureInst.getCategory());
		}

		switch (featureInst.getCategory()) {
		case DATA_ACCESS:
		case DATA_PORT:
		case EVENT_DATA_PORT:
		case PARAMETER:
			if (f.getClassifier() instanceof DataClassifier) {
				processDataType((DataClassifier) f.getClassifier());
			}
		default: // do nothing
		}

		org.sireum.hamr.ir.Name identifier = factory.name(
				currentPath,
				VisitorUtil.buildPosInfo(featureInst.getInstantiatedObjects().get(0)));

		final List<FeatureInstance> featureInstances = featureInst.getFeatureInstances();
		if (featureInstances.isEmpty()) {
			if (f instanceof AccessImpl) {
				final AccessImpl accessImpl = (AccessImpl) f;
				final AadlASTJavaFactory.AccessType accessType = accessImpl.getKind() == AccessType.PROVIDES
						? AadlASTJavaFactory.AccessType.Provides
						: AadlASTJavaFactory.AccessType.Requires;
				AadlASTJavaFactory.AccessCategory accessCategory = null;
				switch (accessImpl.getCategory()) {
				case BUS:
					accessCategory = AadlASTJavaFactory.AccessCategory.Bus;
					break;
				case DATA:
					accessCategory = AadlASTJavaFactory.AccessCategory.Data;
					break;
				case SUBPROGRAM:
					accessCategory = AadlASTJavaFactory.AccessCategory.Subprogram;
					break;
				case SUBPROGRAM_GROUP:
					accessCategory = AadlASTJavaFactory.AccessCategory.SubprogramGroup;
					break;
				case VIRTUAL_BUS:
					accessCategory = AadlASTJavaFactory.AccessCategory.VirtualBus;
					break;
				}
				return factory.featureAccess(identifier, category, classifier, accessType, accessCategory, properties,
						VisitorUtil.getUriFragment(featureInst));
			} else if (f instanceof DirectedFeatureImpl) {
				final AadlASTJavaFactory.Direction direction = handleDirection(featureInst.getDirection());

				return factory.featureEnd(identifier, direction, category, classifier, properties,
						VisitorUtil.getUriFragment(featureInst));
			} else {
				throw new RuntimeException("Not expecting feature: " + featureInst);
			}
		} else {
			final boolean isInverse = ((FeatureGroupImpl) f).isInverse();
			final List<org.sireum.hamr.ir.Feature> features = featureInstances.stream()
					.map(fi -> buildFeature(fi, currentPath)).collect(Collectors.toList());
			return factory.featureGroup(identifier, features, isInverse, category, properties,
					VisitorUtil.getUriFragment(featureInst));
		}
	}

	private org.sireum.hamr.ir.ConnectionReference buildConnectionRef(ConnectionReference connRef, List<String> path) {
		final List<String> context = Arrays.asList(connRef.getContext().getInstanceObjectPath().split("\\."));
		final List<String> name = VisitorUtil.add(context, connRef.getConnection().getName());

		if (compConnMap.containsKey(context)) {
			compConnMap.put(context, VisitorUtil.add(compConnMap.get(context), connRef));
		} else {
			compConnMap.put(context, VisitorUtil.toISet(connRef));
		}
		return factory.connectionReference(factory.name(name, VisitorUtil.buildPosInfo(connRef.getConnection())),
				factory.name(context, VisitorUtil.buildPosInfo(connRef.getContext().getInstantiatedObjects().get(0))),
				path.equals(context));
	}

	private org.sireum.hamr.ir.ConnectionInstance buildConnectionInst(ConnectionInstance connInst, List<String> path) {
		final List<String> currentPath = VisitorUtil.add(path, connInst.getName());

		final org.sireum.hamr.ir.Name name = factory.name(currentPath,
				VisitorUtil.buildPosInfo(connInst.getInstantiatedObjects().get(0)));

		final List<org.sireum.hamr.ir.Property> properties = connInst.getOwnedPropertyAssociations().stream()
				.map(pa -> buildProperty(pa, currentPath)).collect(Collectors.toList());

		AadlASTJavaFactory.ConnectionKind kind = null;
		switch (connInst.getKind()) {
		case ACCESS_CONNECTION:
			kind = AadlASTJavaFactory.ConnectionKind.Access;
			break;
		case FEATURE_CONNECTION:
			kind = AadlASTJavaFactory.ConnectionKind.Feature;
			break;
		case FEATURE_GROUP_CONNECTION:
			kind = AadlASTJavaFactory.ConnectionKind.FeatureGroup;
			break;
		case MODE_TRANSITION_CONNECTION:
			kind = AadlASTJavaFactory.ConnectionKind.ModeTransition;
			break;
		case PARAMETER_CONNECTION:
			kind = AadlASTJavaFactory.ConnectionKind.Parameter;
			break;
		case PORT_CONNECTION:
			kind = AadlASTJavaFactory.ConnectionKind.Port;
			break;
		}

		final List<org.sireum.hamr.ir.ConnectionReference> connectionRefs = connInst.getConnectionReferences().stream()
				.map(ci -> buildConnectionRef(ci, path)).collect(Collectors.toList());

		final org.sireum.hamr.ir.EndPoint src = buildEndPoint(connInst.getSource());

		final org.sireum.hamr.ir.EndPoint dst = buildEndPoint(connInst.getDestination());

		return factory.connectionInstance(name, src, dst, kind, connectionRefs, properties);
	}

	private org.sireum.hamr.ir.Flow buildFlow(FlowSpecificationInstance flowInst, List<String> path) {

		final List<String> currentPath = VisitorUtil.add(path, flowInst.getQualifiedName());

		final org.sireum.hamr.ir.Name name = factory
				.name(currentPath,
				VisitorUtil.buildPosInfo(flowInst.getInstantiatedObjects().get(0)));

		AadlASTJavaFactory.FlowKind kind = null;
		switch (flowInst.getFlowSpecification().getKind()) {
		case SOURCE:
			kind = AadlASTJavaFactory.FlowKind.Source;
			break;
		case SINK:
			kind = AadlASTJavaFactory.FlowKind.Sink;
			break;
		case PATH:
			kind = AadlASTJavaFactory.FlowKind.Path;
			break;
		}

		org.sireum.hamr.ir.Name source = null;
		if (flowInst.getSource() != null) {
			// List<String> us = Arrays.asList(flowInst.getSource().getInstanceObjectPath().split("\\."));
			source = buildFeatureRef(flowInst.getSource(), path);
		}

		org.sireum.hamr.ir.Name sink = null;
		if (flowInst.getDestination() != null) {
			// List<String> ud = Arrays.asList(flowInst.getDestination().getInstanceObjectPath().split("\\."));
			sink = buildFeatureRef(flowInst.getDestination(), path);
		}

		return factory.flow(name, kind, source, sink, VisitorUtil.getUriFragment(flowInst));
	}

	private org.sireum.hamr.ir.Name buildFeatureRef(FeatureInstance featureRef, List<String> path) {
		String name = featureRef.getName();
		EObject temp = featureRef.eContainer();
		while (temp instanceof FeatureInstance
				&& ((FeatureInstance) temp).getCategory() == FeatureCategory.FEATURE_GROUP) {
			name = ((FeatureInstance) temp).getName() + "_" + name;
			temp = ((FeatureInstance) temp).eContainer();
		}

		return factory.name(VisitorUtil.add(path, name),
				VisitorUtil.buildPosInfo(featureRef.getInstantiatedObjects().get(0)));
	}

	protected org.sireum.hamr.ir.Property buildProperty(PropertyAssociation pa, List<String> path) {
		final Property prop = pa.getProperty();
		final List<String> currentPath = VisitorUtil.add(path, prop.getQualifiedName());
		final NamedElement cont = (NamedElement) pa.eContainer();
		// PropertyType propName = prop.getPropertyType();
		List<org.sireum.hamr.ir.PropertyValue> propertyValues = VisitorUtil.iList();
		try {
			PropertyExpression pe = PropertyUtils.getSimplePropertyValue(cont, prop);
			propertyValues = getPropertyExpressionValue(pe, path);
		} catch (Throwable t) {
			java.lang.System.err.println("Error encountered while trying to fetch property value for "
					+ prop.getQualifiedName() + " from " + cont.getQualifiedName() + " : " + t.getMessage());
		}

		return factory.property(factory.name(currentPath, VisitorUtil.buildPosInfo(prop)), propertyValues,
				VisitorUtil.iList());
	}

	private org.sireum.hamr.ir.UnitProp getUnitProp(NumberValue nv) {
		if (nv == null) {
			return factory.unitProp("??", null);
		} else {
			final double v = org.osate.aadl2.operations.NumberValueOperations.getScaledValue(nv);
			final UnitLiteral u = org.osate.aadl2.operations.UnitLiteralOperations.getAbsoluteUnit(nv.getUnit());
			return factory.unitProp(Double.toString(v), u == null ? null : u.getName());
		}
	}

	protected List<org.sireum.hamr.ir.PropertyValue> getPropertyExpressionValue(PropertyExpression pe,
			List<String> path) {

		if(pe instanceof BooleanLiteral) {
			final String b = Boolean.toString(((BooleanLiteral) pe).getValue());
			return VisitorUtil.toIList(factory.valueProp(b));
		}
		else if(pe instanceof NumberValue) {
			return VisitorUtil.toIList(getUnitProp((NumberValue) pe));
		}
		else if(pe instanceof StringLiteral) {
			final String v = ((StringLiteral) pe).getValue();
			return VisitorUtil.toIList(factory.valueProp(v));
		}
		else if(pe instanceof RangeValue) {
			final RangeValue rv = (RangeValue) pe;
			return VisitorUtil
					.toIList(factory.rangeProp(getUnitProp(rv.getMinimumValue()), getUnitProp(rv.getMaximumValue())));
		}
		else if(pe instanceof ClassifierValue) {
			final Classifier cv = ((ClassifierValue) pe).getClassifier();
			if (cv instanceof DataClassifier) {
				processDataType((DataClassifier) cv);
			}
			if (cv.getQualifiedName() != null) {
				return VisitorUtil.toIList(factory.classifierProp(cv.getQualifiedName()));
			} else {
				return VisitorUtil.iList();
			}
			// return VisitorUtil.toIList(factory.classifierProp(cv.getQualifiedName()));
		}
		else if(pe instanceof ListValue) {
			final ListValue lv = (ListValue) pe;
			List<org.sireum.hamr.ir.PropertyValue> elems = VisitorUtil.iList();
			for (PropertyExpression e : lv.getOwnedListElements()) {
				elems = VisitorUtil.addAll(elems, getPropertyExpressionValue(e, path));
			}
			return elems;
		}
		else if(pe instanceof NamedValue) {
			final NamedValue nv = (NamedValue) pe;
			final AbstractNamedValue nv2 = nv.getNamedValue();

			if (nv2 instanceof EnumerationLiteral) {
				final EnumerationLiteral el = (EnumerationLiteral) nv2;
				return VisitorUtil.toIList(factory.valueProp(el.getFullName()));
			}
			else if (nv2 instanceof Property) {
				final Property _p = (Property) nv2;
				if (_p.getDefaultValue() != null) {
					return getPropertyExpressionValue(_p.getDefaultValue(), path);
				} else {
					return VisitorUtil.toIList(factory.valueProp(_p.getQualifiedName()));
				}
			}
			else if (nv2 instanceof PropertyConstant) {
				final PropertyConstant pc = (PropertyConstant) nv2;
				return getPropertyExpressionValue(pc.getConstantValue(), path);
			}
			else {
				java.lang.System.err.println("Not handling " + pe.eClass().getClassifierID() + " " + nv2);
				return VisitorUtil.iList();
			}
		}
		else if (pe instanceof RecordValue) {
			final RecordValue rvy = (RecordValue) pe;
			final List<org.sireum.hamr.ir.Property> properties = rvy.getOwnedFieldValues().stream()
					.map(fv -> factory.property(
							factory.name(VisitorUtil.add(path, fv.getProperty().getQualifiedName()),
									VisitorUtil.buildPosInfo(fv.getProperty())),
							getPropertyExpressionValue(fv.getOwnedValue(), path), VisitorUtil.iList()))
					.collect(Collectors.toList());
			return VisitorUtil.toIList(factory.recordProp(properties));
		}
		else if (pe instanceof ReferenceValue) {
			final ReferenceValue rvx = (ReferenceValue) pe;
			final org.sireum.hamr.ir.Name refName = factory.name(VisitorUtil.toIList(rvx.toString()),
					VisitorUtil.buildPosInfo(rvx.getPath().getNamedElement()));
			return VisitorUtil.toIList(factory.referenceProp(refName));
		}
		else if (pe instanceof InstanceReferenceValue) {

			final InstanceReferenceValue irv = (InstanceReferenceValue) pe;
			final String t = irv.getReferencedInstanceObject().getInstanceObjectPath();

			return VisitorUtil.toIList(factory.referenceProp(factory.name(Arrays.asList(t.split("\\.")),
					VisitorUtil.buildPosInfo(irv.getReferencedInstanceObject()))));
		}
		else {
			java.lang.System.err.println("Need to handle " + pe + " " + pe.eClass().getClassifierID());
			if (pe.getClass().getName() != null) {
				return VisitorUtil.toIList(factory.classifierProp(pe.getClass().getName()));
			} else {
				return VisitorUtil.iList();
			}

		}
	}

	public org.sireum.hamr.ir.Component processDataType(DataClassifier f) {
		final String name = f.getQualifiedName();
		if (datamap.containsKey(name)) {
			return datamap.get(name);
		}

		if (f.getExtended() != null) {
			Classifier c = f.getExtended();
			String parentName = c.getQualifiedName();

			// TODO: add extended classifier name to AIR nodes
			// System.out.println(parentName + " >> " + name);
		}

		/*
		 * need to use 'getAll...' in order to pickup properties inherited from parent
		 * since DataClassifier is coming from the declarative model (i.e. isn't flattened)
		 * javadoc for method:
		 * A list of the property associations. Property associations from
		 * an ancestor component classifier will appear before those of any
		 * descendents.
		 */
		List<PropertyAssociation> allProperties = VisitorUtil.toIList(f.getAllPropertyAssociations());

		List<org.sireum.hamr.ir.Component> subComponents = VisitorUtil.iList();
		if (f instanceof DataTypeImpl) {
			// do nothing as component types can't have subcomponents
		} else if (f instanceof DataImplementation) {
			final DataImplementation di = (DataImplementation) f;

			// the properties from the data component's type are not inherited by
			// the data component's implementation in the declarative model.
			// Add the data component type's properties before the data component
			// implemention's properties
			allProperties = VisitorUtil.addAll(di.getType().getAllPropertyAssociations(), allProperties);

			for (Subcomponent subcom : di.getAllSubcomponents()) {
				if (!(subcom instanceof DataSubcomponent)) {
					throw new RuntimeException("Unexpected data subcomponent: " + subcom.getFullName() + " of type "
							+ subcom.getClass().getSimpleName() + " from " + f.getFullName());
				}

				DataSubcomponent dsc = (DataSubcomponent) subcom;

				final org.sireum.hamr.ir.Name subName = factory.name(VisitorUtil.toIList(dsc.getName()),
						VisitorUtil.buildPosInfo(dsc));
				final List<org.sireum.hamr.ir.Property> fProperties = dsc.getOwnedPropertyAssociations().stream()
						.map(op -> buildProperty(op, VisitorUtil.iList())).collect(Collectors.toList());

				DataClassifier sct = null;
				if (dsc.getDataSubcomponentType() instanceof DataClassifier) {
					sct = (DataClassifier) dsc.getDataSubcomponentType();
				} else {
					if(dsc.getDataSubcomponentType() != null) {
						String mesg = "Expecting a DataClassifier for " + dsc.qualifiedName()
							+ " but found something of type " + dsc.getDataSubcomponentType().getClass().getSimpleName()
							+ (dsc.getDataSubcomponentType().hasName() ? " whose name is " + dsc.getDataSubcomponentType().getQualifiedName() : "")
							+ ". This can happen when your model has multiple copies of the same resource.";

						throw new RuntimeException(mesg);
					}
				}

				if (sct != null) {
					final org.sireum.hamr.ir.Component c = processDataType(sct);

					final List<org.sireum.hamr.ir.Property> cProps = VisitorUtil
							.addAll(VisitorUtil.isz2IList(c.properties()), fProperties);

					final AadlASTJavaFactory.ComponentCategory category = AadlASTJavaFactory.ComponentCategory
							.valueOf(c.category().name());

					final org.sireum.hamr.ir.Classifier classifier = c.classifier().nonEmpty() ? c.classifier().get()
							: null;

					final org.sireum.hamr.ir.Component sub = factory.component(subName, category, classifier,
							VisitorUtil.isz2IList(c.features()), VisitorUtil.isz2IList(c.subComponents()),
							VisitorUtil.isz2IList(c.connections()), VisitorUtil.isz2IList(c.connectionInstances()),
							cProps, VisitorUtil.isz2IList(c.flows()), VisitorUtil.isz2IList(c.modes()),
							VisitorUtil.isz2IList(c.annexes()), VisitorUtil.getUriFragment(sct));

					subComponents = VisitorUtil.add(subComponents, sub);
				} else {
					// type not specified for subcomponent/field
					final org.sireum.hamr.ir.Component sub = factory.component(subName, // name
							AadlASTJavaFactory.ComponentCategory.Data, // category
							null, // classifier
							VisitorUtil.iList(), // features
							VisitorUtil.iList(), // subComponents
							VisitorUtil.iList(), // connections
							VisitorUtil.iList(), // connectionInstances
							fProperties, // properties
							VisitorUtil.iList(), // flows
							VisitorUtil.iList(), // modes
							VisitorUtil.iList(), // annexes
							""
					);

					subComponents = VisitorUtil.add(subComponents, sub);
				}
			}
		} else {
			throw new RuntimeException("Unexpected data type: " + f);
		}

		// NOTE there may be multiple properties associations with the same name if, e.g, a
		// data component extends Base_Type::Integer_32 but also adds the property
		// Data_Size => 16 bits. So would need to 'findLast' when processing these
		// ... so instead remove duplicates? Unless there is a reason why we'd want to
		// know the parent values of properties the child shadows
		List<PropertyAssociation> uniqueProperties = VisitorUtil.removeShadowedProperties(allProperties);

		List<org.sireum.hamr.ir.Property> properties = uniqueProperties.stream()
				.map(op -> buildProperty(op, VisitorUtil.iList()))
				.collect(Collectors.toList());

		List<Annex> annexes = new ArrayList<>();
		for (AnnexVisitor av : annexVisitors) {
			annexes.addAll(av.visit(f, new ArrayList<>()));
		}

		// this is a hack as we're sticking something from the declarative
		// model into a node meant for things from the instance model. Todo
		// would be to add a declarative model AIR AST and ...
		final org.sireum.hamr.ir.Component c = factory.component( //
				factory.name(VisitorUtil.iList(), null), // identifier
				AadlASTJavaFactory.ComponentCategory.Data, // category
				factory.classifier(name), VisitorUtil.iList(), // features
				subComponents, VisitorUtil.iList(), // connections
				VisitorUtil.iList(), // connectionInstances
				properties, // properties
				VisitorUtil.iList(), // flows
				VisitorUtil.iList(), // modes
				annexes, // annexes
				""
		);
		datamap.put(name, c);
		return c;
	}


	private AadlASTJavaFactory.Direction handleDirection(DirectionType d) {
		AadlASTJavaFactory.Direction direction = null;
		switch (d) {
		case IN:
			direction = AadlASTJavaFactory.Direction.In;
			break;
		case OUT:
			direction = AadlASTJavaFactory.Direction.Out;
			break;
		case IN_OUT:
			direction = AadlASTJavaFactory.Direction.InOut;
			break;
		}
		return direction;
	}
}