package org.sireum.aadl.osate.architecture;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.osate.aadl2.DataClassifier;
import org.osate.aadl2.Element;
import org.osate.aadl2.Feature;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.Port;
import org.osate.aadl2.StringLiteral;
import org.osate.aadl2.SubprogramSubcomponent;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.ba.aadlba.ActualPortHolder;
import org.osate.ba.aadlba.AssignmentAction;
import org.osate.ba.aadlba.BehaviorAction;
import org.osate.ba.aadlba.BehaviorActionBlock;
import org.osate.ba.aadlba.BehaviorActionCollection;
import org.osate.ba.aadlba.BehaviorAnnex;
import org.osate.ba.aadlba.BehaviorCondition;
import org.osate.ba.aadlba.BehaviorState;
import org.osate.ba.aadlba.BehaviorTime;
import org.osate.ba.aadlba.BehaviorTransition;
import org.osate.ba.aadlba.BehaviorVariable;
import org.osate.ba.aadlba.BehaviorVariableHolder;
import org.osate.ba.aadlba.BinaryAddingOperator;
import org.osate.ba.aadlba.BinaryNumericOperator;
import org.osate.ba.aadlba.ClassifierPropertyReference;
import org.osate.ba.aadlba.DataComponentReference;
import org.osate.ba.aadlba.DataSubcomponentHolder;
import org.osate.ba.aadlba.DispatchCondition;
import org.osate.ba.aadlba.DispatchConjunction;
import org.osate.ba.aadlba.DispatchTrigger;
import org.osate.ba.aadlba.DispatchTriggerLogicalExpression;
import org.osate.ba.aadlba.Factor;
import org.osate.ba.aadlba.IntegerValue;
import org.osate.ba.aadlba.MultiplyingOperator;
import org.osate.ba.aadlba.PortSendAction;
import org.osate.ba.aadlba.Relation;
import org.osate.ba.aadlba.SimpleExpression;
import org.osate.ba.aadlba.SubprogramCallAction;
import org.osate.ba.aadlba.SubprogramSubcomponentHolder;
import org.osate.ba.aadlba.Term;
import org.osate.ba.aadlba.UnaryAddingOperator;
import org.osate.ba.aadlba.UnaryBooleanOperator;
import org.osate.ba.aadlba.ValueExpression;
import org.osate.ba.aadlba.impl.BehaviorAnnexImpl;
import org.osate.ba.aadlba.util.AadlBaSwitch;
import org.sireum.Option;
import org.sireum.Z;
import org.sireum.Z$;
import org.sireum.aadl.osate.util.BAUtils;
import org.sireum.hamr.ir.AadlASTFactory;
import org.sireum.hamr.ir.Annex;
import org.sireum.hamr.ir.Annex$;
import org.sireum.hamr.ir.BLESSIntConst;
import org.sireum.hamr.ir.BTSAccessExp;
import org.sireum.hamr.ir.BTSAccessExp$;
import org.sireum.hamr.ir.BTSAction;
import org.sireum.hamr.ir.BTSAssertedAction;
import org.sireum.hamr.ir.BTSAssertedAction$;
import org.sireum.hamr.ir.BTSAssertion;
import org.sireum.hamr.ir.BTSAssignmentAction$;
import org.sireum.hamr.ir.BTSBLESSAnnexClause;
import org.sireum.hamr.ir.BTSBLESSAnnexClause$;
import org.sireum.hamr.ir.BTSBehaviorActions;
import org.sireum.hamr.ir.BTSBehaviorActions$;
import org.sireum.hamr.ir.BTSBinaryExp;
import org.sireum.hamr.ir.BTSBinaryExp$;
import org.sireum.hamr.ir.BTSBinaryOp;
import org.sireum.hamr.ir.BTSClassifier$;
import org.sireum.hamr.ir.BTSDispatchCondition;
import org.sireum.hamr.ir.BTSDispatchCondition$;
import org.sireum.hamr.ir.BTSDispatchConjunction;
import org.sireum.hamr.ir.BTSDispatchConjunction$;
import org.sireum.hamr.ir.BTSDispatchTrigger;
import org.sireum.hamr.ir.BTSDispatchTriggerPort$;
import org.sireum.hamr.ir.BTSExecuteConditionExp$;
import org.sireum.hamr.ir.BTSExecutionOrder;
import org.sireum.hamr.ir.BTSExp;
import org.sireum.hamr.ir.BTSFormalExpPair;
import org.sireum.hamr.ir.BTSFormalExpPair$;
import org.sireum.hamr.ir.BTSNameExp;
import org.sireum.hamr.ir.BTSNameExp$;
import org.sireum.hamr.ir.BTSPortOutAction$;
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
import org.sireum.hamr.ir.BTSVariableCategory;
import org.sireum.hamr.ir.BTSVariableDeclaration;
import org.sireum.hamr.ir.BTSVariableDeclaration$;
import org.sireum.hamr.ir.Classifier;
import org.sireum.hamr.ir.Classifier$;
import org.sireum.hamr.ir.Name;

