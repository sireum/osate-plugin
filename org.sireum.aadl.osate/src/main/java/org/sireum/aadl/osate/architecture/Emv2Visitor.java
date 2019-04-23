package org.sireum.aadl.osate.architecture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.osate.aadl2.DirectionType;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.xtext.aadl2.errormodel.errorModel.AllExpression;
import org.osate.xtext.aadl2.errormodel.errorModel.AndExpression;
import org.osate.xtext.aadl2.errormodel.errorModel.ConditionElement;
import org.osate.xtext.aadl2.errormodel.errorModel.ConditionExpression;
import org.osate.xtext.aadl2.errormodel.errorModel.EMV2PropertyAssociation;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorBehaviorEvent;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorBehaviorState;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorBehaviorStateMachine;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorBehaviorTransition;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorModelLibrary;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorPropagation;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorType;
import org.osate.xtext.aadl2.errormodel.errorModel.EventOrPropagation;
import org.osate.xtext.aadl2.errormodel.errorModel.FeatureorPPReference;
import org.osate.xtext.aadl2.errormodel.errorModel.OrExpression;
import org.osate.xtext.aadl2.errormodel.errorModel.OrlessExpression;
import org.osate.xtext.aadl2.errormodel.errorModel.OrmoreExpression;
import org.osate.xtext.aadl2.errormodel.errorModel.OutgoingPropagationCondition;
import org.osate.xtext.aadl2.errormodel.errorModel.TypeSet;
import org.osate.xtext.aadl2.errormodel.errorModel.impl.ErrorPropagationImpl;
import org.osate.xtext.aadl2.errormodel.util.EMV2Util;
import org.osate.xtext.aadl2.naming.Aadl2QualifiedNameProvider;
import org.osate.xtext.aadl2.properties.util.PropertyUtils;
import org.sireum.aadl.ir.Annex;
import org.sireum.aadl.ir.BehaveStateMachine;
import org.sireum.aadl.ir.Emv2BehaviorSection;
import org.sireum.aadl.ir.Emv2Library;
import org.sireum.aadl.ir.ErrorAliasDef;
import org.sireum.aadl.ir.ErrorEvent;
import org.sireum.aadl.ir.ErrorState;
import org.sireum.aadl.ir.ErrorTransition;
import org.sireum.aadl.ir.ErrorTypeDef;
import org.sireum.aadl.ir.ErrorTypeSetDef;
import org.sireum.aadl.ir.Name;
import org.sireum.aadl.ir.PropertyValue;

public class Emv2Visitor {

	final org.sireum.aadl.ir.AadlASTFactory factory = new org.sireum.aadl.ir.AadlASTFactory();
	final LinkedHashMap<String, ErrorModelLibrary> errorLibs = new LinkedHashMap<>();
	final Aadl2QualifiedNameProvider eqp = new Aadl2QualifiedNameProvider();

	public Annex visitEmv2Comp(ComponentInstance root, List<String> path) {
		return new Annex("Emv2",
				factory.emv2Clause(getLibNames(root), VisitorUtil.addAll(inProp(root, path), outProp(root, path)),
						VisitorUtil.addAll(flowSource(root, path),
								VisitorUtil.addAll(flowPath(root, path), flowSink(root, path))),
						componentBehavior(root, path)));
	}

	private List<org.sireum.aadl.ir.Emv2Propagation> errorProp2Map(List<ErrorPropagation> errorProp, boolean isIn,
			List<String> path) {
		List<org.sireum.aadl.ir.Emv2Propagation> prop = new ArrayList<>();
		errorProp.stream().forEach(ep -> {
			HashSet<Name> inErrorTokens = new HashSet<>();
			ep.getTypeSet().getTypeTokens().forEach(tt -> {
				tt.getType().forEach(t -> {
					inErrorTokens.add(getErrorType(t));
				});
			});
			DirectionType epDir = EMV2Util.getErrorPropagationFeatureDirection(ep);
			String dirAdd = "";
			if (epDir.incoming() && epDir.outgoing()) {
				if (isIn) {
					dirAdd = "_IN";
				} else {
					dirAdd = "_OUT";
				}
			}
			org.sireum.aadl.ir.AadlASTJavaFactory.PropagationDirection dir = isIn
					? org.sireum.aadl.ir.AadlASTJavaFactory.PropagationDirection.In
					: org.sireum.aadl.ir.AadlASTJavaFactory.PropagationDirection.Out;
			Name curPath = null;
			if (ep.getFeatureorPPRef() == null) {
				curPath = factory.name(VisitorUtil.add(path, ep.getKind()), VisitorUtil.buildPosInfo(ep));
			} else {
//				ep.getFeatureorPPRef().getFeatureorPP().getContainingComponentImpl().get
				curPath = factory.name(VisitorUtil.add(path, getFeatureString(ep.getFeatureorPPRef()) + dirAdd),
						VisitorUtil.buildPosInfo(ep.getFeatureorPPRef().getFeatureorPP()));
			}
			List<Name> errorTokens = new ArrayList<>();
			errorTokens.addAll(inErrorTokens);
			prop.add(factory.emv2Propagation(dir, curPath, errorTokens));
		});
		return prop;
	}

