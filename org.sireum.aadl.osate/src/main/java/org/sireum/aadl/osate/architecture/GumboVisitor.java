package org.sireum.aadl.osate.architecture;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.EcoreUtil2;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.ComponentType;
import org.osate.aadl2.DataClassifier;
import org.osate.aadl2.DataSubcomponent;
import org.osate.aadl2.Element;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.Option;
import org.sireum.aadl.gumbo.gumbo.BinaryExpr;
import org.sireum.aadl.gumbo.gumbo.DataRefExpr;
import org.sireum.aadl.gumbo.gumbo.GumboSubclause;
import org.sireum.aadl.gumbo.gumbo.IntLit;
import org.sireum.aadl.gumbo.gumbo.InvSpec;
import org.sireum.aadl.gumbo.gumbo.OtherDataRef;
import org.sireum.aadl.gumbo.gumbo.RealLitExpr;
import org.sireum.aadl.gumbo.gumbo.UnaryExpr;
import org.sireum.aadl.gumbo.gumbo.util.GumboSwitch;
import org.sireum.aadl.osate.util.GumboUtils;
import org.sireum.aadl.osate.util.SlangUtils;
import org.sireum.hamr.ir.Annex;
import org.sireum.hamr.ir.Annex$;
import org.sireum.hamr.ir.AnnexLib;
import org.sireum.hamr.ir.GclAccessExp$;
import org.sireum.hamr.ir.GclBinaryExp$;
import org.sireum.hamr.ir.GclBinaryOp;
import org.sireum.hamr.ir.GclCompute;
import org.sireum.hamr.ir.GclExp;
import org.sireum.hamr.ir.GclGuarantee;
import org.sireum.hamr.ir.GclIntegration;
import org.sireum.hamr.ir.GclInvariant;
import org.sireum.hamr.ir.GclInvariant$;
import org.sireum.hamr.ir.GclLiteralExp;
import org.sireum.hamr.ir.GclLiteralExp$;
import org.sireum.hamr.ir.GclLiteralType;
import org.sireum.hamr.ir.GclNameExp$;
import org.sireum.hamr.ir.GclStateVar;
import org.sireum.hamr.ir.GclSubclause$;
import org.sireum.hamr.ir.GclUnaryExp$;
import org.sireum.hamr.ir.GclUnaryOp;

public class GumboVisitor extends GumboSwitch<Boolean> implements AnnexVisitor {

	Visitor v;
	Classifier entryClassifier = null;

	private boolean TODO_HALT = true;

	GclLiteralExp dummy = GclLiteralExp$.MODULE$.apply(GclLiteralType.byName("String").get(), "dummy",
			SlangUtils.toNone());

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

			this.entryClassifier = c;

			addAllBaseTypes(c.eResource().getResourceSet());

			visit(bas.get(0));

			ret.add(Annex$.MODULE$.apply(ANNEX_TYPE, pop()));