public class BAVisitor extends AadlBaSwitch<Boolean> {

	Visitor v;

	public final static String ANNEX_TYPE = "behavior_specification";

	private List<String> path = null;

	private List<String> featureNames = null;
	private List<String> subcomponentNames = null;

	private boolean TODO_HALT = false;

	BTSExecutionOrder.Type Sequential = BTSExecutionOrder.byName("Sequential").get();

	public BAVisitor(Visitor v) {
		this.v = v;
	}

	protected final static AadlASTFactory factory = new AadlASTFactory();

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

	public List<Annex> visit(ComponentInstance ci, List<String> path) {
		List<Annex> ret = new ArrayList<>();
		if (ci.getClassifier() != null) {
			List<BehaviorAnnexImpl> bas = EcoreUtil2.eAllOfType(ci.getClassifier(),
					BehaviorAnnexImpl.class);
			assert bas.size() <= 1 : "Expecting at most one BA clause for " + ci.getFullName() + " but found "
					+ bas.size();

			if (bas.size() == 1) {
				this.path = path;

				featureNames = ci.getFeatureInstances().stream().map(f -> f.getName()).collect(Collectors.toList());

				subcomponentNames = ci.getComponentInstances().stream().map(c -> c.getName())
						.collect(Collectors.toList());

				visit(bas.get(0));

				ret.add(Annex$.MODULE$.apply(ANNEX_TYPE, pop()));
			}
		}
		return ret;
	}

	@Override
	public Boolean caseBehaviorAnnex(BehaviorAnnex o) {

		boolean doNotProve = true; // NA for BA

		List<BTSAssertion> _assertions = new ArrayList<>(); // NA for BA

		Option<BTSAssertion> _invariant = toNone(); // NA for BA

		List<BTSVariableDeclaration> _variables = new ArrayList<>();
		if (o.getVariables() != null) {
			for (BehaviorVariable bv : o.getVariables()) {
				visit(bv);
				_variables.addAll(pop());
			}
		}

		List<BTSStateDeclaration> _states = new ArrayList<>();
		for (BehaviorState bs : o.getStates()) {
			visit(bs);
			_states.add(pop());
		}

		List<BTSTransition> _transitions = new ArrayList<>();
		for (BehaviorTransition bt : o.getTransitions()) {
			visit(bt);
			_transitions.add(pop());
		}

		BTSBLESSAnnexClause b = BTSBLESSAnnexClause$.MODULE$.apply(doNotProve, VisitorUtil.toISZ(_assertions),
				_invariant,
				VisitorUtil.toISZ(_variables), VisitorUtil.toISZ(_states), VisitorUtil.toISZ(_transitions));
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

		Option<BTSAssertion> assertion = toNone(); // NA for BA

		BTSStateDeclaration bsd = BTSStateDeclaration$.MODULE$.apply(id, VisitorUtil.toISZ(categories), assertion);
		push(bsd);

		return false;
	}

