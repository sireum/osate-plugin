package org.sireum.aadl.osate.architecture;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.Port;
import org.osate.aadl2.SubprogramClassifier;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.IS;
import org.sireum.Option;
import org.sireum.Z;
import org.sireum.Z$;
import org.sireum.hamr.ir.Annex;
import org.sireum.hamr.ir.Annex$;
import org.sireum.hamr.ir.BLESSIntConst;
import org.sireum.hamr.ir.BTSAction;
import org.sireum.hamr.ir.BTSAssertedAction;
import org.sireum.hamr.ir.BTSAssertedAction$;
import org.sireum.hamr.ir.BTSAssertion;
import org.sireum.hamr.ir.BTSAssertion$;
import org.sireum.hamr.ir.BTSAssignmentAction;
import org.sireum.hamr.ir.BTSAssignmentAction$;
import org.sireum.hamr.ir.BTSBLESSAnnexClause;
import org.sireum.hamr.ir.BTSBLESSAnnexClause$;
import org.sireum.hamr.ir.BTSBehaviorActions;
import org.sireum.hamr.ir.BTSBehaviorActions$;
import org.sireum.hamr.ir.BTSBehaviorTime;
import org.sireum.hamr.ir.BTSBinaryOp;
import org.sireum.hamr.ir.BTSClassifier$;
import org.sireum.hamr.ir.BTSDispatchCondition;
import org.sireum.hamr.ir.BTSDispatchCondition$;
import org.sireum.hamr.ir.BTSDispatchConjunction;
import org.sireum.hamr.ir.BTSDispatchConjunction$;
import org.sireum.hamr.ir.BTSDispatchTrigger;
import org.sireum.hamr.ir.BTSDispatchTriggerPort$;
import org.sireum.hamr.ir.BTSDispatchTriggerStop;
import org.sireum.hamr.ir.BTSDispatchTriggerTimeout$;
import org.sireum.hamr.ir.BTSExecutionOrder;
import org.sireum.hamr.ir.BTSExp;
import org.sireum.hamr.ir.BTSFormalExpPair;
import org.sireum.hamr.ir.BTSFormalExpPair$;
import org.sireum.hamr.ir.BTSInternalCondition;
import org.sireum.hamr.ir.BTSLiteralExp$;
import org.sireum.hamr.ir.BTSLiteralType;
import org.sireum.hamr.ir.BTSModeCondition;
import org.sireum.hamr.ir.BTSNameExp;
import org.sireum.hamr.ir.BTSNameExp$;
import org.sireum.hamr.ir.BTSPortInAction;
import org.sireum.hamr.ir.BTSPortInAction$;
import org.sireum.hamr.ir.BTSPortOutAction;
import org.sireum.hamr.ir.BTSPortOutAction$;
import org.sireum.hamr.ir.BTSSkipAction$;
import org.sireum.hamr.ir.BTSStateCategory;
import org.sireum.hamr.ir.BTSStateDeclaration;
import org.sireum.hamr.ir.BTSStateDeclaration$;
import org.sireum.hamr.ir.BTSSubprogramCallAction$;
import org.sireum.hamr.ir.BTSTransition;
import org.sireum.hamr.ir.BTSTransition$;
import org.sireum.hamr.ir.BTSTransitionCondition;
import org.sireum.hamr.ir.BTSTransitionLabel;
import org.sireum.hamr.ir.BTSTransitionLabel$;
import org.sireum.hamr.ir.BTSType;
import org.sireum.hamr.ir.BTSUnaryOp;
import org.sireum.hamr.ir.BTSVariableCategory;
import org.sireum.hamr.ir.BTSVariableDeclaration;
import org.sireum.hamr.ir.BTSVariableDeclaration$;
import org.sireum.hamr.ir.Classifier;
import org.sireum.hamr.ir.Classifier$;
import org.sireum.hamr.ir.Name;

