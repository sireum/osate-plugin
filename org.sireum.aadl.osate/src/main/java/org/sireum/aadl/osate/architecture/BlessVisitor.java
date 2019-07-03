package org.sireum.aadl.osate.architecture;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.IS;
import org.sireum.Option;
import org.sireum.Z;
import org.sireum.Z$;
import org.sireum.aadl.ir.Annex;
import org.sireum.aadl.ir.Annex$;
import org.sireum.aadl.ir.BLESSExpression;
import org.sireum.aadl.ir.BLESSIntConst;
import org.sireum.aadl.ir.BLESSType;
import org.sireum.aadl.ir.BTSAction;
import org.sireum.aadl.ir.BTSAssertion;
import org.sireum.aadl.ir.BTSBLESSAnnexClause;
import org.sireum.aadl.ir.BTSBLESSAnnexClause$;
import org.sireum.aadl.ir.BTSExecuteCondition;
import org.sireum.aadl.ir.BTSStateCategory;
import org.sireum.aadl.ir.BTSStateDeclaration;
import org.sireum.aadl.ir.BTSStateDeclaration$;
import org.sireum.aadl.ir.BTSTransition;
import org.sireum.aadl.ir.BTSTransition$;
import org.sireum.aadl.ir.BTSTransitionCondition;
import org.sireum.aadl.ir.BTSTransitionLabel;
import org.sireum.aadl.ir.BTSTransitionLabel$;
import org.sireum.aadl.ir.BTSVariableCategory;
import org.sireum.aadl.ir.BTSVariableDeclaration;
import org.sireum.aadl.ir.BTSVariableDeclaration$;
import org.sireum.aadl.ir.Name;

import edu.ksu.bless.assertion.assertion.Assertion;
import edu.ksu.bless.assertion.assertion.ValueConstant;
import edu.ksu.bless.bless.bLESS.BLESSAnnexSubclause;
import edu.ksu.bless.bless.bLESS.BehaviorState;
import edu.ksu.bless.bless.bLESS.BehaviorTransition;
import edu.ksu.bless.bless.bLESS.BehaviorVariable;
import edu.ksu.bless.bless.bLESS.Declarator;
import edu.ksu.bless.bless.bLESS.DispatchCondition;
import edu.ksu.bless.bless.bLESS.TransitionLabel;
import edu.ksu.bless.bless.bLESS.impl.BLESSAnnexSubclauseImpl;
import edu.ksu.bless.bless.bLESS.util.BLESSSwitch;
import edu.ksu.bless.subbless.subBLESS.AssertedAction;
import edu.ksu.bless.subbless.subBLESS.Expression;

public class BlessVisitor {

	protected final org.sireum.aadl.ir.AadlASTFactory factory = new org.sireum.aadl.ir.AadlASTFactory();

	BTSVisitor bv = new BTSVisitor();

	public static String BLESS = "BLESS";

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

	class BTSVisitor extends BLESSSwitch<Boolean> {
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

		@Override
		public Boolean caseBLESSAnnexSubclause(BLESSAnnexSubclause object) {

			boolean doNotProve = object.isNo_proof();

			List<BTSAssertion> _assertions = new ArrayList<>();
			if (object.getAssert_clause() != null) {
				for (Assertion ass : object.getAssert_clause().getAssertions()) {
					// TODO assertion visitor
				}
			}

			List<BTSAssertion> _invariants = new ArrayList<>();
			Assertion inv = object.getInvariant().getInv();
			// TODO assertion visitor. Note that there can only be one invariant

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

			BTSBLESSAnnexClause b = BTSBLESSAnnexClause$.MODULE$.apply(doNotProve, l2is(_assertions), l2is(_invariants),
					l2is(_variables), l2is(_states), l2is(_transitions));
			push(b);

			return false;
		}

		@Override
		public Boolean caseBehaviorState(BehaviorState object) {
			Name id = factory.name(VisitorUtil.toIList(object.getQualifiedName()), null);

			BTSStateCategory.Type category = null;
			if (object.isFinal()) {
				category = BTSStateCategory.byName("").get(); // TODO no Final state in air????
				throw new RuntimeException("No Final state in air");
			} else if (object.isInitial()) {
				category = BTSStateCategory.byName("Initial").get();
			} else if (object.isComplete()) {
				category = BTSStateCategory.byName("Complete").get();
			} else {
				throw new RuntimeException("No category provided");
			}

			Assertion ass = object.getState_assertion(); // TODO
			Option<BTSAssertion> assertion = org.sireum.None.apply();

			BTSStateDeclaration bsd = BTSStateDeclaration$.MODULE$.apply(id, category, assertion);
			push(bsd);

			return false;
		}

		@Override
		public Boolean caseBehaviorVariable(BehaviorVariable object) {

			// TODO slang enum types and category needs to be a seq
			Option<BTSVariableCategory.Type> category = BTSVariableCategory.byName("Final");

			BLESSType varType = BLESSType.apply(); // TODO update air

			Expression exp = object.getExpression();
			BLESSExpression assignExpression = BLESSExpression.apply(); // TODO update air

			Assertion ass = object.getAssertion();
			Option<BTSAssertion> variableAssertion = org.sireum.None.apply(); // TODO update air

			List<BTSVariableDeclaration> names = new ArrayList<>();
			for (Declarator d : object.getVariable_names()) {
				Name name = factory.name(VisitorUtil.toIList(d.getVariable()), null);

				Option<BLESSIntConst> arraySize = org.sireum.None.apply();

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
				_sourceStates.add(factory.name(VisitorUtil.toIList(srcName), null));
			}

			BehaviorState dest = object.getDestination();
			String destName = dest.getQualifiedName(); // just need name
			Name destState = factory.name(VisitorUtil.toIList(destName), null);

			DispatchCondition dc = object.getDispatch(); // TODO air fix condition is optional
			BTSTransitionCondition transitionCondition = BTSExecuteCondition.apply();

			List<BTSAction> _actions = new ArrayList<>();
			if (object.getActions() != null) {
				for (AssertedAction aa : object.getActions().getAction()) {
					// TODO subbless stuff
				}
			}

			Assertion ass = object.getAss();
			Option<BTSAssertion> assertion = org.sireum.None.apply(); // TODO assertion stuff

			BTSTransition bt = BTSTransition$.MODULE$.apply(label, l2is(_sourceStates), destState, transitionCondition,
					l2is(_actions), assertion);
			push(bt);

			return false;
		}

		@Override
		public Boolean caseTransitionLabel(TransitionLabel object) {
			Name id = factory.name(VisitorUtil.toIList(object.getId()), null);

			Option<Z> priority = null;
			if (object.getPriority() != null) {
				Z value = Z$.MODULE$.apply(Integer.parseInt(object.getPriority()));
				priority = org.sireum.Some$.MODULE$.apply(value);
			} else {
				priority = org.sireum.None.apply();
			}

			BTSTransitionLabel label = BTSTransitionLabel$.MODULE$.apply(id, priority);
			push(label);

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
			return doSwitch(o);
		}

		private <T> IS<Z, T> l2is(List<T> l) {
			return VisitorUtil.list2ISZ(l);
		}
	}

}