	@Override
	public Boolean caseBehaviorTransition(BehaviorTransition object) {

		Name id = toSimpleName(object.getName());

		Z value = Z$.MODULE$.apply(object.getPriority());

		Option<Z> priority = toSome(value);

		BTSTransitionLabel label = BTSTransitionLabel$.MODULE$.apply(id, priority);

		List<Name> _sourceStates = new ArrayList<>();
		BehaviorState src = object.getSourceState();
		_sourceStates.add(toSimpleName(src.getName()));

		BehaviorState dest = object.getDestinationState();
		String destName = dest.getName(); // just need name
		Name destState = toSimpleName(destName);

		Option<BTSTransitionCondition> _transitionCondition = toNone();
		BehaviorCondition bc = object.getCondition();
		if (bc != null) {
			visit(bc);
			if (bc instanceof DispatchCondition) {
				_transitionCondition = toSome(pop());
			} else if (bc instanceof ValueExpression) {
				_transitionCondition = toSome(BTSExecuteConditionExp$.MODULE$.apply(pop()));
			} else {
				throw new RuntimeException("handle transition condition: " + bc);
			}
		}

		Option<BTSBehaviorActions> actions = toNone();
		if (object.getActionBlock() != null) {
			visit(object.getActionBlock());
			actions = toSome(pop());
		}

		Option<BTSAssertion> assertion = toNone(); // NA for BA

		BTSTransition bt = BTSTransition$.MODULE$.apply(label, VisitorUtil.toISZ(_sourceStates), destState,
				_transitionCondition,
				actions, assertion);
		push(bt);

		return false;
	}

	@Override
	public Boolean caseBehaviorActionBlock(BehaviorActionBlock object) {
		BehaviorTime bt = object.getTimeout();
		assert bt == null : "Need to handle " + bt;

		visit(object.getContent());
		Object r = pop();
		if (r instanceof BTSBehaviorActions) {
			push(r);
		} else {
			BTSAction baa = (BTSAction) r;

			Option<BTSAssertion> pre = toNone();
			Option<BTSAssertion> post = toNone();
			BTSAssertedAction baa2 = BTSAssertedAction$.MODULE$.apply(pre, baa, post);

			push(BTSBehaviorActions$.MODULE$.apply(Sequential, VisitorUtil.toISZ(baa2)));
		}
		return false;
	}

	@Override
	public Boolean casePortSendAction(PortSendAction object) {
		Name name = toName(object.getPort().getPort().getName());

		Option<BTSExp> exp = toNone();
		if (object.getValueExpression() != null) {
			visit(object.getValueExpression());
			exp = toSome(pop());
		}

		push(BTSPortOutAction$.MODULE$.apply(name, exp));

		return false;
	}

	@Override
	public Boolean caseAssignmentAction(AssignmentAction object) {
		visit(object.getTarget());
		BTSExp lhs = pop();

		visit(object.getValueExpression());
		BTSExp rhs = pop();

		push(BTSAssignmentAction$.MODULE$.apply(lhs, rhs));

		return false;
	}

	@Override
	public Boolean caseBehaviorActionCollection(BehaviorActionCollection object) {

		List<BTSAssertedAction> actions = new ArrayList<>();
		for (BehaviorAction ba : object.getActions()) {
			visit(ba);
			BTSAction action = pop();

			Option<BTSAssertion> pre = toNone(); // NA for BA
			Option<BTSAssertion> post = toNone(); // NA for BA
			actions.add(BTSAssertedAction$.MODULE$.apply(pre, action, post));
		}

		// TODO: how to determine execution order
		push(BTSBehaviorActions$.MODULE$.apply(Sequential, VisitorUtil.toISZ(actions)));

		return false;
	}