import com.multitude.aadl.bless.bLESS.AssertedAction;
import com.multitude.aadl.bless.bLESS.Assertion;
import com.multitude.aadl.bless.bLESS.Assignment;
import com.multitude.aadl.bless.bLESS.BLESSSubclause;
import com.multitude.aadl.bless.bLESS.BasicAction;
import com.multitude.aadl.bless.bLESS.BehaviorActions;
import com.multitude.aadl.bless.bLESS.BehaviorState;
import com.multitude.aadl.bless.bLESS.BehaviorTransition;
import com.multitude.aadl.bless.bLESS.CommunicationAction;
import com.multitude.aadl.bless.bLESS.Constant;
import com.multitude.aadl.bless.bLESS.DispatchCondition;
import com.multitude.aadl.bless.bLESS.DispatchConjunction;
import com.multitude.aadl.bless.bLESS.DispatchTrigger;
import com.multitude.aadl.bless.bLESS.ExecuteCondition;
import com.multitude.aadl.bless.bLESS.FormalActual;
import com.multitude.aadl.bless.bLESS.InternalCondition;
import com.multitude.aadl.bless.bLESS.ModeCondition;
import com.multitude.aadl.bless.bLESS.NamedAssertion;
import com.multitude.aadl.bless.bLESS.NamelessAssertion;
import com.multitude.aadl.bless.bLESS.PortInput;
import com.multitude.aadl.bless.bLESS.PortOutput;
import com.multitude.aadl.bless.bLESS.SubprogramCall;
import com.multitude.aadl.bless.bLESS.TransitionLabel;
import com.multitude.aadl.bless.bLESS.Type;
import com.multitude.aadl.bless.bLESS.TypeDeclaration;
import com.multitude.aadl.bless.bLESS.Value;
import com.multitude.aadl.bless.bLESS.ValueName;
import com.multitude.aadl.bless.bLESS.VariableDeclaration;
import com.multitude.aadl.bless.bLESS.impl.BLESSSubclauseImpl;
import com.multitude.aadl.bless.bLESS.util.BLESSSwitch;

public class BlessVisitor extends BLESSSwitch<Boolean> {

	protected final static org.sireum.hamr.ir.AadlASTFactory factory = new org.sireum.hamr.ir.AadlASTFactory();

	Visitor v = null;

	public final static String BLESS = "BLESS";

	private List<String> path = null;

	private List<String> featureNames = null;
	private List<String> subcomponentNames = null;

	public BlessVisitor(Visitor v) {
		this.v = v;
	}

	public List<Annex> visit(ComponentInstance ci, List<String> path) {
		List<Annex> ret = new ArrayList<>();
		if (ci.getClassifier() != null) {
			List<BLESSSubclauseImpl> bas = EcoreUtil2.eAllOfType(ci.getClassifier(), BLESSSubclauseImpl.class);
			assert (bas.size() <= 1);

			if (bas.size() == 1) {
				this.path = path;

				featureNames = ci.getFeatureInstances().stream().map(f -> f.getName()).collect(Collectors.toList());

				subcomponentNames = ci.getComponentInstances()
						.stream()
						.map(c -> c.getName())
						.collect(Collectors.toList());

				visit(bas.get(0));

				ret.add(Annex$.MODULE$.apply(BLESS, pop()));
			}
		}
		return ret;
	}