			this.entryClassifier = null;
		}

		return ret;
	}

	private void addAllBaseTypes(ResourceSet rs) {
		URI u = URI.createURI("platform:/plugin/org.osate.contribution.sei/resources/packages/Base_Types.aadl");
		Resource r = rs.getResource(u, true);
		AadlPackage baseTypes = (AadlPackage) r.getContents().get(0);
		for (org.osate.aadl2.Classifier c : baseTypes.getOwnedPublicSection().getOwnedClassifiers()) {
			if (!c.getName().equals("Natural")) {
				v.processDataType((DataClassifier) c);
			}
		}
	}

	@Override
	public Boolean caseGumboSubclause(GumboSubclause object) {
		List<GclStateVar> _state = new ArrayList<>();
		if (object.getSpecs().getState() != null) {
			visit(object.getSpecs().getState());
		}

		List<GclInvariant> _invariants = new ArrayList<>();
		if (object.getSpecs().getInvariants() != null) {
			for (InvSpec s : object.getSpecs().getInvariants().getSpecs()) {
				String displayName = s.getDisplayName();
				visit(s.getExpr());
				_invariants.add(GclInvariant$.MODULE$.apply(displayName, pop()));
			}
		}

		Option<GclIntegration> _integration = SlangUtils.toNone();
		if (object.getSpecs().getIntegration() != null) {
			visit(object.getSpecs().getIntegration());
		}

		List<GclGuarantee> _initializes = new ArrayList<>();
		if (object.getSpecs().getInitialize() != null) {
			visit(object.getSpecs().getInitialize());
		}

		Option<GclCompute> _compute = SlangUtils.toNone();
		if (object.getSpecs().getCompute() != null) {
			visit(object.getSpecs());
		}

		push(GclSubclause$.MODULE$.apply(VisitorUtil.toISZ(_state), VisitorUtil.toISZ(_invariants),
				VisitorUtil.toISZ(_initializes), _integration, _compute));

		return false;
	}


	@Override
	public Boolean caseBinaryExpr(BinaryExpr object) {
		visit(object.getLeft());
		GclExp lhs = pop();

		visit(object.getRight());
		GclExp rhs = pop();

		GclBinaryOp.Type op = GumboUtils.toBinaryOp(object.getOp());

		push(GclBinaryExp$.MODULE$.apply(op, lhs, rhs, SlangUtils.toNone()));

		return false;
	}

	@Override
	public Boolean caseUnaryExpr(UnaryExpr object) {
		visit(object.getExpr());
		GclExp exp = pop();

		GclUnaryOp.Type op = GumboUtils.toUnaryOp(object.getOp());

		push(GclUnaryExp$.MODULE$.apply(op, exp, SlangUtils.toNone()));

		return false;
	}

	@Override
	public Boolean caseDataRefExpr(DataRefExpr object) {
		EObject o = object.getPortOrSubcomponentOrStateVar();

		GclExp exp = null;

		if (o instanceof DataSubcomponent) {
			DataSubcomponent ds = (DataSubcomponent) o;

			ComponentType ct = ds.getComponentType();
			Element owner = ds.getOwner();

			if (owner != this.entryClassifier) {
				todo(object, "Probably not dealing with a data component");

				exp = dummy;
			} else {
				exp = GclNameExp$.MODULE$.apply(GumboUtils.toName(ds.getName()), SlangUtils.toNone());
			}
		}

		if (object.getRef() != null) {
			OtherDataRef ref = object.getRef();
			NamedElement n = ref.getNamedElement();

			if (n instanceof DataSubcomponent) {
				DataSubcomponent ds = (DataSubcomponent) n;
				String attName = ds.getName();

				push(GclAccessExp$.MODULE$.apply(exp, attName, SlangUtils.toNone()));
			} else {
				todo(n, "Not yet");

				push(dummy);
			}
		} else {
			push(exp);
		}

		return false;
	}

	@Override
	public Boolean caseIntLit(IntLit object) {

		GclLiteralType.Type typ = GclLiteralType.byName("Integer").get();

		push(GclLiteralExp$.MODULE$.apply(typ, object.getValue(), SlangUtils.toNone()));

		return false;
	}

	@Override
	public Boolean caseRealLitExpr(RealLitExpr object) {

		GclLiteralType.Type typ = GclLiteralType.byName("Real").get();

		push(GclLiteralExp$.MODULE$.apply(typ, object.getVal(), SlangUtils.toNone()));

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

	private void todo(Object o, String msg) {
		RuntimeException e = new RuntimeException(msg + ": " + o);

		if (TODO_HALT) {
			throw e;
		} else {
			StackTraceElement ste = e.getStackTrace()[1];

			System.err.println(e.getMessage() + ": " + ste);
		}
	}

	@Override
	public List<AnnexLib> buildAnnexLibraries(Element arg0) {
		return VisitorUtil.iList();
	}
}
