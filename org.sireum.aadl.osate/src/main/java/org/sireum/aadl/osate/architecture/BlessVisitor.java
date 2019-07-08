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
import org.sireum.aadl.ir.*;
import org.sireum.aadl.ir.Name;

import edu.ksu.bless.assertion.assertion.*;
import edu.ksu.bless.assertion.assertion.util.AssertionSwitch;
import edu.ksu.bless.bless.bLESS.*;
import edu.ksu.bless.bless.bLESS.impl.BLESSAnnexSubclauseImpl;
import edu.ksu.bless.bless.bLESS.util.BLESSSwitch;
import edu.ksu.bless.subbless.subBLESS.*;
import edu.ksu.bless.subbless.subBLESS.util.SubBLESSSwitch;

public class BlessVisitor {

	protected final static org.sireum.aadl.ir.AadlASTFactory factory = new org.sireum.aadl.ir.AadlASTFactory();

	final BTSVisitor bv = new BTSVisitor();
	final AssertionVisitor av = new AssertionVisitor();
	final SubBlessVisitor sv = new SubBlessVisitor();

	public final static String BLESS = "BLESS";

	public Annex visit(ComponentInstance ci) {
		if (ci.getClassifier() != null) {
			List<BLESSAnnexSubclauseImpl> bas = EcoreUtil2.eAllOfType(ci.getClassifier(),
					BLESSAnnexSubclauseImpl.class);
			assert (bas.size() <= 1);

			if (bas.size() == 1) {
				bv.visit(bas.get(0));
				return Annex$.MODULE$.apply(BLESS, bv.pop());
			}
		}
		return null;
	}

