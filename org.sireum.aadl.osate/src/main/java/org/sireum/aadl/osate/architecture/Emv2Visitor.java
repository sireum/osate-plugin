package org.sireum.aadl.osate.architecture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.osate.xtext.aadl2.errormodel.errorModel.EMV2Path;
import org.osate.xtext.aadl2.errormodel.errorModel.EMV2PropertyAssociation;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorBehaviorEvent;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorBehaviorState;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorBehaviorStateMachine;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorBehaviorTransition;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorModelLibrary;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorModelSubclause;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorPath;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorPropagation;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorSink;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorSource;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorType;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorTypes;
import org.osate.xtext.aadl2.errormodel.errorModel.EventOrPropagation;
import org.osate.xtext.aadl2.errormodel.errorModel.FeatureorPPReference;
import org.osate.xtext.aadl2.errormodel.errorModel.OrExpression;
import org.osate.xtext.aadl2.errormodel.errorModel.OrlessExpression;
import org.osate.xtext.aadl2.errormodel.errorModel.OrmoreExpression;
import org.osate.xtext.aadl2.errormodel.errorModel.OutgoingPropagationCondition;
import org.osate.xtext.aadl2.errormodel.errorModel.TypeSet;
import org.osate.xtext.aadl2.errormodel.errorModel.impl.ErrorPropagationImpl;
import org.osate.xtext.aadl2.errormodel.util.EMV2Properties;
import org.osate.xtext.aadl2.errormodel.util.EMV2Util;
import org.osate.xtext.aadl2.naming.Aadl2QualifiedNameProvider;
import org.sireum.hamr.ir.Annex;
import org.sireum.hamr.ir.AnnexLib;
import org.sireum.hamr.ir.BehaveStateMachine;
import org.sireum.hamr.ir.Emv2BehaviorSection;
import org.sireum.hamr.ir.Emv2Library;
import org.sireum.hamr.ir.ErrorAliasDef;
import org.sireum.hamr.ir.ErrorTransition;
import org.sireum.hamr.ir.ErrorTypeDef;
import org.sireum.hamr.ir.ErrorTypeSetDef;
import org.sireum.hamr.ir.Name;
public class Emv2Visitor {

	final String ALL_STATE = "all";

	final org.sireum.hamr.ir.AadlASTFactory factory = new org.sireum.hamr.ir.AadlASTFactory();
	final LinkedHashMap<String, ErrorModelLibrary> errorLibs = new LinkedHashMap<>();
	final Aadl2QualifiedNameProvider eqp = new Aadl2QualifiedNameProvider();

	private Visitor coreVisitor = null;

	public Emv2Visitor(Visitor visitor) {
		this.coreVisitor = visitor;
	}

	public Annex visitEmv2Comp(ComponentInstance root, List<String> path) {
		return new Annex("Emv2",
				factory.emv2Clause(getLibNames(root), VisitorUtil.addAll(inProp(root, path), outProp(root, path)),
						VisitorUtil.addAll(flowSource(root, path),
								VisitorUtil.addAll(flowPath(root, path), flowSink(root, path))),
						componentBehavior(root, path), buildEmv2Property(root, path)));
	}