	void handle(Object o) {
		System.err.println("Need to handle " + o.getClass().getCanonicalName());

		int i = 0;
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			System.err.println("  " + ste);
			if (i++ > 9) {
				break;
			}
		}
	}

	<T> IS<Z, T> l2is(List<T> l) {
		return VisitorUtil.toISZ(l);
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

	@Override
	public Boolean caseBLESSSubclause(BLESSSubclause object) {

		boolean doNotProve = object.isNo_proof();

		List<BTSAssertion> _assertions = new ArrayList<>();
		if (object.getAssert_clause() != null) {
			for (NamedAssertion na : object.getAssert_clause().getAssertions()) {
				visit(na);
				_assertions.add(pop());
			}
		}

		Option<BTSAssertion> _invariant = toNone();
		if (object.getInvariant() != null) {
			visit(object.getInvariant().getInv());
			_invariant = toSome(pop());
		}

		List<BTSVariableDeclaration> _variables = new ArrayList<>();
		if (object.getVariables() != null) {

			for (VariableDeclaration bv : object.getVariables().getBehavior_variables()) {
				visit(bv);
				_variables.add(pop());
			}
		}

		List<BTSStateDeclaration> _states = new ArrayList<>();
		for (BehaviorState bs : object.getStatesSection().getStates()) {
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
	public Boolean caseAssertion(Assertion object) {

		BTSAssertion ret = null;
		if (object.getNamedassertion() != null) {
			visit(object.getNamedassertion());
			ret = pop();
		}

		if (object.getNamelessassertion() != null) {
			visit(object.getNamelessassertion());
			ret = pop();
		}

		if (object.getNamelessenumeration() != null) {
			visit(object.getNamelessenumeration());
			ret = pop();
		}

		if (object.getNamelessfunction() != null) {
			visit(object.getNamelessfunction());
			ret = pop();
		}

		push(ret);

		return false;
	}

	@Override
	public Boolean caseNamedAssertion(NamedAssertion object) {
		handle(object);

		return false;
	}

	@Override
	public Boolean caseNamelessAssertion(NamelessAssertion object) {

		visit(object.getPredicate());
		BTSExp exp = pop();

		// TODO: I guess we never fleshed out assertion subtypes
		BTSAssertion a = BTSAssertion$.MODULE$.apply();
		push(a);

		return false;
	}

	@Override
	public Boolean caseValue(Value object) {

		if(object.getConstant() != null) {
			visit(object.getConstant());
		} else if(object.getValue_name() != null){
			visit(object.getValue_name());
		} else {
			throw new RuntimeException("need to handle other Value types");
		}

		return false;
	}

	@Override
	public Boolean caseValueName(ValueName object) {
		Name n = toSimpleName(object.getId().getQualifiedName());

		BTSNameExp ret = BTSNameExp$.MODULE$.apply(n);

		push(ret);

		return false;
	}

	@Override
	public Boolean caseConstant(Constant object) {
		BTSLiteralType.Type typ = null;
		String exp = null;

		if(object.getF() == null) {
			typ = BTSLiteralType.byName("BOOLEAN").get();
			exp = "false";
		} else if (object.getT() == null) {
			typ = BTSLiteralType.byName("BOOLEAN").get();
			exp = "true";
		} else {
			throw new RuntimeException("Need to handle other types of Constant");
		}

		push(BTSLiteralExp$.MODULE$.apply(typ, exp));
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
			visit(object.getState_assertion());
			assertion = toSome(pop());
		}

		BTSStateDeclaration bsd = BTSStateDeclaration$.MODULE$.apply(id, l2is(categories), assertion);
		push(bsd);

		return false;
	}

	@Override
	public Boolean caseVariableDeclaration(VariableDeclaration object) {

		Option<BTSVariableCategory.Type> category = toNone();
		if (object.isNonvolatile()) {
			category = toSome(BTSVariableCategory.byName("Nonvolatile").get());
		} else if (object.isShared()) {
			category = toSome(BTSVariableCategory.byName("Shared").get());
		} else if (object.isConstant()) {
			category = toSome(BTSVariableCategory.byName("Constant").get());
		} else if (object.isSpread()) {
			category = toSome(BTSVariableCategory.byName("Spread").get());
		} else if (object.isFinal()) {
			category = toSome(BTSVariableCategory.byName("Final").get());
		}

		Option<BTSExp> assignExpression = toNone();
		if (object.getExpression() != null) {
			visit(object.getExpression());
			assignExpression = toSome(pop());
		}

		Option<BTSAssertion> variableAssertion = toNone();
		if (object.getAssertion() != null) {
			visit(object.getAssertion());
			variableAssertion = toSome(pop());
		}

		Name name = toSimpleName(object.getVariable().getQualifiedName());

		if (object.getVariable().getTod().getTy() != null) {
			Type t = object.getVariable().getTod().getTy();

			throw new RuntimeException("Need to handle Type " + t);

		} else {
			TypeDeclaration t = object.getVariable().getTod().getRef();

			if(t.getQualifiedName() == null) {
				throw new RuntimeException("qualified name is null");
			} else {
				String s = convertToAadlName(t.getQualifiedName());
				Classifier c = Classifier$.MODULE$.apply(s);
				push(BTSClassifier$.MODULE$.apply(c));
			}
		}

		BTSType varType = pop();

		Option<BLESSIntConst> arraySize = toNone();

		BTSVariableDeclaration vd = BTSVariableDeclaration$.MODULE$.apply(name, category, varType, assignExpression,
				arraySize, variableAssertion);

		push(vd);

		return false;
	}

	private String convertToAadlName(String qualifiedName) {
		return qualifiedName.replaceAll("_colon", ":").replaceAll("_dot", ".");
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
			assert (!object.getActions().isAmp()); // TODO
			assert (object.getActions().isSemi()); // TODO

			visit(object.getActions());
			actions = toSome(pop());
		}

		Option<BTSAssertion> assertion = toNone();
		if (object.getAss() != null) {
			visit(object.getAss());
			assertion = toSome(pop());
		}

		BTSTransition bt = BTSTransition$.MODULE$.apply(label, l2is(_sourceStates), destState, _transitionCondition,
				actions, assertion);
		push(bt);

		return false;
	}

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

		if (object.getSkip() != null) {
			push(BTSSkipAction$.MODULE$.apply());
			return false;
		} else {
			// visit children via default case
			return null;
		}
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
	public Boolean caseCommunicationAction(CommunicationAction object) {

		if (object.getFp() != null) {
			throw new RuntimeException("Need to handle frozen ports");
		} else {
			defaultCase(object); // visit children via default case
		}

		return false;
	}

	@Override
	public Boolean caseSubprogramCall(SubprogramCall object) {
		String qname = object.getProcedure().getFullName();

		if(!subcomponentNames.contains(qname)) {
			if (object.getProcedure().getClassifier() instanceof SubprogramClassifier) {
				SubprogramClassifier st = (SubprogramClassifier) object.getProcedure().getClassifier();

				v.processSubprogramClassifier(st);
			}
		}


		assert subcomponentNames.contains(
				object.getProcedure().getQualifiedName()) : object.getProcedure().getQualifiedName()
				+ " not found";

		// TODO: handle correctly
		Name name = toSimpleName(object.getProcedure().getQualifiedName());

		List<BTSFormalExpPair> params = new ArrayList<>();
		if (object.getParameters() != null) {
			for (FormalActual fa : object.getParameters().getVariables()) {
				Option<Name> paramName = toNone();
				if (fa.getFormal() != null) {
					paramName = toSome(toName(fa.getFormal().getFullName()));
				}

				Option<BTSExp> exp = toNone();
				if (fa.getActual() != null) {
					if (fa.getActual().getValue() != null) {
						Name actualName = toSimpleName(fa.getActual().getValue().getId().getFullName());
						exp = toSome(BTSNameExp$.MODULE$.apply(actualName));
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

		visit(object.getPort());
		BTSExp variable = pop();

		BTSPortInAction a = BTSPortInAction$.MODULE$.apply(name, variable);
		push(a);

		return false;
	}

	@Override
	public Boolean casePortOutput(PortOutput object) {
		Name name = toName(object.getPort().getName());

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
	public Boolean caseTransitionLabel(TransitionLabel object) {
		Name id = toSimpleName(object.getId());

		Option<Z> priority = null;
		if (object.getPriority() != null) {
			Z value = Z$.MODULE$.apply(Integer.parseInt(object.getPriority().getPriority()));
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
		if (object.getFrozen() != null) {
			for (Port p : object.getFrozen().getFrozen()) {
				frozenPorts.add(toName(p));
			}
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
		if (object.getStop() != null) {
			assert ret == null;

			ret = BTSDispatchTriggerStop.apply();
		}

		if (object.getTimeout() != null) {
			assert ret == null;
			List<Name> ports = new ArrayList<>();
			if (object.isLp()) {
				for (NamedElement p : object.getPorts()) {
					ports.add(toName(p.getFullName()));
				}
			}

			Option<BTSBehaviorTime> time = toNone();
			if (object.getTime() != null) {
				visit(object.getTime());
				time = toSome(pop());
			}

			ret = BTSDispatchTriggerTimeout$.MODULE$.apply(l2is(ports), time);
		}

		if (object.getPort() != null) {
			assert ret == null;

			ret = BTSDispatchTriggerPort$.MODULE$.apply(toName(object.getPort().getFullName()));
		}

		assert ret != null;
		push(ret);

		return false;
	}

	@Override
	public Boolean caseExecuteCondition(ExecuteCondition object) {

		handle(object);

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