	private Name getErrorType(NamedElement error) {

		return factory.name(VisitorUtil.add(
				VisitorUtil.toIList(EMV2Util.getPrintName(EMV2Util.getContainingErrorModelLibrary(error))),
				error.getName()), VisitorUtil.buildPosInfo(error));
	}

	private String getFeatureString(FeatureorPPReference fpp) {
		String res = fpp.getFeatureorPP().getName();
		FeatureorPPReference next = fpp.getNext();
		while (next != null) {
			res = res + "_" + next.getFeatureorPP().getName();
			next = next.getNext();
		}
		return res;
	}

	private List<org.sireum.aadl.ir.Emv2Propagation> inProp(ComponentInstance root, List<String> path) {
		List<ErrorPropagation> errorProp = EMV2Util.getAllIncomingErrorPropagations(root).stream()
				.filter(it -> (it instanceof ErrorPropagationImpl)).map(it -> (ErrorPropagationImpl) it)
				.collect(Collectors.toList());
		return errorProp2Map(errorProp, true, path);
	}

	private List<org.sireum.aadl.ir.Emv2Propagation> outProp(ComponentInstance root, List<String> path) {
		List<ErrorPropagation> errorProp = EMV2Util.getAllOutgoingErrorPropagations(root.getComponentClassifier())
				.stream().filter(it -> (it instanceof ErrorPropagationImpl)).map(it -> (ErrorPropagationImpl) it)
				.collect(Collectors.toList());
		return errorProp2Map(errorProp, false, path);
	}

	private List<org.sireum.aadl.ir.Emv2Flow> flowSource(ComponentInstance root, List<String> path) {
		List<org.sireum.aadl.ir.Emv2Flow> sources = new ArrayList<>();
		EMV2Util.getAllErrorSources(root.getComponentClassifier()).forEach(src -> {
			String name = src.getName();
			if (src.getSourceModelElement() instanceof ErrorPropagation) {
				ErrorPropagation s = (ErrorPropagation) src.getSourceModelElement();
				DirectionType epDir = EMV2Util.getErrorPropagationFeatureDirection(s);
				String dirAdd = (epDir.incoming() && epDir.outgoing()) ? "_OUT" : "";
				String featureName = (s.getFeatureorPPRef() == null) ? s.getKind()
						: getFeatureString(s.getFeatureorPPRef()) + dirAdd;
				org.sireum.aadl.ir.Emv2Propagation prop = null;
				if (src.getTypeTokenConstraint() != null) {
					List<Name> errorP = src.getTypeTokenConstraint().getTypeTokens().stream()
							.flatMap(it -> it.getType().stream().map(et -> getErrorType(et)))
							.collect(Collectors.toList());
					prop = factory.emv2Propagation(org.sireum.aadl.ir.AadlASTJavaFactory.PropagationDirection.Out,
							factory.name(VisitorUtil.add(path, featureName), VisitorUtil.buildPosInfo(s)), errorP);
				} else {
					prop = errorProp2Map(VisitorUtil.toIList(s), false, path).get(0);
				}
				sources.add(factory.emv2Flow(factory.name(VisitorUtil.add(path, name), VisitorUtil.buildPosInfo(src)),
						org.sireum.aadl.ir.AadlASTJavaFactory.FlowKind.Source, prop, null));
			}
		});
		return sources;
	}