	private org.sireum.hamr.ir.ElementRef buildEmv2ElemRef(EMV2Path emv2path, ComponentInstance root) {
		NamedElement ne = EMV2Util.getErrorModelElement(emv2path);
		List<String> path = Arrays
				.asList(EMV2Util.getLastComponentInstance(emv2path, root).getInstanceObjectPath().split("//."));
		if (ne instanceof ErrorSource) {
			ErrorSource es = (ErrorSource) ne;
			return factory.emv2ElementRef(org.sireum.hamr.ir.AadlASTJavaFactory.Emv2ElementKind.Source,
					factory.name(VisitorUtil.add(path, EMV2Util.getPrintName(ne)), VisitorUtil.buildPosInfo(ne)),
					VisitorUtil.iList());
		} else if (ne instanceof ErrorSink) {
			ErrorSink es = (ErrorSink) ne;
			return factory.emv2ElementRef(org.sireum.hamr.ir.AadlASTJavaFactory.Emv2ElementKind.Sink,
					factory.name(VisitorUtil.add(path, EMV2Util.getPrintName(ne)), VisitorUtil.buildPosInfo(ne)),
					VisitorUtil.iList());
		} else if (ne instanceof ErrorPath) {
			ErrorPath es = (ErrorPath) ne;
			return factory.emv2ElementRef(org.sireum.hamr.ir.AadlASTJavaFactory.Emv2ElementKind.Path,
					factory.name(VisitorUtil.add(path, EMV2Util.getPrintName(ne)), VisitorUtil.buildPosInfo(ne)),
					VisitorUtil.iList());
		} else if (ne instanceof ErrorPropagation) {
			ErrorPropagation es = (ErrorPropagation) ne;
			String pathName = EMV2Util.getPrintName(emv2path);
			String pathName2 = (es.getFeatureorPPRef() != null) ? getFeatureString(es.getFeatureorPPRef())
					: es.getKind();
			if ((es.getFeatureorPPRef() == null) && (es.getDirection().incoming())) {
				pathName2 = pathName2 + "_IN";
			} else if ((es.getFeatureorPPRef() == null) && (es.getDirection().outgoing())) {
				pathName2 = pathName2 + "_OUT";
			}
			// System.out.println(pathName2);
			// es.getFeatureorPPRef()
			List<Name> errorTypes = new ArrayList<Name>();
			ErrorTypes ets = EMV2Util.getErrorType(emv2path) != null ? EMV2Util.getErrorType(emv2path)
					: emv2path.getEmv2Target().getErrorType();
			if (ets != null) {
				if (ets instanceof TypeSet) {
					TypeSet ts = (TypeSet) ets;
					ts.getTypeTokens().forEach(tt -> {
						tt.getType().forEach(t -> {
							Optional<Name> errorTypeOpt = getErrorType(t);
							if(errorTypeOpt.isPresent()) {
								errorTypes.add(errorTypeOpt.get());
							}
						});
					});
				} else {
					ErrorType et = (ErrorType) ets;
					Optional<Name> errorTypeOpt = getErrorType(et);
					if(errorTypeOpt.isPresent()) {
						errorTypes.add(errorTypeOpt.get());
					}
				}
			}
			return factory.emv2ElementRef(org.sireum.hamr.ir.AadlASTJavaFactory.Emv2ElementKind.Propagation,
					factory.name(VisitorUtil.add(path, pathName2), VisitorUtil.buildPosInfo(ne)),
					errorTypes);
		} else if (ne instanceof ErrorBehaviorState) {
			ErrorBehaviorState es = (ErrorBehaviorState) ne;
			return factory.emv2ElementRef(org.sireum.hamr.ir.AadlASTJavaFactory.Emv2ElementKind.State,
					factory.name(VisitorUtil.add(path, EMV2Util.getPrintName(ne)), VisitorUtil.buildPosInfo(ne)),
					VisitorUtil.iList());
		} else if (ne instanceof ErrorBehaviorEvent) {
			ErrorBehaviorEvent es = (ErrorBehaviorEvent) ne;
			return factory.emv2ElementRef(org.sireum.hamr.ir.AadlASTJavaFactory.Emv2ElementKind.Event,
					factory.name(VisitorUtil.add(path, EMV2Util.getPrintName(ne)), VisitorUtil.buildPosInfo(ne)),
					VisitorUtil.iList());
		} else if (ne instanceof ErrorBehaviorTransition) {
			ErrorBehaviorTransition es = (ErrorBehaviorTransition) ne;
			return factory.emv2ElementRef(org.sireum.hamr.ir.AadlASTJavaFactory.Emv2ElementKind.Event,
					factory.name(VisitorUtil.add(path, EMV2Util.getPrintName(ne)), VisitorUtil.buildPosInfo(ne)),
					VisitorUtil.iList());
		} else {
			System.out.println("not matched :" + ne.getClass().getName());
			return null;
		}
	}

