package org.sireum.aadl.osate.architecture;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.Option;
import org.sireum.aadl.gumbo.gumbo.BinaryExpr;
import org.sireum.aadl.gumbo.gumbo.GumboSubclause;
import org.sireum.aadl.gumbo.gumbo.IntLit;
import org.sireum.aadl.gumbo.gumbo.InvSpec;
import org.sireum.aadl.gumbo.gumbo.RealLitExpr;
import org.sireum.aadl.gumbo.gumbo.UnaryExpr;
import org.sireum.aadl.gumbo.gumbo.util.GumboSwitch;
import org.sireum.aadl.osate.util.GumboUtils;
import org.sireum.aadl.osate.util.SlangUtils;
import org.sireum.hamr.ir.Annex;
import org.sireum.hamr.ir.Annex$;
import org.sireum.hamr.ir.AnnexLib;
import org.sireum.hamr.ir.BinaryExp$;
import org.sireum.hamr.ir.BinaryOp;
import org.sireum.hamr.ir.Compute;
import org.sireum.hamr.ir.Exp;
import org.sireum.hamr.ir.Guarantee;
import org.sireum.hamr.ir.GumboInvariant;
import org.sireum.hamr.ir.GumboInvariant$;
import org.sireum.hamr.ir.GumboSubclause$;
import org.sireum.hamr.ir.Integration;
import org.sireum.hamr.ir.LiteralExp$;
import org.sireum.hamr.ir.LiteralType;
import org.sireum.hamr.ir.StateVar;
import org.sireum.hamr.ir.UnaryExp$;
import org.sireum.hamr.ir.UnaryOp;

public class GumboVisitor extends GumboSwitch<Boolean> implements AnnexVisitor {

	Visitor v;

	private final String ANNEX_TYPE = "gumbo";
	private List<String> path = null;

	public GumboVisitor(Visitor v) {
		this.v = v;
	}

	@Override
	public List<Annex> visit(ComponentInstance ci, List<String> currentPath) {
		return visit(ci.getComponentClassifier(), currentPath);
	}

	@Override
	public List<Annex> visit(Classifier c, List<String> path) {
		List<Annex> ret = new ArrayList<>();

		List<GumboSubclause> bas = EcoreUtil2.eAllOfType(c, GumboSubclause.class);
		assert bas.size() <= 1
				: "Expecting at most one Gumbo clause for " + c.getFullName() + " but found " + bas.size();

		if (bas.size() == 1) {
			this.path = path;

			visit(bas.get(0));

			ret.add(Annex$.MODULE$.apply(ANNEX_TYPE, pop()));
		}

		return ret;
	}

	@Override
	public Boolean caseGumboSubclause(GumboSubclause object) {
		List<StateVar> _state = new ArrayList<>();
		if (object.getSpecs().getState() != null) {
			visit(object.getSpecs().getState());
		}

		List<GumboInvariant> _invariants = new ArrayList<>();
		if (object.getSpecs().getInvariants() != null) {
			for (InvSpec s : object.getSpecs().getInvariants().getSpecs()) {
				String displayName = s.getDisplayName();
				visit(s.getExpr());
				_invariants.add(GumboInvariant$.MODULE$.apply(displayName, pop()));
			}
		}

		Option<Integration> _integration = SlangUtils.toNone();
		if (object.getSpecs().getIntegration() != null) {
			visit(object.getSpecs().getIntegration());
		}

		List<Guarantee> _initializes = new ArrayList<>();
		if (object.getSpecs().getInitialize() != null) {
			visit(object.getSpecs().getInitialize());
		}

		Option<Compute> _compute = SlangUtils.toNone();
		if (object.getSpecs().getCompute() != null) {
			visit(object.getSpecs());
		}

		push(GumboSubclause$.MODULE$.apply(VisitorUtil.toISZ(_state), VisitorUtil.toISZ(_invariants),
				VisitorUtil.toISZ(_initializes), _integration, _compute));

		return false;
	}


	@Override
	public Boolean caseBinaryExpr(BinaryExpr object) {
		visit(object.getLeft());
		Exp lhs = pop();

		visit(object.getRight());
		Exp rhs = pop();

		BinaryOp.Type op = GumboUtils.toBinaryOp(object.getOp());

		push(BinaryExp$.MODULE$.apply(op, lhs, rhs, SlangUtils.toNone()));

		return false;
	}

	@Override
	public Boolean caseUnaryExpr(UnaryExpr object) {
		visit(object.getExpr());
		Exp exp = pop();

		UnaryOp.Type op = GumboUtils.toUnaryOp(object.getOp());

		push(UnaryExp$.MODULE$.apply(op, exp, SlangUtils.toNone()));

		return false;
	}

	@Override
	public Boolean caseIntLit(IntLit object) {

		LiteralType.Type typ = LiteralType.byName("Integer").get();

		push(LiteralExp$.MODULE$.apply(typ, object.getValue(), SlangUtils.toNone()));

		return false;
	}

	@Override
	public Boolean caseRealLitExpr(RealLitExpr object) {

		LiteralType.Type typ = LiteralType.byName("Real").get();

		push(LiteralExp$.MODULE$.apply(typ, object.getVal(), SlangUtils.toNone()));

		return false;
	}

	public Boolean visit(EObject o) {
		assert (isSwitchFor(o.eClass().getEPackage()));
		return doSwitch(o);
	}

	@Override
	public Boolean defaultCase(EObject o) {
		for (EObject child : o.eContents()) {
			visit(child);
		}
		return null;
	}

	Object result = null;

	void push(Object o) {
		assert result == null : "Stack not empty: " + result;
		result = o;
	}

	@SuppressWarnings("unchecked")
	<T> T pop() {
		assert result != null : "Stack is empty";
		T ret = (T) result;
		result = null;
		return ret;
	}

	@Override
	public List<AnnexLib> buildAnnexLibraries(Element arg0) {
		return VisitorUtil.iList();
	}
}