	private List<org.sireum.aadl.ir.Emv2Flow> flowSink(ComponentInstance root, List<String> path) {
		List<org.sireum.aadl.ir.Emv2Flow> sinks = new ArrayList<>();
		EMV2Util.getAllErrorSinks(root.getComponentClassifier()).forEach(snk -> {
			String name = snk.getName();
			DirectionType epDir = EMV2Util.getErrorPropagationFeatureDirection(snk.getIncoming());
			String dirAdd = (epDir.incoming() && epDir.outgoing()) ? "_IN" : "";
			String featureName = (snk.getIncoming().getFeatureorPPRef() == null) ? snk.getIncoming().getKind()
					: getFeatureString(snk.getIncoming().getFeatureorPPRef()) + dirAdd;
			org.sireum.aadl.ir.Emv2Propagation prop = null;
			if (snk.getTypeTokenConstraint() != null) {
				List<Name> errorP = snk.getTypeTokenConstraint().getTypeTokens().stream()
						.flatMap(it -> it.getType().stream().map(et -> getErrorType(et))).collect(Collectors.toList());
				prop = factory.emv2Propagation(org.sireum.aadl.ir.AadlASTJavaFactory.PropagationDirection.In,
						factory.name(VisitorUtil.add(path, featureName), VisitorUtil.buildPosInfo(snk)), errorP);
			} else {
				prop = errorProp2Map(VisitorUtil.toIList(snk.getIncoming()), false, path).get(0);
			}
			sinks.add(factory.emv2Flow(factory.name(VisitorUtil.add(path, name), VisitorUtil.buildPosInfo(snk)),
					org.sireum.aadl.ir.AadlASTJavaFactory.FlowKind.Source, prop, null));

		});
		return sinks;
	}

	private List<org.sireum.aadl.ir.Emv2Flow> flowPath(ComponentInstance root, List<String> path) {
		List<org.sireum.aadl.ir.Emv2Flow> paths = new ArrayList<>();
		EMV2Util.getAllErrorPaths(root.getComponentClassifier()).forEach(pth -> {
			String name = pth.getName();
			org.sireum.aadl.ir.Emv2Propagation inError = null;
			org.sireum.aadl.ir.Emv2Propagation outError = null;
			if (pth.getTypeTokenConstraint() != null) {
				DirectionType inDir = EMV2Util.getErrorPropagationFeatureDirection(pth.getIncoming());
				String inDirAdd = (inDir.incoming() && inDir.outgoing()) ? "_IN" : "";
				String pp = (pth.getIncoming().getFeatureorPPRef() == null) ? pth.getIncoming().getKind()
						: getFeatureString(pth.getIncoming().getFeatureorPPRef()) + inDirAdd;
				List<Name> errorTokens = pth.getTypeTokenConstraint().getTypeTokens().stream()
						.flatMap(it -> it.getType().stream().map(et -> getErrorType(et))).collect(Collectors.toList());
				inError = factory.emv2Propagation(org.sireum.aadl.ir.AadlASTJavaFactory.PropagationDirection.In,
						factory.name(VisitorUtil.add(path, pp), VisitorUtil.buildPosInfo(pth)), errorTokens);
			} else {
				inError = errorProp2Map(VisitorUtil.toIList(pth.getIncoming()), true, path).get(0);
			}
			if (pth.getTargetToken() != null) {
				DirectionType outDir = EMV2Util.getErrorPropagationFeatureDirection(pth.getOutgoing());
				String outDirAdd = (outDir.incoming() && outDir.outgoing()) ? "_OUT" : "";
				String pp = (pth.getOutgoing().getFeatureorPPRef() == null) ? pth.getOutgoing().getKind()
						: getFeatureString(pth.getOutgoing().getFeatureorPPRef()) + outDirAdd;
				List<Name> errorTokens = pth.getTargetToken().getTypeTokens().stream()
						.flatMap(it -> it.getType().stream().map(et -> getErrorType(et))).collect(Collectors.toList());
				outError = factory.emv2Propagation(org.sireum.aadl.ir.AadlASTJavaFactory.PropagationDirection.Out,
						factory.name(VisitorUtil.add(path, pp), VisitorUtil.buildPosInfo(pth.getOutgoing())),
						errorTokens);
			} else {
				outError = errorProp2Map(VisitorUtil.toIList(pth.getOutgoing()), false, path).get(0);
			}

			paths.add(factory.emv2Flow(factory.name(VisitorUtil.add(path, name), VisitorUtil.buildPosInfo(pth)),
					org.sireum.aadl.ir.AadlASTJavaFactory.FlowKind.Path, inError, outError));
		});
		return paths;
	}