	private List<org.sireum.hamr.ir.Property> buildEmv2Property(ComponentInstance root, List<String> path) {
		EList<ErrorModelSubclause> emscs = EMV2Util.getAllContainingClassifierEMV2Subclauses(root);
		List<EMV2PropertyAssociation> pas = new ArrayList<EMV2PropertyAssociation>();

		emscs.forEach(emsc -> {
			pas.addAll(EMV2Properties.getPropertyAssociationListInContext(emsc));
		});
		List<org.sireum.hamr.ir.Property> res = pas.stream().map(pa -> {
			final Property prop = pa.getProperty();
			List<org.sireum.hamr.ir.ElementRef> elems = pa.getEmv2Path().stream().map(ep -> {
				org.sireum.hamr.ir.ElementRef eRes = buildEmv2ElemRef(ep, root);
				return eRes;
			}).collect(Collectors.toList());
			final List<String> currentPath = VisitorUtil.add(path, prop.getQualifiedName());
			final NamedElement cont = (NamedElement) pa.eContainer();
			List<org.sireum.hamr.ir.PropertyValue> propertyValues = VisitorUtil.iList();
			try {
				PropertyExpression pe = EMV2Properties.getPropertyValue(pa);
				propertyValues = coreVisitor.getPropertyExpressionValue(pe, path);
			} catch (Throwable t) {
				java.lang.System.err.println("Error encountered while trying to fetch property value for "
						+ prop.getQualifiedName() + " from " + cont.getQualifiedName() + " : " + t.getMessage());
			}
			return factory.property(factory.name(currentPath, VisitorUtil.buildPosInfo(prop)), propertyValues, elems);
		}).collect(Collectors.toList());

//				pas.stream().map(pa -> {
//			List<String> appliesTo = new ArrayList();
//			final Property prop = pa.getProperty();
//			pa.getEmv2Path().forEach(ep -> {
//				// String val = path;
//				List<String> fqp = VisitorUtil.iList();
//				ComponentInstance relativeCI = EMV2Util.getLastComponentInstance(ep, root);
//				if (relativeCI != null) {
//					fqp.addAll(Arrays
//							.asList(EMV2Util.getLastComponentInstance(ep, root).getInstanceObjectPath().split("//.")));
//				} else {
//					fqp.addAll(path);
//				}
//				appliesTo.add(EMV2Util.getPrintName(EMV2Util.getErrorModelElement(ep)));
//			});
//			final List<String> currentPath = VisitorUtil.add(path, prop.getQualifiedName());
//			final NamedElement cont = (NamedElement) pa.eContainer();
//			List<org.sireum.hamr.ir.PropertyValue> propertyValues = VisitorUtil.iList();
//			try {
//				PropertyExpression pe = EMV2Properties.getPropertyValue(pa);
//				propertyValues = coreVisitor.getPropertyExpressionValue(pe, path);
//			} catch (Throwable t) {
//				java.lang.System.err.println("Error encountered while trying to fetch property value for "
//						+ prop.getQualifiedName() + " from " + cont.getQualifiedName() + " : " + t.getMessage());
//			}
//			return factory.property(factory.name(currentPath, VisitorUtil.buildPosInfo(prop)), propertyValues,
//					appliesTo);
//		}).collect(Collectors.toList());
		return res;
	}

