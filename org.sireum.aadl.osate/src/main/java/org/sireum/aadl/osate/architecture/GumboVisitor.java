package org.sireum.aadl.osate.architecture;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.aadl.gumbo.gumbo.FeatureElement;
import org.sireum.aadl.gumbo.gumbo.Flow;
import org.sireum.aadl.gumbo.gumbo.GumboSubclause;
import org.sireum.aadl.gumbo.gumbo.util.GumboSwitch;
import org.sireum.hamr.ir.Annex;

public class GumboVisitor extends GumboSwitch<Boolean> {

	Visitor v;

	private final String ANNEX_TYPE = "gumbo";
	private List<String> path = null;

	public GumboVisitor(Visitor v) {

		this.v = v;
	}

	public List<Annex> visit(ComponentInstance ci, List<String> currentPath) {
		List<Annex> ret = new ArrayList<>();
		if (ci.getClassifier() != null) {
			List<GumboSubclause> bas = EcoreUtil2.eAllOfType(ci.getClassifier(), GumboSubclause.class);
			assert bas.size() <= 1 : "Expecting at most one Gumbo clause for " + ci.getFullName() + " but found "
					+ bas.size();

			if (bas.size() == 1) {
				this.path = path;

				visit(bas.get(0));

				// ret.add(Annex$.MODULE$.apply(ANNEX_TYPE, pop()));
			}
		}
		return ret;
	}

	@Override
	public Boolean caseFlow(Flow object) {

		for (FeatureElement fe : object.getSrcPorts()) {
			NamedElement ne = fe.getFeature();
			System.out.println(ne);
			System.out.println();
		}
		for (FeatureElement fe : object.getDstPorts()) {
			NamedElement ne = fe.getFeature();
			System.out.println(ne);
			System.out.println();
		}
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
		assert (result != null);
		T ret = (T) result;
		result = null;
		return ret;
	}
}
