package org.sireum.aadl.osate.architecture;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.osate.aadl2.DataClassifier;
import org.osate.aadl2.Port;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.IS;
import org.sireum.Option;
import org.sireum.Z;
import org.sireum.Z$;
import org.sireum.aadl.ir.Annex;
import org.sireum.aadl.ir.Annex$;
import org.sireum.aadl.ir.BLESSIntConst;
import org.sireum.aadl.ir.BTSAccessExp;
import org.sireum.aadl.ir.BTSAccessExp$;
import org.sireum.aadl.ir.BTSAction;
import org.sireum.aadl.ir.BTSAssertedAction;
import org.sireum.aadl.ir.BTSAssertedAction$;
import org.sireum.aadl.ir.BTSAssertion;
import org.sireum.aadl.ir.BTSAssertion$;
import org.sireum.aadl.ir.BTSAssignmentAction;
import org.sireum.aadl.ir.BTSAssignmentAction$;
import org.sireum.aadl.ir.BTSBLESSAnnexClause;
import org.sireum.aadl.ir.BTSBLESSAnnexClause$;
import org.sireum.aadl.ir.BTSBehaviorActions;
import org.sireum.aadl.ir.BTSBehaviorActions$;
import org.sireum.aadl.ir.BTSBehaviorTime;
import org.sireum.aadl.ir.BTSBehaviorTime$;
import org.sireum.aadl.ir.BTSBinaryExp;
import org.sireum.aadl.ir.BTSBinaryExp$;
import org.sireum.aadl.ir.BTSBinaryOp;
import org.sireum.aadl.ir.BTSClassifier$;
import org.sireum.aadl.ir.BTSConditionalActions;
import org.sireum.aadl.ir.BTSConditionalActions$;
import org.sireum.aadl.ir.BTSDispatchCondition;
import org.sireum.aadl.ir.BTSDispatchCondition$;
import org.sireum.aadl.ir.BTSDispatchConjunction;
import org.sireum.aadl.ir.BTSDispatchConjunction$;
import org.sireum.aadl.ir.BTSDispatchTrigger;
import org.sireum.aadl.ir.BTSDispatchTriggerPort$;
import org.sireum.aadl.ir.BTSDispatchTriggerStop;
import org.sireum.aadl.ir.BTSDispatchTriggerTimeout$;
import org.sireum.aadl.ir.BTSExecuteCondition;
import org.sireum.aadl.ir.BTSExecuteConditionExp$;
import org.sireum.aadl.ir.BTSExecuteConditionOtherwise$;
import org.sireum.aadl.ir.BTSExecuteConditionTimeout$;
import org.sireum.aadl.ir.BTSExecutionOrder;
import org.sireum.aadl.ir.BTSExistentialLatticeQuantification;
import org.sireum.aadl.ir.BTSExistentialLatticeQuantification$;
import org.sireum.aadl.ir.BTSExp;
import org.sireum.aadl.ir.BTSFormalExpPair;
import org.sireum.aadl.ir.BTSFormalExpPair$;
import org.sireum.aadl.ir.BTSFrozenPortAction$;
import org.sireum.aadl.ir.BTSFunctionCall;
import org.sireum.aadl.ir.BTSFunctionCall$;
import org.sireum.aadl.ir.BTSGuardedAction;
import org.sireum.aadl.ir.BTSGuardedAction$;
import org.sireum.aadl.ir.BTSIfBAAction;
import org.sireum.aadl.ir.BTSIfBAAction$;
import org.sireum.aadl.ir.BTSIfBLESSAction;
import org.sireum.aadl.ir.BTSIfBLESSAction$;
import org.sireum.aadl.ir.BTSInternalCondition;
import org.sireum.aadl.ir.BTSLiteralExp$;
import org.sireum.aadl.ir.BTSLiteralType;
import org.sireum.aadl.ir.BTSModeCondition;
import org.sireum.aadl.ir.BTSNameExp;
import org.sireum.aadl.ir.BTSNameExp$;
import org.sireum.aadl.ir.BTSPortInAction;
import org.sireum.aadl.ir.BTSPortInAction$;
import org.sireum.aadl.ir.BTSPortOutAction;
import org.sireum.aadl.ir.BTSPortOutAction$;
import org.sireum.aadl.ir.BTSSkipAction$;
import org.sireum.aadl.ir.BTSStateCategory;
import org.sireum.aadl.ir.BTSStateDeclaration;
import org.sireum.aadl.ir.BTSStateDeclaration$;
import org.sireum.aadl.ir.BTSSubprogramCallAction$;
import org.sireum.aadl.ir.BTSTransition;
import org.sireum.aadl.ir.BTSTransition$;
import org.sireum.aadl.ir.BTSTransitionCondition;
import org.sireum.aadl.ir.BTSTransitionLabel;
import org.sireum.aadl.ir.BTSTransitionLabel$;
import org.sireum.aadl.ir.BTSType;
import org.sireum.aadl.ir.BTSUnaryExp$;
import org.sireum.aadl.ir.BTSUnaryOp;
import org.sireum.aadl.ir.BTSUniversalLatticeQuantification;
import org.sireum.aadl.ir.BTSUniversalLatticeQuantification$;
import org.sireum.aadl.ir.BTSVariableCategory;
import org.sireum.aadl.ir.BTSVariableDeclaration;
import org.sireum.aadl.ir.BTSVariableDeclaration$;
import org.sireum.aadl.ir.Classifier;
import org.sireum.aadl.ir.Classifier$;
import org.sireum.aadl.ir.Name;
import org.sireum.aadl.ir.TODO;