	private Emv2BehaviorSection componentBehavior(ComponentInstance root, List<String> path) {

		List<ErrorEvent> events = EMV2Util.getAllErrorBehaviorEvents(root).stream().map(be -> {

			return factory.errorEvent(factory.name(VisitorUtil.add(path, be.getName()), VisitorUtil.buildPosInfo(be)));
		}).collect(Collectors.toList());

		List<ErrorTransition> transitions = EMV2Util.getAllErrorBehaviorTransitions(root).stream()
				.map(bt -> errorTransition(bt, path)).collect(Collectors.toList());

		List<org.sireum.aadl.ir.ErrorPropagation> propagations = EMV2Util.getAllOutgoingPropagationConditions(root)
				.stream().map(opc -> errorPropagation(opc, path)).collect(Collectors.toList());

		return factory.emv2BehaviorSection(events, transitions, propagations);
	}

	private org.sireum.aadl.ir.ErrorPropagation errorPropagation(OutgoingPropagationCondition opc, List<String> path) {
		Name id = null;
		if (opc.getName() != null) {
			List<String> cPath = VisitorUtil.add(path, opc.getName());
			id = factory.name(cPath, VisitorUtil.buildPosInfo(opc));
		}
		List<Name> source = VisitorUtil.toIList(factory.name(VisitorUtil.toIList(opc.getState().getFullName()),
				VisitorUtil.buildPosInfo(opc.getState())));

		org.sireum.aadl.ir.ErrorCondition ec = null;
		if (opc.getCondition() != null) {
			ec = errorCondition(opc.getCondition(), path);
		}

		List<org.sireum.aadl.ir.Emv2Propagation> prop = errorProp2Map(VisitorUtil.toIList(opc.getOutgoing()), false,
				path);

		return factory.errorPropagation(id, source, ec, prop);

	}

	private List<Name> getLibNames(ComponentInstance root) {
		List<Name> libNames = new ArrayList<>();
		EList<ErrorModelLibrary> el = EMV2Util.getErrorModelSubclauseWithUseTypes(root.getComponentClassifier());
		if (el != null) {
			el.forEach(e -> {
				errorLibs.put(EMV2Util.getLibraryName(e), e);
				libNames.add(
						factory.name(VisitorUtil.toIList(EMV2Util.getLibraryName(e)), VisitorUtil.buildPosInfo(e)));
			});
		}
		return libNames;
	}

	private void addAdditionalLibs() {
		List<ErrorModelLibrary> useLibs = new ArrayList<>();

		errorLibs.keySet().forEach(eln -> {
			errorLibs.get(eln).getUseTypes().forEach(use -> {
				if (!errorLibs.containsKey(EMV2Util.getLibraryName(use))) {
					useLibs.add(use);
				}
			});

			errorLibs.get(eln).getExtends().forEach(use -> {
				if (!errorLibs.containsKey(EMV2Util.getLibraryName(use))) {
					useLibs.add(use);
				}
			});

		});

		useLibs.forEach(useLib -> {
			errorLibs.put(EMV2Util.getLibraryName(useLib), useLib);
		});

	}

	public List<Emv2Library> buildLibs() {
		List<Emv2Library> emv2Libs = new ArrayList<>();
		addAdditionalLibs();
		errorLibs.keySet().forEach(eln -> {
			emv2Libs.add(emv2Lib(errorLibs.get(eln)));
		});
		return emv2Libs;
	}