	@Override
	public Boolean caseSubprogramSubcomponentHolder(SubprogramSubcomponentHolder object) {

		List<IntegerValue> arrayIndexes = object.getArrayIndexes();
		assert (arrayIndexes.isEmpty());

		// TODO: what is the difference b/w subcomponent and subprogramsubcomponent?
		assert (object.getElement() == object.getSubcomponent());
		assert (object.getSubcomponent() == object.getSubprogramSubcomponent());

		SubprogramSubcomponent ssc = object.getSubprogramSubcomponent();

		push(toName(ssc.getName()));

		return false;
	}

	@Override
	public Boolean caseSubprogramCallAction(SubprogramCallAction object) {
		visit(object.getSubprogram());
		Name name = pop();

		List<Feature> features = null;
		if (object.getSubprogram().getElement() instanceof SubprogramSubcomponent) {
			SubprogramSubcomponent q = (SubprogramSubcomponent) object.getSubprogram().getElement();
			features = q.getAllFeatures();
		} else {
			throw new RuntimeException("Unexpected " + object.getSubprogram().getElement());
		}

		assert features.size() == object.getParameterLabels().size() : "Apparantly BA allows this";

		List<BTSFormalExpPair> params = new ArrayList<>();
		for (int index = 0; index < object.getParameterLabels().size(); index++) {
			Feature f = features.get(index);

			visit(object.getParameterLabels().get(index));
			BTSNameExp ne = pop();

			// TODO:
			Option<Name> paramName = toSome(toSimpleName(f.getName()));
			params.add(BTSFormalExpPair$.MODULE$.apply(paramName, toSome(ne)));
		}

		push(BTSSubprogramCallAction$.MODULE$.apply(name, VisitorUtil.toISZ(params)));

		return false;
	}

	@Override
	public Boolean caseDispatchTriggerLogicalExpression(DispatchTriggerLogicalExpression object) {
		List<BTSDispatchConjunction> conjunctions = new ArrayList<>();

		for (DispatchConjunction dc : object.getDispatchConjunctions()) {
			List<BTSDispatchTrigger> triggers = new ArrayList<>();
			for(DispatchTrigger t : dc.getDispatchTriggers()) {
				if (t instanceof ActualPortHolder) {
					ActualPortHolder e = (ActualPortHolder) t;

					String n = e.getPort().getFullName();
					Name portName = toName(n);

					triggers.add(BTSDispatchTriggerPort$.MODULE$.apply(portName));
				} else {
					throw new RuntimeException("need to handle " + t);
				}
			}
			conjunctions.add(BTSDispatchConjunction$.MODULE$.apply(VisitorUtil.toISZ(triggers)));
		}

		push(conjunctions);

		return false;
	}

	@Override
	public Boolean caseDispatchCondition(DispatchCondition object) {
		List<BTSDispatchConjunction> dispatchTriggers = new ArrayList<>();
		if (object.getDispatchTriggerCondition() != null) {
			visit(object.getDispatchTriggerCondition());
			dispatchTriggers.addAll(pop());
		}

		List<Name> frozenPorts = new ArrayList<>();
		for (ActualPortHolder p : object.getFrozenPorts()) {
			frozenPorts.add(toName(p.getPort().getName()));
		}

		BTSDispatchCondition bdc = BTSDispatchCondition$.MODULE$.apply(VisitorUtil.toISZ(dispatchTriggers),
				VisitorUtil.toISZ(frozenPorts));
		push(bdc);

		return false;
	}