	private List<org.sireum.hamr.ir.Emv2Propagation> errorProp2Map(List<ErrorPropagation> errorProp, boolean isIn,
			List<String> path) {
		List<org.sireum.hamr.ir.Emv2Propagation> prop = new ArrayList<>();
		errorProp.stream().forEach(ep -> {
			List<Name> inErrorTokens = new ArrayList<>();
			ep.getTypeSet().getTypeTokens().forEach(tt -> {
				tt.getType().forEach(t -> {
					Optional<Name> errorTypeOpt = getErrorType(t);
					if(errorTypeOpt.isPresent()) {
						inErrorTokens.add(errorTypeOpt.get());
					}
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
			org.sireum.hamr.ir.AadlASTJavaFactory.PropagationDirection dir = isIn
					? org.sireum.hamr.ir.AadlASTJavaFactory.PropagationDirection.In
					: org.sireum.hamr.ir.AadlASTJavaFactory.PropagationDirection.Out;
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

	private Optional<Name> getErrorType(NamedElement error) {

		if (EMV2Util.getContainingErrorModelLibrary(error) != null) {
		return Optional.of(factory.name(VisitorUtil.add(
				VisitorUtil.toIList(EMV2Util.getPrintName(EMV2Util.getContainingErrorModelLibrary(error))),
				error.getName()), VisitorUtil.buildPosInfo(error)));
		} else {
			return Optional.empty();
		}
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

	private List<org.sireum.hamr.ir.Emv2Propagation> inProp(ComponentInstance root, List<String> path) {
		List<ErrorPropagation> errorProp = EMV2Util.getAllIncomingErrorPropagations(root).stream()
				.filter(it -> (it instanceof ErrorPropagationImpl)).map(it -> (ErrorPropagationImpl) it)
				.collect(Collectors.toList());
		return errorProp2Map(errorProp, true, path);
	}

	private List<org.sireum.hamr.ir.Emv2Propagation> outProp(ComponentInstance root, List<String> path) {
		List<ErrorPropagation> errorProp = EMV2Util.getAllOutgoingErrorPropagations(root.getComponentClassifier())
				.stream().filter(it -> (it instanceof ErrorPropagationImpl)).map(it -> (ErrorPropagationImpl) it)
				.collect(Collectors.toList());
		return errorProp2Map(errorProp, false, path);
	}

	private List<org.sireum.hamr.ir.Emv2Flow> flowSource(ComponentInstance root, List<String> path) {
		List<org.sireum.hamr.ir.Emv2Flow> sources = new ArrayList<>();

		EMV2Util.getAllErrorSources(root.getComponentClassifier()).forEach(src -> {
			String name = src.getName();
			if (src.getSourceModelElement() instanceof ErrorPropagation) {
				ErrorPropagation s = (ErrorPropagation) src.getSourceModelElement();
				DirectionType epDir = EMV2Util.getErrorPropagationFeatureDirection(s);
				String dirAdd = (epDir.incoming() && epDir.outgoing()) ? "_OUT" : "";
				String featureName = (s.getFeatureorPPRef() == null) ? s.getKind()
						: getFeatureString(s.getFeatureorPPRef()) + dirAdd;
				org.sireum.hamr.ir.Emv2Propagation prop = null;
				if (src.getTypeTokenConstraint() != null) {
					List<Name> errorP = src.getTypeTokenConstraint().getTypeTokens().stream()
							.flatMap(it -> it.getType().stream()
									.flatMap(et -> getErrorType(et).isPresent() ? Stream.of(getErrorType(et).get())
											: Stream.empty()))
							.collect(Collectors.toList());
					prop = factory.emv2Propagation(org.sireum.hamr.ir.AadlASTJavaFactory.PropagationDirection.Out,
							factory.name(VisitorUtil.add(path, featureName), VisitorUtil.buildPosInfo(s)), errorP);
				} else {
					prop = errorProp2Map(VisitorUtil.toIList(s), false, path).get(0);
				}
				sources.add(factory.emv2Flow(factory.name(VisitorUtil.add(path, name), VisitorUtil.buildPosInfo(src)),
						org.sireum.hamr.ir.AadlASTJavaFactory.FlowKind.Source, null, prop,
						VisitorUtil.getUriFragment(src)));
			}
		});
		return sources;
	}

	private List<org.sireum.hamr.ir.Emv2Flow> flowSink(ComponentInstance root, List<String> path) {
		List<org.sireum.hamr.ir.Emv2Flow> sinks = new ArrayList<>();
		EMV2Util.getAllErrorSinks(root.getComponentClassifier()).forEach(snk -> {
			String name = snk.getName();
			DirectionType epDir = EMV2Util.getErrorPropagationFeatureDirection(snk.getIncoming());
			String dirAdd = (epDir.incoming() && epDir.outgoing()) ? "_IN" : "";
			String featureName = (snk.getIncoming().getFeatureorPPRef() == null) ? snk.getIncoming().getKind()
					: getFeatureString(snk.getIncoming().getFeatureorPPRef()) + dirAdd;
			org.sireum.hamr.ir.Emv2Propagation prop = null;
			if (snk.getTypeTokenConstraint() != null) {
				List<Name> errorP = snk.getTypeTokenConstraint().getTypeTokens().stream()
						.flatMap(it -> it.getType().stream()
								.flatMap(et -> getErrorType(et).isPresent() ? Stream.of(getErrorType(et).get())
										: Stream.empty()))
						.collect(Collectors.toList());
				prop = factory.emv2Propagation(org.sireum.hamr.ir.AadlASTJavaFactory.PropagationDirection.In,
						factory.name(VisitorUtil.add(path, featureName), VisitorUtil.buildPosInfo(snk)), errorP);
			} else {
				prop = errorProp2Map(VisitorUtil.toIList(snk.getIncoming()), false, path).get(0);
			}
			sinks.add(factory.emv2Flow(factory.name(VisitorUtil.add(path, name), VisitorUtil.buildPosInfo(snk)),
					org.sireum.hamr.ir.AadlASTJavaFactory.FlowKind.Sink, prop, null, VisitorUtil.getUriFragment(snk)));

		});
		return sinks;
	}

	private List<org.sireum.hamr.ir.Emv2Flow> flowPath(ComponentInstance root, List<String> path) {
		List<org.sireum.hamr.ir.Emv2Flow> paths = new ArrayList<>();
		EMV2Util.getAllErrorPaths(root.getComponentClassifier()).forEach(pth -> {
			String name = pth.getName();
			org.sireum.hamr.ir.Emv2Propagation inError = null;
			org.sireum.hamr.ir.Emv2Propagation outError = null;
			if (pth.getTypeTokenConstraint() != null && !pth.isAllIncoming()) {
				DirectionType inDir = EMV2Util.getErrorPropagationFeatureDirection(pth.getIncoming());
				String inDirAdd = (inDir.incoming() && inDir.outgoing()) ? "_IN" : "";
				String pp = (pth.getIncoming().getFeatureorPPRef() == null) ? pth.getIncoming().getKind()
						: getFeatureString(pth.getIncoming().getFeatureorPPRef()) + inDirAdd;
				List<Name> errorTokens = pth.getTypeTokenConstraint().getTypeTokens().stream()
						.flatMap(it -> it.getType().stream()
								.flatMap(et -> getErrorType(et).isPresent() ? Stream.of(getErrorType(et).get())
										: Stream.empty()))
						.collect(Collectors.toList());
				inError = factory.emv2Propagation(org.sireum.hamr.ir.AadlASTJavaFactory.PropagationDirection.In,
						factory.name(VisitorUtil.add(path, pp), VisitorUtil.buildPosInfo(pth)), errorTokens);
			} else if (pth.isAllIncoming()) {
				ErrorPropagation ep = EMV2Util.getErrorPropagation((EMV2Path) pth);
				// System.out.println(ep);
			} else {
				inError = errorProp2Map(VisitorUtil.toIList(pth.getIncoming()), true, path).get(0);
			}
			if (pth.getTargetToken() != null && !pth.isAllOutgoing()) {
				DirectionType outDir = EMV2Util.getErrorPropagationFeatureDirection(pth.getOutgoing());
				String outDirAdd = (outDir.incoming() && outDir.outgoing()) ? "_OUT" : "";
				String pp = (pth.getOutgoing().getFeatureorPPRef() == null) ? pth.getOutgoing().getKind()
						: getFeatureString(pth.getOutgoing().getFeatureorPPRef()) + outDirAdd;
				List<Name> errorTokens = pth.getTargetToken().getTypeTokens().stream()
						.flatMap(it -> it.getType().stream()
								.flatMap(et -> getErrorType(et).isPresent() ? Stream.of(getErrorType(et).get())
										: Stream.empty()))
						.collect(Collectors.toList());
				outError = factory.emv2Propagation(org.sireum.hamr.ir.AadlASTJavaFactory.PropagationDirection.Out,
						factory.name(VisitorUtil.add(path, pp), VisitorUtil.buildPosInfo(pth.getOutgoing())),
						errorTokens);
			} else if (pth.isAllOutgoing()) {
				Collection<ErrorPropagation> ep = EMV2Util.getOutgoingPropagationOrAll(pth);
				// System.out.println(ep);
			} else {
				outError = errorProp2Map(VisitorUtil.toIList(pth.getOutgoing()), false, path).get(0);
			}

			paths.add(factory.emv2Flow(factory.name(VisitorUtil.add(path, name), VisitorUtil.buildPosInfo(pth)),
					org.sireum.hamr.ir.AadlASTJavaFactory.FlowKind.Path, inError, outError,
					VisitorUtil.getUriFragment(pth)));
		});
		return paths;
	}

	private Emv2BehaviorSection componentBehavior(ComponentInstance root, List<String> path) {

		List<org.sireum.hamr.ir.ErrorEvent> events = EMV2Util.getAllErrorBehaviorEvents(root).stream().map(be -> {

			return factory.errorEvent(factory.name(VisitorUtil.add(path, be.getName()), VisitorUtil.buildPosInfo(be)));
		}).collect(Collectors.toList());

		List<ErrorTransition> transitions = EMV2Util.getAllErrorBehaviorTransitions(root).stream()
				.map(bt -> errorTransition(bt, path)).collect(Collectors.toList());

		List<org.sireum.hamr.ir.ErrorPropagation> propagations = EMV2Util.getAllOutgoingPropagationConditions(root)
				.stream().map(opc -> errorPropagation(opc, path)).collect(Collectors.toList());

		return factory.emv2BehaviorSection(events, transitions, propagations);
	}

	private org.sireum.hamr.ir.ErrorPropagation errorPropagation(OutgoingPropagationCondition opc, List<String> path) {
		Name id = null;
		if (opc.getName() != null) {
			List<String> cPath = VisitorUtil.add(path, opc.getName());
			id = factory.name(cPath, VisitorUtil.buildPosInfo(opc));
		}

		List<Name> source = VisitorUtil.iList();
		if (!opc.isAllStates()) {
			source = VisitorUtil.toIList(factory.name(VisitorUtil.toIList(opc.getState().getFullName()),
					VisitorUtil.buildPosInfo(opc.getState())));
		} else {
			source = EMV2Util.getAllErrorBehaviorStates(opc.getContainingComponentImpl()).stream()
					.map(ebs -> getStateName(ebs)).collect(Collectors.toList());
		}
		org.sireum.hamr.ir.ErrorCondition ec = null;
		if (opc.getCondition() != null) {
			ec = errorCondition(opc.getCondition(), path);
		}
		List<org.sireum.hamr.ir.Emv2Propagation> prop = VisitorUtil.iList();

		if (!opc.isAllPropagations()) {
			prop = errorProp2Map(VisitorUtil.toIList(opc.getOutgoing()), false, path);
		} else {
			List<ErrorPropagation> errorProp = EMV2Util
					.getAllOutgoingErrorPropagations(opc.getContainingComponentImpl()).stream()
					.filter(it -> (it instanceof ErrorPropagationImpl)).map(it -> (ErrorPropagationImpl) it)
					.collect(Collectors.toList());
			prop = errorProp2Map(errorProp, false, path);
		}
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

		EMV2Util.getAllContainingClassifierEMV2Subclauses(root).forEach(clause -> {
			Name tempName = null;
			if (clause.getUseBehavior() != null) {
				tempName = factory.name(
						VisitorUtil.toIList(clause.getUseBehavior().getElementRoot().getName() + "."
								+ clause.getUseBehavior().getQualifiedName()),
						VisitorUtil.buildPosInfo(clause.getUseBehavior()));
				ErrorModelLibrary el2 = EMV2Util.getContainingErrorModelLibrary(clause.getUseBehavior());
				if (el2 != null) {
					errorLibs.put(EMV2Util.getLibraryName(el2), el2);
					libNames.add(factory.name(VisitorUtil.toIList(EMV2Util.getLibraryName(el2)),
							VisitorUtil.buildPosInfo(el2)));

				}
			}

			if (tempName != null && !libNames.contains(tempName)) {
				libNames.add(tempName);
			}
		});
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

	public List<AnnexLib> buildLibs() {
		List<AnnexLib> emv2Libs = new ArrayList<>();
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

	private org.sireum.hamr.ir.ErrorTypeDef errorType(ErrorType et) {
		Name etn = getErrorType(et).get();
		Name st = null;
		if (et.getSuperType() != null) {
			st = getErrorType(et.getSuperType()).get();
		}

		return factory.errorTypeDef(etn, st, VisitorUtil.getUriFragment(et));
	}

	private ErrorAliasDef errorAliasType(ErrorType et) {
		Name etn = getErrorType(et).get();
		if (et.getAliasedType() != null) {
			return factory.errorAliseDef(etn, getErrorType(et.getAliasedType()).get());
		} else {
			return null;
		}
	}

	private org.sireum.hamr.ir.ErrorTypeSetDef errorTypeSet(TypeSet ts) {
		Name tsn = factory.name(
				VisitorUtil.add(VisitorUtil.toIList(EMV2Util.getPrintName(EMV2Util.getContainingErrorModelLibrary(ts))),
						ts.getName()),
				VisitorUtil.buildPosInfo(ts));

		List<Name> errorTkn = ts.getTypeTokens().stream()
				.flatMap(tt -> tt.getType().stream().flatMap(
						et -> getErrorType(et).isPresent() ? Stream.of(getErrorType(et).get()) : Stream.empty()))
				.collect(Collectors.toList());

		return factory.errorTypeSetDef(tsn, errorTkn);
	}

	private ErrorAliasDef errorAliasTypeDef(TypeSet ts) {
		if (ts.getAliasedType() != null) {
			Name tsn = getErrorType(ts).get();
			Name atsn = getErrorType(ts.getAliasedType()).get();
			return factory.errorAliseDef(tsn, atsn);
		} else {
			return null;
		}
	}

	private org.sireum.hamr.ir.BehaveStateMachine errorBehaviorStateMachine(ErrorBehaviorStateMachine ebsm) {

		Name id = getErrorType(ebsm).get();
		List<String> path = VisitorUtil.add(
				VisitorUtil.toIList(EMV2Util.getPrintName(EMV2Util.getContainingErrorModelLibrary(ebsm))),
				ebsm.getName());

		List<org.sireum.hamr.ir.ErrorEvent> events = ebsm.getEvents().stream()
				.map(evnt -> factory.errorEvent(
						factory.name(VisitorUtil.add(path, evnt.getName()), VisitorUtil.buildPosInfo(evnt))))
				.collect(Collectors.toList());

		List<org.sireum.hamr.ir.ErrorState> states = ebsm.getStates().stream()
				.map(st -> factory.errorState(
						factory.name(VisitorUtil.add(path, st.getName()), VisitorUtil.buildPosInfo(st)), st.isIntial()))
				.collect(Collectors.toList());

		List<ErrorTransition> transitions = ebsm.getTransitions().stream().map(trans -> errorTransition(trans, path))
				.collect(Collectors.toList());

		List<org.sireum.hamr.ir.Property> properties = VisitorUtil.iList();// ebsm.getProperties().stream().map(pa -> emv2Property(pa,
																			// path)).collect(Collectors.toList());

		return factory.behaveStateMachine(id, events, states, transitions, properties);

	}

//	private org.sireum.hamr.ir.Property emv2Property(EMV2PropertyAssociation epa, List<String> path) {
//		Property prop = epa.getProperty();
//		final NamedElement cont = (NamedElement) epa.eContainer();
//
//		List<PropertyValue> values = VisitorUtil.iList();
//		try {
//			PropertyExpression pe = PropertyUtils.getSimplePropertyValue(cont, prop);
//			values = new Visitor().getPropertyExpressionValue(pe, path);
//		} catch (Throwable t) {
//			java.lang.System.err.println("Error encountered while trying to fetch property value for "
//					+ prop.getQualifiedName() + " from " + cont.getQualifiedName() + " : " + t.getMessage());
//		}
//
//		return factory.property(
//				factory.name(VisitorUtil.add(path, epa.getProperty().getName()), VisitorUtil.buildPosInfo(prop)),
//				values);
//	}

	private org.sireum.hamr.ir.ErrorTransition errorTransition(ErrorBehaviorTransition ebt, List<String> path) {
		List<String> cp = (ebt.getName() != null) ? VisitorUtil.add(path, ebt.getName()) : path;
		Name name = null;
		if (ebt.getName() != null) {
			factory.name(cp, VisitorUtil.buildPosInfo(ebt));
		}

		Name source = null;
		if (ebt.isAllStates()) {
			if (ebt.getOwner() instanceof ComponentInstance
					&& EMV2Util.getAllErrorBehaviorStates((ComponentInstance) ebt.getOwner()).isEmpty()) {
				source = getStateName(EMV2Util.getAllErrorBehaviorStates((ComponentInstance) ebt.getOwner()).stream()
						.findFirst().get());
			}
			source = factory.name(VisitorUtil.add(VisitorUtil.iList(), ALL_STATE), VisitorUtil.buildPosInfo(ebt));
		} else {
			source = getStateName(ebt.getSource());
		}
		org.sireum.hamr.ir.ErrorCondition condition = errorCondition(ebt.getCondition(), path);
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

	private org.sireum.hamr.ir.ErrorCondition errorCondition(ConditionExpression cond, List<String> path) {
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