	private Emv2Library emv2Lib(ErrorModelLibrary eml) {
		Name name = factory.name(VisitorUtil.toIList(EMV2Util.getLibraryName(eml)), VisitorUtil.buildPosInfo(eml));
		List<String> useTypes = eml.getUseTypes().stream().map(ut -> ut.getName()).collect(Collectors.toList());
		List<String> useExtends = eml.getExtends().stream().map(ue -> ue.getName()).collect(Collectors.toList());
		List<ErrorTypeDef> etds = eml.getTypes().stream().map(et -> errorType(et)).collect(Collectors.toList());
		List<ErrorTypeSetDef> etsds = eml.getTypesets().stream().map(ets -> errorTypeSet(ets))
				.collect(Collectors.toList());
		List<ErrorAliasDef> etads = eml.getTypes().stream().filter(et -> et.getAliasedType() != null)
				.map(et -> errorAliasType(et)).collect(Collectors.toList());
		List<ErrorAliasDef> etsads = eml.getTypesets().stream().filter(ets -> ets.getAliasedType() != null)
				.map(ets -> errorAliasTypeDef(ets)).collect(Collectors.toList());

		List<BehaveStateMachine> bsms = eml.getBehaviors().stream().map(bs -> errorBehaviorStateMachine(bs))
				.collect(Collectors.toList());

		return factory.emv2Library(name, VisitorUtil.addAll(useTypes, useExtends), etds, etsds,
				VisitorUtil.addAll(etads, etsads), bsms);

	}

	private org.sireum.aadl.ir.ErrorTypeDef errorType(ErrorType et) {
		Name etn = getErrorType(et);
		Name st = null;
		if (et.getSuperType() != null) {
			st = getErrorType(et.getSuperType());
		}

		return factory.errorTypeDef(etn, st);
	}

	private ErrorAliasDef errorAliasType(ErrorType et) {
		Name etn = getErrorType(et);
		if (et.getAliasedType() != null) {
			return factory.errorAliseDef(etn, getErrorType(et.getAliasedType()));
		} else {
			return null;
		}
	}

	private org.sireum.aadl.ir.ErrorTypeSetDef errorTypeSet(TypeSet ts) {
		Name tsn = factory.name(
				VisitorUtil.add(VisitorUtil.toIList(EMV2Util.getPrintName(EMV2Util.getContainingErrorModelLibrary(ts))),
						ts.getName()),
				VisitorUtil.buildPosInfo(ts));

		List<Name> errorTkn = ts.getTypeTokens().stream()
				.flatMap(tt -> tt.getType().stream().map(et -> getErrorType(et))).collect(Collectors.toList());

		return factory.errorTypeSetDef(tsn, errorTkn);
	}

	private ErrorAliasDef errorAliasTypeDef(TypeSet ts) {
		if (ts.getAliasedType() != null) {
			Name tsn = getErrorType(ts);
			Name atsn = getErrorType(ts.getAliasedType());
			return factory.errorAliseDef(tsn, atsn);
		} else {
			return null;
		}
	}

	private org.sireum.aadl.ir.BehaveStateMachine errorBehaviorStateMachine(ErrorBehaviorStateMachine ebsm) {

		Name id = getErrorType(ebsm);
		List<String> path = VisitorUtil.add(
				VisitorUtil.toIList(EMV2Util.getPrintName(EMV2Util.getContainingErrorModelLibrary(ebsm))),
				ebsm.getName());

		List<ErrorEvent> events = ebsm.getEvents().stream()
				.map(evnt -> factory.errorEvent(
						factory.name(VisitorUtil.add(path, evnt.getName()), VisitorUtil.buildPosInfo(evnt))))
				.collect(Collectors.toList());

		List<ErrorState> states = ebsm.getStates().stream()
				.map(st -> factory.errorState(
						factory.name(VisitorUtil.add(path, st.getName()), VisitorUtil.buildPosInfo(st)), st.isIntial()))
				.collect(Collectors.toList());

		List<ErrorTransition> transitions = ebsm.getTransitions().stream().map(trans -> errorTransition(trans, path))
				.collect(Collectors.toList());

		List<org.sireum.aadl.ir.Property> properties = ebsm.getProperties().stream().map(pa -> emv2Property(pa, path))
				.collect(Collectors.toList());

		return factory.behaveStateMachine(id, events, states, transitions, properties);

	}

	private org.sireum.aadl.ir.Property emv2Property(EMV2PropertyAssociation epa, List<String> path) {
		Property prop = epa.getProperty();
		final NamedElement cont = (NamedElement) epa.eContainer();

		List<PropertyValue> values = VisitorUtil.iList();
		try {
			PropertyExpression pe = PropertyUtils.getSimplePropertyValue(cont, prop);
			values = new Visitor().getPropertyExpressionValue(pe, path);
		} catch (Throwable t) {
			java.lang.System.err.println("Error encountered while trying to fetch property value for "
					+ prop.getQualifiedName() + " from " + cont.getQualifiedName() + " : " + t.getMessage());
		}

		return factory.property(
				factory.name(VisitorUtil.add(path, epa.getProperty().getName()), VisitorUtil.buildPosInfo(prop)),
				values);
	}