	@Override
	public Boolean caseBehaviorVariable(BehaviorVariable object) {

		Option<BTSVariableCategory.Type> category = toNone();

		List<BTSVariableDeclaration> names = new ArrayList<>();

		DataClassifier dc = object.getDataClassifier();
		this.v.processDataType(dc);

		Classifier c = Classifier$.MODULE$.apply(dc.getQualifiedName());
		BTSType varType = BTSClassifier$.MODULE$.apply(c);

		Name name = toSimpleName(object.getName());

		Option<BTSExp> assignExpression = toNone();
		assert object.getOwnedValueConstant() == null : "Need to handle ba variable const init " + object;

		Option<BLESSIntConst> arraySize = toNone();

		Option<BTSAssertion> variableAssertion = toNone(); // NA for BA

		BTSVariableDeclaration vd = BTSVariableDeclaration$.MODULE$.apply(name, category, varType, assignExpression,
				arraySize, variableAssertion);
		names.add(vd);

		push(names);

		return false;
	}

	@Override
	public Boolean caseDataSubcomponentHolder(DataSubcomponentHolder object) {

		NamedElement ne = object.getElement();
		push(ne.getName());

		return false;
	}

	@Override
	public Boolean caseBehaviorVariableHolder(BehaviorVariableHolder object) {

		List<IntegerValue> arrayIndexes = object.getArrayIndexes();
		assert (arrayIndexes.isEmpty());

		BehaviorVariable bv = object.getBehaviorVariable();
		push(BTSNameExp$.MODULE$.apply(toSimpleName(bv.getName())));

		return false;
	}

	@Override
	public Boolean caseDataComponentReference(DataComponentReference object) {

		assert (object.getData().size() >= 2); // name exp followed by access ??

		visit(object.getData().get(0));
		BTSNameExp exp = pop();

		visit(object.getData().get(1));
		String attributeName = pop();
		BTSAccessExp bts = BTSAccessExp$.MODULE$.apply(exp, attributeName);

		for (int i = 2; i < object.getData().size(); i++) {
			visit(object.getData().get(i));
			attributeName = pop();
			bts = BTSAccessExp$.MODULE$.apply(bts, attributeName);
		}

		push(bts);

		return false;
	}

	@Override
	public Boolean caseActualPortHolder(ActualPortHolder object) {
		push(BTSNameExp$.MODULE$.apply(toName(object.getPort().getName())));

		return false;
	}

	@Override
	public Boolean caseFactor(Factor object) {

		BinaryNumericOperator bno = object.getBinaryNumericOperator();
		UnaryBooleanOperator ubo = object.getUnaryBooleanOperator();

		visit(object.getFirstValue());
		BTSExp lhs = pop();

		if (object.getSecondValue() != null) {
			assert (!BAUtils.isNoneEnumerator(bno));

			visit(object.getSecondValue());
			BTSExp rhs = pop();

			BTSBinaryOp.Type op = BAUtils.toBinaryOp(bno);

			push(BTSBinaryExp$.MODULE$.apply(op, lhs, rhs));
		} else {
			if (BAUtils.isNoneEnumerator(ubo)) {
				push(lhs);
			} else {
				BAUtils.convertToUnaryExp(lhs, ubo);
			}
		}

		return false;
	}

	@Override
	public Boolean caseClassifierPropertyReference(ClassifierPropertyReference object) {

		// TODO: this handles enums only
		// inspired by https://github.com/osate/osate2/blob/master/ba/org.osate.ba/src/org/osate/ba/utils/AadlBaUtils.java#L1873

		assert (object.getProperties().size() == 2);

		// The first property name is supposed to be a property association
		// Enumerators. Don't need to check it.

		Element el = object.getProperties().get(1).getProperty().getElement();
		if (el instanceof StringLiteral) {
			StringLiteral sl = ((StringLiteral) el);
			BTSNameExp ne = BTSNameExp$.MODULE$.apply(toSimpleName(object.getClassifier().getQualifiedName()));
			push(BTSAccessExp$.MODULE$.apply(ne, sl.getValue()));
		} else {
			throw new RuntimeException();
		}

		return false;
	}