	static void handle(Class<?> c) {
		System.err.println("Need to handle " + c.getCanonicalName());

		int i = 0;
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			System.err.println("  " + ste);
			if (i++ > 9) {
				break;
			}
		}
	}

	private <T> IS<Z, T> l2is(List<T> l) {
		return VisitorUtil.list2ISZ(l);
	}

	static Name toName(String n) {
		return factory.name(VisitorUtil.toIList(n), null);
	}

	static <T> org.sireum.None<T> toNone() {
		return org.sireum.None$.MODULE$.apply();

	}

	static <T> org.sireum.Some<T> toSome(T t) {
		return org.sireum.Some$.MODULE$.apply(t);
	}

	// FIXME is bless grammar case sensitive?
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
		} else if(r.equals("not")) {
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
			Name id = toName(object.getQualifiedName());

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
			BTSType varType = av.pop(); // TODO update air


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
				Name name = toName(d.getVariable());

				Option<BLESSIntConst> arraySize = toNone();

				for (ValueConstant vc : d.getArray_size()) {
					// TODO ???????
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
				String srcName = bs.getQualifiedName(); // just need name
				_sourceStates.add(toName(srcName));
			}

			BehaviorState dest = object.getDestination();
			String destName = dest.getQualifiedName(); // just need name
			Name destState = toName(destName);


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
			Name id = toName(object.getId());

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
			for (Port n : object.getFrozen()) {
				frozenPorts.add(toName(n.getQualifiedName()));
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
			if(object.isStop()) {
				assert ret == null;

				ret = BTSDispatchTriggerStop.apply();
			}

			if(object.isTimeout()) {
				assert ret == null;
				List<Name> ports = new ArrayList<>();
				if (object.isLp()) {
					for (Port p : object.getPorts()) {
						ports.add(toName(p.getQualifiedName()));
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

				Name port = toName(object.getPort().getQualifiedName());

				ret = BTSDispatchTriggerPort$.MODULE$.apply(port);
			}

			assert ret != null;
			push(ret);

			return false;
		}

		@Override
		public Boolean caseExecuteCondition(ExecuteCondition object) {
			handle(object.getClass());

			BTSExecuteCondition c = BTSExecuteCondition.apply();
			push(c);

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

			object.getNamedassertion();
			object.getNamedenumeration();
			object.getNamedfunction();

			object.getNamelessassertion();
			object.getNamelessenumeration();
			object.getNamelessfunction();

			BTSAssertion a = BTSAssertion$.MODULE$.apply();
			push(a);

			return false;
		}

		@Override
		public Boolean caseType(Type object) {

			if (object.getData_component_reference() != null) {
				DataClassifier c = object.getData_component_reference();
				push(BTSClassifier$.MODULE$.apply(toName(c.getQualifiedName())));
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
				visit(object.getNumber());
				throw new RuntimeException("TODO");
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
		public Boolean defaultCase(EObject o) {
			for (EObject child : o.eContents()) {
				visit(child);
			}
			return null;
		}

		public Boolean visit(EObject o) {
			assert (this.isSwitchFor(o.eClass().getEPackage()));
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
				Option<String> availability = toNone();
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

				int i = 0;

				visit(object.getTest().get(i));
				BTSExp cond = pop();

				visit(object.getActions().get(i));
				BTSBehaviorActions actions = pop();

				BTSConditionalActions ifBranch = BTSConditionalActions$.MODULE$.apply(cond, actions);

				List<BTSConditionalActions> elseIfBranches = new ArrayList<>();
				for (; i < object.getTest().size(); i++) {
					object.getTest().get(i);
					object.getActions().get(i);

					throw new RuntimeException("TODO");
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

			Option<String> catchClause = toNone();
			if (object.getCatch_clause() != null) {
				throw new RuntimeException(); // TODO
			}

			BTSExistentialLatticeQuantification q = BTSExistentialLatticeQuantification$.MODULE$.apply(
					l2is(quantifiedVariables), actions, timeout, catchClause);
			push(q);

			return false;
		}

		@Override
		public Boolean caseVariableDeclaration(VariableDeclaration object) {

			// TODO slang enum types and category needs to be a seq
			Option<BTSVariableCategory.Type> category = toNone();
			if (object.isConstant()) {
				category = BTSVariableCategory.byName("Constant");
			}

			av.visit(object.getType());
			BTSType varType = av.pop(); // TODO update air

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
				Name name = toName(d);

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
			if(object.getLv() != null) {
				throw new RuntimeException(); // TODO
			}

			Option<String> range = toNone();
			if(object.getR() != null) {
				throw new RuntimeException(); // TODO
			}

			visit(object.getElq());
			BTSExistentialLatticeQuantification elq = pop();

			BTSUniversalLatticeQuantification q = BTSUniversalLatticeQuantification$.MODULE$.apply(
					l2is(latticeVariables), range, elq);
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
			if(object.getExp() != null) {
				visit(object.getExp());
			} else if(object.getSub() != null) {
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
			if(object.isNot()) {
				push(BTSUnaryExp$.MODULE$.apply(toUnaryOp("not"), pop()));
			} else if(object.isAbs()) {
				push(BTSUnaryExp$.MODULE$.apply(toUnaryOp("abs"), pop()));
			} else if(object.isMinus()) {
				push(BTSUnaryExp$.MODULE$.apply(toUnaryOp("-"), pop()));
			}

			return false;
		}

		@Override
		public Boolean caseFunctionCall(FunctionCall object) {
			String pack = object.getPack().stream().collect(Collectors.joining("::"));
			String func = object.getFunc();

			Name name = toName(pack + "::" + func);

			List<BTSFormalExpPair> args = new ArrayList<>();
			if (object.getParameters() != null) {
				throw new RuntimeException("process params");
			}

			BTSFunctionCall c = BTSFunctionCall$.MODULE$.apply(name, l2is(args));
			push(c);

			return false;
		}

		@Override
		public Boolean caseCommunicationAction(CommunicationAction object) {

			if(object.getFrozen_port() != null) {
				Port p = object.getFrozen_port();
				push(BTSFrozenPortAction$.MODULE$.apply(toName(p.getQualifiedName())));
			} else {
				defaultCase(object); // visit children via default case
			}

            return false;
		}

		@Override
		public Boolean caseSubprogramCall(SubprogramCall object) {
			Name name = toName(object.getProcedure());

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
							exp = toSome(BTSNameExp$.MODULE$.apply(toName(fa.getActual().getVariable())));
						} else if (fa.getActual().getConstant() != null) {
							throw new RuntimeException("TODO");
						} else {
							throw new RuntimeException("TODO");
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
			Port p = object.getPort();
			Name name = toName(p.getQualifiedName());

			visit(object.getVariable());
			BTSExp variable = pop();

			BTSPortInAction a = BTSPortInAction$.MODULE$.apply(name, variable);
			push(a);

			return false;
		}

		@Override
		public Boolean casePortOutput(PortOutput object) {

			Port p = object.getPort();
			Name name = toName(p.getQualifiedName());

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

			Name name = toName(object.getId());
			BTSNameExp e = BTSNameExp$.MODULE$.apply(name);
			push(e);

			assert (object.getArray_index().isEmpty()); // TODO

			assert (object.getPn().isEmpty()); // TODO

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