	private org.sireum.aadl.ir.ErrorTransition errorTransition(ErrorBehaviorTransition ebt, List<String> path) {
		List<String> cp = (ebt.getName() != null) ? VisitorUtil.add(path, ebt.getName()) : path;
		Name name = null;
		if (ebt.getName() != null) {
			factory.name(cp, VisitorUtil.buildPosInfo(ebt));
		}

		Name source = getStateName(ebt.getSource());
		org.sireum.aadl.ir.ErrorCondition condition = errorCondition(ebt.getCondition(), path);
		Name target = null;
		if (ebt.getTarget() != null) {
			target = getStateName(ebt.getTarget());
		} else {
			target = ebt.getDestinationBranches().stream().map(db -> getStateName(db.getTarget()))
					.collect(Collectors.toList()).get(0); // TODO: Support branching with probability
		}
		return factory.errorTransition(name, source, condition, target);
	}

	private Name getStateName(ErrorBehaviorState state) {
		List<String> libName = VisitorUtil
				.toIList(EMV2Util.getPrintName(EMV2Util.getContainingErrorModelLibrary(state)));
		List<String> bsm = eqp.getFullyQualifiedName(state.getOwner()).skipFirst(1).getSegments();
		return factory.name(VisitorUtil.addAll(libName, VisitorUtil.add(bsm, state.getName())),
				VisitorUtil.buildPosInfo(state));
	}

	private org.sireum.aadl.ir.ErrorCondition errorCondition(ConditionExpression cond, List<String> path) {
		if (cond instanceof ConditionElement) {
			ConditionElement element = (ConditionElement) cond;
			EventOrPropagation eop = EMV2Util.getErrorEventOrPropagation(element);
			if (eop instanceof ErrorPropagation) {
				ErrorPropagation ep = (ErrorPropagation) eop;
				return factory.conditionTrigger(VisitorUtil.iList(),
						errorProp2Map(VisitorUtil.toIList(ep), true, path));
			} else {

				ErrorBehaviorEvent ee = (ErrorBehaviorEvent) eop;

//				QualifiedName qn = eqp.getFullyQualifiedName(ee);
//				QualifiedName qn2 = eqp.getFullyQualifiedName(ee);
//				String qn3 = EMV2Util.getPrintName(ee);
//				String qn4 = (EMV2Util.getContainingErrorModelSubclause(ee) != null)
//						? EMV2Util.getContainingErrorModelSubclause(ee).getFullName()
//						: EMV2Util.getContainingErrorModelLibrary(ee).getFullName();
//				QualifiedName qn5 = eqp.getFullyQualifiedName(ee.getOwner()).skipFirst(1);

				return factory.conditionTrigger(
						VisitorUtil.toIList(
								factory.name(VisitorUtil.add(path, ee.getName()), VisitorUtil.buildPosInfo(ee))),
						VisitorUtil.iList());
			}
		} else if (cond instanceof AndExpression) {
			AndExpression and = (AndExpression) cond;
			return factory.andCondition(
					and.getOperands().stream().map(ao -> errorCondition(ao, path)).collect(Collectors.toList()));
		} else if (cond instanceof OrExpression) {
			OrExpression or = (OrExpression) cond;
			return factory.orCondition(
					or.getOperands().stream().map(ao -> errorCondition(ao, path)).collect(Collectors.toList()));
		} else if (cond instanceof OrmoreExpression) {
			OrmoreExpression or = (OrmoreExpression) cond;
			int count = new Long(or.getCount()).intValue();
			return factory.orMoreCondition(count,
					or.getOperands().stream().map(ao -> errorCondition(ao, path)).collect(Collectors.toList()));

		} else if (cond instanceof OrlessExpression) {
			OrlessExpression or = (OrlessExpression) cond;
			int count = new Long(or.getCount()).intValue();
			return factory.orLessCondition(count,
					or.getOperands().stream().map(ao -> errorCondition(ao, path)).collect(Collectors.toList()));
		} else if (cond instanceof AllExpression) {
			AllExpression or = (AllExpression) cond;
			return factory.allCondition(
					or.getOperands().stream().map(ao -> errorCondition(ao, path)).collect(Collectors.toList()));
		}
		return null;
	}

}