import edu.ksu.bless.assertion.assertion.Assertion;
import edu.ksu.bless.assertion.assertion.NumericConstant;
import edu.ksu.bless.assertion.assertion.PartialName;
import edu.ksu.bless.assertion.assertion.Type;
import edu.ksu.bless.assertion.assertion.ValueConstant;
import edu.ksu.bless.assertion.assertion.util.AssertionSwitch;
import edu.ksu.bless.bless.bLESS.BLESSAnnexSubclause;
import edu.ksu.bless.bless.bLESS.BehaviorState;
import edu.ksu.bless.bless.bLESS.BehaviorTransition;
import edu.ksu.bless.bless.bLESS.BehaviorVariable;
import edu.ksu.bless.bless.bLESS.Declarator;
import edu.ksu.bless.bless.bLESS.DispatchCondition;
import edu.ksu.bless.bless.bLESS.DispatchConjunction;
import edu.ksu.bless.bless.bLESS.DispatchTrigger;
import edu.ksu.bless.bless.bLESS.ExecuteCondition;
import edu.ksu.bless.bless.bLESS.InternalCondition;
import edu.ksu.bless.bless.bLESS.ModeCondition;
import edu.ksu.bless.bless.bLESS.TransitionLabel;
import edu.ksu.bless.bless.bLESS.impl.BLESSAnnexSubclauseImpl;
import edu.ksu.bless.bless.bLESS.util.BLESSSwitch;
import edu.ksu.bless.subbless.subBLESS.Alternative;
import edu.ksu.bless.subbless.subBLESS.AssertedAction;
import edu.ksu.bless.subbless.subBLESS.Assignment;
import edu.ksu.bless.subbless.subBLESS.BasicAction;
import edu.ksu.bless.subbless.subBLESS.BehaviorActions;
import edu.ksu.bless.subbless.subBLESS.BehaviorTime;
import edu.ksu.bless.subbless.subBLESS.CommunicationAction;
import edu.ksu.bless.subbless.subBLESS.ExistentialLatticeQuantification;
import edu.ksu.bless.subbless.subBLESS.Expression;
import edu.ksu.bless.subbless.subBLESS.ExpressionOrRelation;
import edu.ksu.bless.subbless.subBLESS.FormalActual;
import edu.ksu.bless.subbless.subBLESS.FormalExpressionPair;
import edu.ksu.bless.subbless.subBLESS.FunctionCall;
import edu.ksu.bless.subbless.subBLESS.GuardedAction;
import edu.ksu.bless.subbless.subBLESS.PortInput;
import edu.ksu.bless.subbless.subBLESS.PortOutput;
import edu.ksu.bless.subbless.subBLESS.Subexpression;
import edu.ksu.bless.subbless.subBLESS.SubprogramCall;
import edu.ksu.bless.subbless.subBLESS.UniversalLatticeQuantification;
import edu.ksu.bless.subbless.subBLESS.Value;
import edu.ksu.bless.subbless.subBLESS.VariableDeclaration;
import edu.ksu.bless.subbless.subBLESS.util.SubBLESSSwitch;

public class BlessVisitor {

	protected final static org.sireum.aadl.ir.AadlASTFactory factory = new org.sireum.aadl.ir.AadlASTFactory();

	final BTSVisitor bv = new BTSVisitor();
	final AssertionVisitor av = new AssertionVisitor();
	final SubBlessVisitor sv = new SubBlessVisitor();

	public final static String BLESS = "BLESS";

	private List<String> path = null;

	private List<String> featureNames = null;
	private List<String> subcomponentNames = null;

	public Annex visit(ComponentInstance ci, List<String> path) {
		if (ci.getClassifier() != null) {
			List<BLESSAnnexSubclauseImpl> bas = EcoreUtil2.eAllOfType(ci.getClassifier(),
					BLESSAnnexSubclauseImpl.class);
			assert (bas.size() <= 1);

			if (bas.size() == 1) {
				this.path = path;

				featureNames = ci.getFeatureInstances().stream().map(f -> f.getName()).collect(Collectors.toList());

				subcomponentNames = ci.getComponentInstances().stream().map(c -> c.getName())
						.collect(Collectors.toList());

				bv.visit(bas.get(0));
				return Annex$.MODULE$.apply(BLESS, bv.pop());
			}
		}
		return null;
	}