	@Override
	public Boolean caseTerm(Term object) {
		assert (!object.getFactors().isEmpty());

		List<MultiplyingOperator> mos = object.getMultiplyingOperators();

		List<BTSExp> expressions = new ArrayList<>();
		for (Factor f : object.getFactors()) {
			visit(f);
			expressions.add(pop());
		}

		if(expressions.size() > 1) {
			convertToBinaryExp(expressions, mos);
		} else {
			assert (expressions.size() == 1);
			push(expressions.get(0));
		}

		return false;
	}

	@Override
	public Boolean caseSimpleExpression(SimpleExpression object) {

		assert (object.getTerms().size() > 0);

		List<BinaryAddingOperator> binOps = object.getBinaryAddingOperators();
		UnaryAddingOperator unaryOp = object.getUnaryAddingOperator();

		List<BTSExp> expressions = new ArrayList<>();
		for (Term t : object.getTerms()) {
			visit(t);
			expressions.add(pop());
		}

		if (expressions.size() > 1) {
			push(convertToBinaryExp(expressions, binOps));
		} else {
			if (BAUtils.isNoneEnumerator(unaryOp)) {
				push(expressions.get(0));
			} else {
				push(BAUtils.convertToUnaryExp(expressions.get(0), unaryOp));
			}
		}
		return false;
	}

	/**
	 * converts a list of expressions into a binary exp.  Must be the case that
	 * size of binOps is 1 less than size of expressions.
	 *
	 * WARNING - destructively modifies {@code expressions} list
	 * @param expressions
	 * @param binOps
	 * @return
	 */
	private BTSBinaryExp convertToBinaryExp(List<BTSExp> expressions, List<? extends Enumerator> binOps) {
		assert binOps.size() == expressions.size() - 1 : "There are " + binOps.size() + " but " + expressions.size()
				+ " expressions";

		// TODO: operator precedence in BA (or is it just paren expr?)

		for (int i = 0; i < expressions.size() - 1; i++) {
			BTSBinaryOp.Type op = BAUtils.toBinaryOp(binOps.get(i).getLiteral());

			// treat list as a stack
			BTSBinaryExp be = BTSBinaryExp$.MODULE$.apply(op, expressions.get(i), expressions.get(i + 1));
			expressions.set(i + 1, be);
		}

		return (BTSBinaryExp) expressions.get(expressions.size() - 1);
	}

	@Override
	public Boolean caseRelation(Relation object) {

		visit(object.getFirstExpression());
		BTSExp lhs = pop();

		if (object.getSecondExpression() != null) {
			BTSBinaryOp.Type op = BAUtils.toBinaryOp(object.getRelationalOperator().getLiteral());

			visit(object.getSecondExpression());
			BTSExp rhs = pop();

			push(BTSBinaryExp$.MODULE$.apply(op, lhs, rhs));
		} else {
			assert (BAUtils.isNoneEnumerator(object.getRelationalOperator()));

			push(lhs);
		}

		return false;
	}

	@Override
	public Boolean caseValueExpression(ValueExpression object) {

		assert (object.getRelations().size() >= 1);

		List<BTSExp> expressions = new ArrayList<>();
		for (Relation r : object.getRelations()) {
			visit(r);
			expressions.add(pop());
		}

		if (expressions.size() > 1) {
			push(convertToBinaryExp(expressions, object.getLogicalOperators()));
		} else {
			push(expressions.get(0));
		}

		return false;
	}

	public Boolean visit(EObject o) {
		assert (isSwitchFor(o.eClass().getEPackage()));
		return doSwitch(o);
	}

	Object result = null;

	void push(Object o) {
		assert result == null : "Stack not empty: " + result;
		result = o;
	}

	@SuppressWarnings("unchecked")
	<T> T pop() {
		assert (result != null);
		T ret = (T) result;
		result = null;
		return ret;
	}

	private void todo(Object o, String msg) {
		RuntimeException e = new RuntimeException(msg + ": " + o);

		if (TODO_HALT) {
			throw e;
		} else {
			StackTraceElement ste = e.getStackTrace()[1];

			System.err.println(e.getMessage() + ": " + ste);
		}
	}

}