	void handle(Class<?> c) {
		System.err.println("Need to handle " + c.getCanonicalName());

		int i = 0;
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			System.err.println("  " + ste);
			if (i++ > 9) {
				break;
			}
		}
	}

	<T> IS<Z, T> l2is(List<T> l) {
		return VisitorUtil.list2ISZ(l);
	}

	Name toName(String n, List<String> _path) {
		return factory.name(VisitorUtil.add(_path, n), null);
	}

	Name toName(String n) {
		return toName(n, path);
	}

	Name toName(Port p) {
		return toName(p.getName());
	}

	Name toSimpleName(String s) {
		if (featureNames.contains(s) || subcomponentNames.contains(s)) {
			return toName(s, path);
		} else {
			return toName(s, VisitorUtil.iList());
		}
	}

	<T> org.sireum.None<T> toNone() {
		return org.sireum.None$.MODULE$.apply();

	}

	<T> org.sireum.Some<T> toSome(T t) {
		return org.sireum.Some$.MODULE$.apply(t);
	}

	static BTSBinaryOp.Type toBinaryOp(String r) {
		if (r.equals("+")) {
			return BTSBinaryOp.byName("PLUS").get();
		} else if (r.equals("-")) {
			return BTSBinaryOp.byName("MINUS").get();
		} else if (r.equals("*")) {
			return BTSBinaryOp.byName("MULT").get();
		} else if (r.equals("/")) {
			return BTSBinaryOp.byName("DIV").get();
		} else if (r.equals("mod")) {
			return BTSBinaryOp.byName("MOD").get();
		} else if (r.equals("rem")) {
			return BTSBinaryOp.byName("REM").get();
		} else if (r.equals("**")) {
			return BTSBinaryOp.byName("EXP").get();
		} else if (r.equals("and")) {
			return BTSBinaryOp.byName("AND").get();
		} else if (r.equals("or")) {
			return BTSBinaryOp.byName("OR").get();
		} else if (r.equals("xor")) {
			return BTSBinaryOp.byName("XOR").get();
		} else if (r.equals("then")) {
			return BTSBinaryOp.byName("ANDTHEN").get();
		} else if (r.equals("else")) {
			return BTSBinaryOp.byName("ORELSE").get();
		} else if (r.equals("=")) {
			return BTSBinaryOp.byName("EQ").get();
		} else if (r.equals("<>")) {
			return BTSBinaryOp.byName("NEQ").get();
		} else if (r.equals("<")) {
			return BTSBinaryOp.byName("LT").get();
		} else if (r.equals("<=")) {
			return BTSBinaryOp.byName("LTE").get();
		} else if (r.equals(">=")) {
			return BTSBinaryOp.byName("GTE").get();
		} else if (r.equals(">")) {
			return BTSBinaryOp.byName("GT").get();
		}
		throw new RuntimeException();
	}

	static BTSUnaryOp.Type toUnaryOp(String r) {
		if (r.equals("-")) {
			return BTSUnaryOp.byName("NEG").get();
		} else if (r.equals("not")) {
			return BTSUnaryOp.byName("NOT").get();
		} else if (r.equals("abs")) {
			return BTSUnaryOp.byName("ABS").get();
		}

		throw new RuntimeException();
	}

	class BTSVisitor extends BLESSSwitch<Boolean> {

		@Override
		public Boolean caseBLESSAnnexSubclause(BLESSAnnexSubclause object) {

			boolean doNotProve = object.isNo_proof();

			List<BTSAssertion> _assertions = new ArrayList<>();
			if (object.getAssert_clause() != null) {
				for (Assertion ass : object.getAssert_clause().getAssertions()) {
					av.visit(ass);
					_assertions.add(av.pop());
				}
			}

			Option<BTSAssertion> _invariant = toNone();
			if (object.getInvariant() != null) {
				av.visit(object.getInvariant().getInv());
				_invariant = toSome(av.pop());
			}

			List<BTSVariableDeclaration> _variables = new ArrayList<>();
			if (object.getVariables() != null) {
				for (BehaviorVariable bv : object.getVariables().getBv()) {
					visit(bv);
					List<BTSVariableDeclaration> __variables = pop();
					_variables.addAll(__variables);
				}
			}

			List<BTSStateDeclaration> _states = new ArrayList<>();
			for (BehaviorState bs : object.getStates()) {
				visit(bs);
				_states.add(pop());
			}

			List<BTSTransition> _transitions = new ArrayList<>();
			for (BehaviorTransition bt : object.getTransitions().getBt()) {
				visit(bt);
				_transitions.add(pop());
			}

			BTSBLESSAnnexClause b = BTSBLESSAnnexClause$.MODULE$.apply(doNotProve, l2is(_assertions), _invariant,
					l2is(_variables), l2is(_states), l2is(_transitions));
			push(b);

			return false;
		}

		@Override
		public Boolean caseBehaviorState(BehaviorState object) {
			Name id = toSimpleName(object.getName());

			List<BTSStateCategory.Type> categories = new ArrayList<>();
			if (object.isInitial()) {
				categories.add(BTSStateCategory.byName("Initial").get());
			}
			if (object.isComplete()) {
				categories.add(BTSStateCategory.byName("Complete").get());
			}
			if (object.isFinal()) {
				categories.add(BTSStateCategory.byName("Final").get());
			}

			if (categories.isEmpty()) {
				categories.add(BTSStateCategory.byName("Execute").get());
			}

			Option<BTSAssertion> assertion = toNone();
			if (object.getState_assertion() != null) {
				av.visit(object.getState_assertion());
				assertion = toSome(av.pop());
			}

			BTSStateDeclaration bsd = BTSStateDeclaration$.MODULE$.apply(id, l2is(categories), assertion);
			push(bsd);

			return false;
		}

		@Override
		public Boolean caseBehaviorVariable(BehaviorVariable object) {

			Option<BTSVariableCategory.Type> category = toNone();
			if (object.isNonvolitile()) {
				category = toSome(BTSVariableCategory.byName("Nonvolatile").get());
			} else if (object.isShared()) {
				category = toSome(BTSVariableCategory.byName("Shared").get());
			} else if (object.isConstant()) {
				category = toSome(BTSVariableCategory.byName("Constant").get());
			} else if (object.isSpread()) {
				category = toSome(BTSVariableCategory.byName("Spread").get());
			} else if (object.getFinal() != null) {
				category = toSome(BTSVariableCategory.byName("Final").get());
			}

			av.visit(object.getType());
			BTSType varType = av.pop();

			Option<BTSExp> assignExpression = toNone();
			if (object.getExpression() != null) {
				sv.visit(object.getExpression());
				assignExpression = toSome(sv.pop());
			}

			Option<BTSAssertion> variableAssertion = toNone();
			if (object.getAssertion() != null) {
				av.visit(object.getAssertion());
				variableAssertion = toSome(av.pop());
			}

			List<BTSVariableDeclaration> names = new ArrayList<>();
			for (Declarator d : object.getVariable_names()) {
				Name name = toSimpleName(d.getVariable());

				Option<BLESSIntConst> arraySize = toNone();

				for (ValueConstant vc : d.getArray_size()) {
					av.visit(vc);
					BTSExp exp = av.pop();

					// TODO ???????
					throw new RuntimeException("TODO");
				}

				BTSVariableDeclaration vd = BTSVariableDeclaration$.MODULE$.apply(name, category, varType,
						assignExpression, arraySize, variableAssertion);
				names.add(vd);
			}

			push(names);

			return false;
		}

		@Override
		public Boolean caseBehaviorTransition(BehaviorTransition object) {

			visit(object.getTransition_label());
			BTSTransitionLabel label = pop();

			List<Name> _sourceStates = new ArrayList<>();
			for (BehaviorState bs : object.getSources()) {
				String srcName = bs.getName(); // just need name
				_sourceStates.add(toSimpleName(srcName));
			}

			BehaviorState dest = object.getDestination();
			String destName = dest.getName(); // just need name
			Name destState = toSimpleName(destName);

			Option<BTSTransitionCondition> _transitionCondition = null;
			if (object.getDispatch() != null) {
				assert (_transitionCondition == null);
				visit(object.getDispatch());
				_transitionCondition = toSome(pop());
			}
			if (object.getExecute() != null) {
				assert (_transitionCondition == null);
				visit(object.getExecute());
				_transitionCondition = toSome(pop());
			}

			if (object.getMode() != null) {
				assert (_transitionCondition == null);
				visit(object.getMode());
				_transitionCondition = toSome(pop());
			}

			if (object.getInternal() != null) {
				assert (_transitionCondition == null);
				visit(object.getInternal());
				_transitionCondition = toSome(pop());
			}

			if (_transitionCondition == null) {
				_transitionCondition = toNone();
			}

			Option<BTSBehaviorActions> actions = toNone();
			if (object.getActions() != null) {
				sv.visit(object.getActions());
				actions = toSome(sv.pop());
			}

			Option<BTSAssertion> assertion = toNone();
			if (object.getAss() != null) {
				av.visit(object.getAss());
				assertion = toSome(av.pop());
			}

			BTSTransition bt = BTSTransition$.MODULE$.apply(label, l2is(_sourceStates), destState, _transitionCondition,
					actions, assertion);
			push(bt);

			return false;
		}

		@Override
		public Boolean caseTransitionLabel(TransitionLabel object) {
			Name id = toSimpleName(object.getId());

			Option<Z> priority = null;
			if (object.getPriority() != null) {
				Z value = Z$.MODULE$.apply(Integer.parseInt(object.getPriority()));
				priority = toSome(value);
			} else {
				priority = toNone();
			}

			BTSTransitionLabel label = BTSTransitionLabel$.MODULE$.apply(id, priority);
			push(label);

			return false;
		}

		@Override
		public Boolean caseDispatchCondition(DispatchCondition object) {
			List<BTSDispatchConjunction> dispatchTriggers = new ArrayList<>();
			if (object.getDe() != null) {
				for (DispatchConjunction dc : object.getDe().getDc()) {
					visit(dc);
					dispatchTriggers.add(pop());
				}
			}

			List<Name> frozenPorts = new ArrayList<>();
			for (Port p : object.getFrozen()) {
				frozenPorts.add(toName(p));
			}

			BTSDispatchCondition bdc = BTSDispatchCondition$.MODULE$.apply(l2is(dispatchTriggers), l2is(frozenPorts));
			push(bdc);

			return false;
		}

		@Override
		public Boolean caseDispatchConjunction(DispatchConjunction object) {

			List<BTSDispatchTrigger> conjunction = new ArrayList<>();
			for (DispatchTrigger t : object.getTrigger()) {
				visit(t);
				conjunction.add(pop());
			}

			BTSDispatchConjunction dc = BTSDispatchConjunction$.MODULE$.apply(l2is(conjunction));
			push(dc);

			return false;
		}

		@Override
		public Boolean caseDispatchTrigger(DispatchTrigger object) {

			BTSDispatchTrigger ret = null;
			if (object.isStop()) {
				assert ret == null;

				ret = BTSDispatchTriggerStop.apply();
			}

			if (object.isTimeout()) {
				assert ret == null;
				List<Name> ports = new ArrayList<>();
				if (object.isLp()) {
					for (Port p : object.getPorts()) {
						ports.add(toName(p));
					}
				}

				Option<BTSBehaviorTime> time = toNone();
				if (object.getTime() != null) {
					sv.visit(object.getTime());
					time = toSome(pop());
				}

				ret = BTSDispatchTriggerTimeout$.MODULE$.apply(l2is(ports), time);
			}

			if (object.getPort() != null) {
				assert ret == null;

				ret = BTSDispatchTriggerPort$.MODULE$.apply(toName(object.getPort()));
			}

			assert ret != null;
			push(ret);

			return false;
		}

		@Override
		public Boolean caseExecuteCondition(ExecuteCondition object) {

			if (object.getOtherwise() != null) {
				push(BTSExecuteConditionOtherwise$.MODULE$.apply());
			} else if (object.getTimeout() != null) {
				push(BTSExecuteConditionTimeout$.MODULE$.apply());
			} else if (object.getEor() != null) {

				sv.visit(object.getEor());
				BTSExp e = sv.pop();

				BTSExecuteCondition c = BTSExecuteConditionExp$.MODULE$.apply(e);
				push(c);

			} else {
				throw new RuntimeException("Unexpected execute condition " + object);
			}

			return false;
		}

		@Override
		public Boolean caseModeCondition(ModeCondition object) {
			handle(object.getClass());

			BTSModeCondition c = BTSModeCondition.apply();
			push(c);

			return false;
		}

		@Override
		public Boolean caseInternalCondition(InternalCondition object) {
			handle(object.getClass());

			BTSInternalCondition c = BTSInternalCondition.apply();
			push(c);

			return false;
		}

		@Override
		public Boolean defaultCase(EObject o) {
			for (EObject child : o.eContents()) {
				visit(child);
			}
			return null;
		}

		public Boolean visit(EObject o) {
			assert (isSwitchFor(o.eClass().getEPackage()));
			return doSwitch(o);
		}

		Object result = null;

		void push(Object o) {
			assert (result == null);
			result = o;
		}

		@SuppressWarnings("unchecked")
		<T> T pop() {
			assert (result != null);
			T ret = (T) result;
			result = null;
			return ret;
		}
	}

	class AssertionVisitor extends AssertionSwitch<Boolean> {

		@Override
		public Boolean caseAssertion(Assertion object) {
			handle(object.getClass());

			BTSAssertion a = BTSAssertion$.MODULE$.apply();
			push(a);

			return false;
		}

		@Override
		public Boolean caseType(Type object) {

			if (object.getData_component_reference() != null) {
				DataClassifier dc = object.getData_component_reference();
				Classifier c = Classifier$.MODULE$.apply(dc.getQualifiedName());
				push(BTSClassifier$.MODULE$.apply(c));
			} else {
				defaultCase(object); // visit children via default case
				throw new RuntimeException("TODO");
			}

			return false;
		}

		@Override
		public Boolean caseValueConstant(ValueConstant object) {
			BTSLiteralType.Type typ = null;
			String exp = null;

			if (object.getNumber() != null) {
				NumericConstant nc = object.getNumber();
				if (nc.getInteger() != null) {
					typ = BTSLiteralType.byName("INTEGER").get();
					exp = nc.getInteger();
				} else if (nc.getReal() != null) {
					typ = BTSLiteralType.byName("FLOAT").get();
					exp = nc.getReal();
				} else {
					throw new RuntimeException("TODO");
				}
			} else if (object.getString_literal() != null) {
				typ = BTSLiteralType.byName("STRING").get();
				exp = object.getString_literal();
			} else if (object.isT()) {
				typ = BTSLiteralType.byName("BOOLEAN").get();
				exp = "true";
			} else if (object.isF()) {
				typ = BTSLiteralType.byName("BOOLEAN").get();
				exp = "false";
			} else {
				throw new RuntimeException("Unexpected");
			}

			push(BTSLiteralExp$.MODULE$.apply(typ, exp));

			return false;
		}

		@Override
		public Boolean casePartialName(PartialName object) {
			assert (object.getArray_index().isEmpty()); // TODO

			push(BTSNameExp$.MODULE$.apply(toName(object.getRecord_id()))); // TODO

			return false;
		}

		@Override
		public Boolean defaultCase(EObject o) {
			for (EObject child : o.eContents()) {
				visit(child);
			}
			return null;
		}

		public Boolean visit(EObject o) {
			assert (isSwitchFor(o.eClass().getEPackage()));
			return doSwitch(o);
		}

		Object result = null;

		void push(Object o) {
			assert (result == null);
			result = o;
		}

		@SuppressWarnings("unchecked")
		<T> T pop() {
			assert (result != null);
			T ret = (T) result;
			result = null;
			return ret;
		}
	}

	class SubBlessVisitor extends SubBLESSSwitch<Boolean> {

		@Override
		public Boolean caseBehaviorActions(BehaviorActions object) {
			BTSExecutionOrder.Type executionOrder = BTSExecutionOrder.byName("Sequential").get();
			if (object.isAmp()) {
				executionOrder = BTSExecutionOrder.byName("Concurrent").get();
			}

			List<BTSAssertedAction> actions = new ArrayList<>();
			for (AssertedAction a : object.getAction()) {
				visit(a);
				actions.add(pop());
			}

			BTSBehaviorActions a = BTSBehaviorActions$.MODULE$.apply(executionOrder, l2is(actions));
			push(a);

			return false;
		}

		@Override
		public Boolean caseAlternative(Alternative object) {

			if (object.getAlternative() != null && !object.getAlternative().isEmpty()) {
				Option<TODO> availability = toNone();
				if (object.getAvailability() != null) {
					throw new RuntimeException("TODO");
				}

				List<BTSGuardedAction> alternatives = new ArrayList<>();
				for (GuardedAction ga : object.getAlternative()) {
					visit(ga.getGuard());
					BTSExp guard = pop();

					visit(ga.getAction());
					BTSAssertedAction action = pop();

					BTSGuardedAction a = BTSGuardedAction$.MODULE$.apply(guard, action);
					alternatives.add(a);
				}

				BTSIfBLESSAction a = BTSIfBLESSAction$.MODULE$.apply(availability, l2is(alternatives));
				push(a);

			} else if (object.getTest() != null) {

				assert (object.getTest().size() == object.getActions().size());

				int i = 0;

				visit(object.getTest().get(i));
				BTSExp cond = pop();

				visit(object.getActions().get(i));
				BTSBehaviorActions actions = pop();

				BTSConditionalActions ifBranch = BTSConditionalActions$.MODULE$.apply(cond, actions);

				List<BTSConditionalActions> elseIfBranches = new ArrayList<>();
				for (; i < object.getTest().size(); i++) {
					visit(object.getTest().get(i));
					BTSExp econd = pop();

					visit(object.getActions().get(i));
					BTSBehaviorActions eactions = pop();

					BTSConditionalActions eb = BTSConditionalActions$.MODULE$.apply(econd, eactions);
					elseIfBranches.add(eb);
				}

				Option<BTSBehaviorActions> elseBranch = toNone();
				if (i < object.getActions().size()) {
					assert (i + 1 == object.getActions().size());

					visit(object.getActions().get(i));

					throw new RuntimeException("TODO");
				}

				BTSIfBAAction a = BTSIfBAAction$.MODULE$.apply(ifBranch, l2is(elseIfBranches), elseBranch);
				push(a);
			}

			return false;
		}

		@Override
		public Boolean caseAssertedAction(AssertedAction object) {
			Option<BTSAssertion> precondition = toNone();
			if (object.getPrecondition() != null) {
				visit(object.getPrecondition());
				precondition = toSome(pop());
			}

			Option<BTSAssertion> postcondition = toNone();
			if (object.getPostcondition() != null) {
				visit(object.getPostcondition());
				postcondition = toSome(pop());
			}

			visit(object.getAction());
			BTSAction action = pop();

			BTSAssertedAction a = BTSAssertedAction$.MODULE$.apply(precondition, action, postcondition);
			push(a);

			return false;
		}

		@Override
		public Boolean caseBasicAction(BasicAction object) {
			if (object.isSkip()) {
				push(BTSSkipAction$.MODULE$.apply());
				return false;
			} else {
				// visit children via default case
				return null;
			}
		}

		@Override
		public Boolean caseExistentialLatticeQuantification(ExistentialLatticeQuantification object) {

			List<BTSVariableDeclaration> quantifiedVariables = new ArrayList<>();
			if (object.getQuantified_variables() != null) {
				for (VariableDeclaration vd : object.getQuantified_variables().getVariable()) {
					visit(vd);
					List<BTSVariableDeclaration> vars = pop();
					quantifiedVariables.addAll(vars);
				}
			}

			visit(object.getActions());
			BTSBehaviorActions actions = pop();

			Option<BTSBehaviorTime> timeout = toNone();
			if (object.getTimeout() != null) {
				throw new RuntimeException(); // TODO
			}

			Option<TODO> catchClause = toNone();
			if (object.getCatch_clause() != null) {
				throw new RuntimeException(); // TODO
			}

			BTSExistentialLatticeQuantification q = BTSExistentialLatticeQuantification$.MODULE$
					.apply(l2is(quantifiedVariables), actions, timeout, catchClause);
			push(q);

			return false;
		}

		@Override
		public Boolean caseVariableDeclaration(VariableDeclaration object) {

			Option<BTSVariableCategory.Type> category = toNone();
			if (object.isConstant()) {
				category = BTSVariableCategory.byName("Constant");
			}

			av.visit(object.getType());
			BTSType varType = av.pop();

			Option<BTSExp> assignExpression = toNone();
			if (object.getExpression() != null) {
				sv.visit(object.getExpression());
				assignExpression = toSome(sv.pop());
			}

			Option<BTSAssertion> variableAssertion = toNone();
			if (object.getAssertion() != null) {
				av.visit(object.getAssertion());
				variableAssertion = toSome(av.pop());
			}

			List<BTSVariableDeclaration> names = new ArrayList<>();
			for (String d : object.getVariable_names()) {
				Name name = toSimpleName(d);

				Option<BLESSIntConst> arraySize = toNone();

				BTSVariableDeclaration vd = BTSVariableDeclaration$.MODULE$.apply(name, category, varType,
						assignExpression, arraySize, variableAssertion);
				names.add(vd);
			}

			push(names);

			return false;
		}

		@Override
		public Boolean caseUniversalLatticeQuantification(UniversalLatticeQuantification object) {

			List<Name> latticeVariables = new ArrayList<>();
			if (object.getLv() != null) {
				throw new RuntimeException(); // TODO
			}

			Option<TODO> range = toNone();
			if (object.getR() != null) {
				throw new RuntimeException(); // TODO
			}

			visit(object.getElq());
			BTSExistentialLatticeQuantification elq = pop();

			BTSUniversalLatticeQuantification q = BTSUniversalLatticeQuantification$.MODULE$
					.apply(l2is(latticeVariables), range, elq);
			push(q);

			return false;
		}

		@Override
		public Boolean caseAssignment(Assignment object) {
			visit(object.getLhs());
			BTSExp lhs = pop();

			visit(object.getRhs());
			BTSExp rhs = pop();

			BTSAssignmentAction a = BTSAssignmentAction$.MODULE$.apply(lhs, rhs);
			push(a);

			return false;
		}

		@Override
		public Boolean caseExpression(Expression object) {

			assert object.getSe().size() > 0;

			if (object.getSe().size() == 1) {
				assert object.getSym() == null;

				visit(object.getSe().get(0));

			} else {
				String rs = object.getSym();
				BTSBinaryOp.Type op = toBinaryOp(rs);

				List<Subexpression> elems = object.getSe();
				int i = elems.size() - 1;

				visit(elems.get(i--));
				BTSExp rhs = pop();

				visit(elems.get(i--));
				BTSExp lhs = pop();

				BTSBinaryExp temp = BTSBinaryExp$.MODULE$.apply(op, lhs, rhs);

				for (; i >= 0; i--) {
					visit(elems.get(i));
					lhs = pop();

					temp = BTSBinaryExp$.MODULE$.apply(op, lhs, temp);
				}

				push(temp);
			}

			return false;
		}

		@Override
		public Boolean caseExpressionOrRelation(ExpressionOrRelation object) {
			if (object.getExp() != null) {
				visit(object.getExp());
			} else if (object.getSub() != null) {
				// FIXME: grammar assumes all ops are the same (e.g. x < y < z)
				String rs = object.getR();
				BTSBinaryOp.Type op = toBinaryOp(rs);

				assert object.getSub().size() >= 2;

				List<Subexpression> elems = object.getSub();
				int i = elems.size() - 1;

				visit(elems.get(i--));
				BTSExp rhs = pop();

				visit(elems.get(i--));
				BTSExp lhs = pop();

				BTSBinaryExp temp = BTSBinaryExp$.MODULE$.apply(op, lhs, rhs);

				for (; i >= 0; i--) {
					visit(elems.get(i));
					lhs = pop();

					temp = BTSBinaryExp$.MODULE$.apply(op, lhs, temp);
				}

				push(temp);

			} else {
				throw new RuntimeException("Unexpected");
			}

			return false;
		}

		@Override
		public Boolean caseSubexpression(Subexpression object) {

			defaultCase(object); // visit children via default case

			// handle unary exp
			if (object.isNot()) {
				push(BTSUnaryExp$.MODULE$.apply(toUnaryOp("not"), pop()));
			} else if (object.isAbs()) {
				push(BTSUnaryExp$.MODULE$.apply(toUnaryOp("abs"), pop()));
			} else if (object.isMinus()) {
				push(BTSUnaryExp$.MODULE$.apply(toUnaryOp("-"), pop()));
			}

			return false;
		}

		@Override
		public Boolean caseFunctionCall(FunctionCall object) {
			String pack = object.getPack().stream().collect(Collectors.joining("::"));
			String func = object.getFunc();

			Name name = toName(func);

			List<BTSFormalExpPair> args = new ArrayList<>();
			if (object.getParameters() != null) {
				for (FormalExpressionPair p : object.getParameters().getParameters()) {
					Option<Name> paramName = toSome(toName(p.getFormal()));

					visit(p.getActual());
					Option<BTSExp> exp = toSome(pop());

					args.add(BTSFormalExpPair$.MODULE$.apply(paramName, exp));
				}
			}

			BTSFunctionCall c = BTSFunctionCall$.MODULE$.apply(name, l2is(args));
			push(c);

			return false;
		}

		@Override
		public Boolean caseCommunicationAction(CommunicationAction object) {

			if (object.getFrozen_port() != null) {
				push(BTSFrozenPortAction$.MODULE$.apply(toName(object.getFrozen_port())));
			} else {
				defaultCase(object); // visit children via default case
			}

			return false;
		}

		@Override
		public Boolean caseSubprogramCall(SubprogramCall object) {
			assert (subcomponentNames.contains(object.getProcedure()));

			Name name = toSimpleName(object.getProcedure());

			List<BTSFormalExpPair> params = new ArrayList<>();
			if (object.getParameters() != null) {
				for (FormalActual fa : object.getParameters().getVariables()) {
					Option<Name> paramName = toNone();
					if (fa.getFormal() != null) {
						paramName = toSome(toName(fa.getFormal()));
					}

					Option<BTSExp> exp = toNone();
					if (fa.getActual() != null) {
						if (fa.getActual().getVariable() != null) {
							exp = toSome(BTSNameExp$.MODULE$.apply(toSimpleName(fa.getActual().getVariable())));
						} else if (fa.getActual().getConstant() != null) {
							av.visit(fa.getActual().getConstant());
							exp = toSome(av.pop());
						} else {
							throw new RuntimeException("Unexpected");
						}
					}

					params.add(BTSFormalExpPair$.MODULE$.apply(paramName, exp));
				}
			}
			push(BTSSubprogramCallAction$.MODULE$.apply(name, l2is(params)));

			return false;
		}

		@Override
		public Boolean casePortInput(PortInput object) {
			Name name = toName(object.getPort());

			visit(object.getVariable());
			BTSExp variable = pop();

			BTSPortInAction a = BTSPortInAction$.MODULE$.apply(name, variable);
			push(a);

			return false;
		}

		@Override
		public Boolean casePortOutput(PortOutput object) {
			Name name = toName(object.getPort());

			Option<BTSExp> exp = toNone();
			if (object.getEor() != null) {
				visit(object.getEor());
				exp = toSome(pop());
			}

			BTSPortOutAction a = BTSPortOutAction$.MODULE$.apply(name, exp);
			push(a);

			return false;
		}

		@Override
		public Boolean caseName(edu.ksu.bless.subbless.subBLESS.Name object) {

			Name name = toSimpleName(object.getId());
			BTSNameExp e = BTSNameExp$.MODULE$.apply(name);

			assert (object.getArray_index().isEmpty()); // TODO

			if (!object.getPn().isEmpty()) { // weird grammar
				// access exp
				assert (object.getPn().get(0).getArray_index().isEmpty()); // TODO
				String attribute = object.getPn().get(0).getRecord_id();

				BTSAccessExp t = BTSAccessExp$.MODULE$.apply(e, attribute);

				for (int i = 1; i < object.getPn().size(); i++) {
					PartialName pn = object.getPn().get(i);
					assert (pn.getArray_index().isEmpty()); // TODO

					attribute = pn.getRecord_id();

					t = BTSAccessExp$.MODULE$.apply(t, attribute);
				}

				push(t);

			} else if (!object.getArray_index().isEmpty()) {
				// indexing exp
			} else {
				push(e);
			}

			return false;
		}

		@Override
		public Boolean caseValue(Value object) {
			if (object.getPort() != null) {
				Port p = object.getPort();

				if (object.isQ()) {
					throw new RuntimeException("TODO");
				} else if (object.isFresh()) {
					throw new RuntimeException("TODO");
				} else if (object.isCount()) {
					throw new RuntimeException("TODO");
				} else if (object.isUpdated()) {
					throw new RuntimeException("TODO");
				}

				throw new RuntimeException("TODO");

			} else if (object.getVariable() != null) {
				visit(object.getVariable());
			} else if (object.isInmode()) {
				for (org.osate.aadl2.Mode m : object.getModes()) {

				}
				throw new RuntimeException("TODO");
			} else if (object.isTimeout()) {
				throw new RuntimeException("TODO");
			} else if (object.isNul()) {
				throw new RuntimeException("TODO");
			} else if (object.isNow()) {
				throw new RuntimeException("TODO");
			} else if (object.isTops()) {
				throw new RuntimeException("TODO");
			} else if (object.getConst() != null) {
				av.visit(object.getConst());
				BTSExp e = av.pop();
				push(e);
			} else {
				throw new RuntimeException("Unexpected");
			}

			return false;
		}

		@Override
		public Boolean caseBehaviorTime(BehaviorTime object) {
			handle(object.getClass());

			BTSBehaviorTime t = BTSBehaviorTime$.MODULE$.apply();
			push(t);

			return false;
		}

		@Override
		public Boolean defaultCase(EObject o) {
			for (EObject child : o.eContents()) {
				visit(child);
			}
			return null;
		}

		public Boolean visit(EObject o) {
			assert (isSwitchFor(o.eClass().getEPackage()));
			return doSwitch(o);
		}

		Object result = null;

		void push(Object o) {
			assert (result == null);
			result = o;
		}

		@SuppressWarnings("unchecked")
		<T> T pop() {
			assert (result != null);
			T ret = (T) result;
			result = null;
			return ret;
		}
	}
}
