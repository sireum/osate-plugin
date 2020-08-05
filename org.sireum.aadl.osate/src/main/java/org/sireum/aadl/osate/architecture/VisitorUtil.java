package org.sireum.aadl.osate.architecture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.LineAndColumn;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.annexsupport.AnnexRegistry;
import org.osate.annexsupport.AnnexTextPositionResolverRegistry;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorModelLibrary;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorModelSubclause;
import org.sireum.aadl.osate.util.Util;
import org.sireum.message.Position;

public class VisitorUtil {

	static AnnexTextPositionResolverRegistry textpositionresolverregistry = (AnnexTextPositionResolverRegistry) AnnexRegistry
			.getRegistry(AnnexRegistry.ANNEX_TEXTPOSITIONRESOLVER_EXT_ID);

	public static <T> List<T> isz2IList(org.sireum.IS<org.sireum.Z, T> isz) {
		return Collections.unmodifiableList(scala.collection.JavaConverters.seqAsJavaList(isz.elements()));
	}

	public static <T> List<T> toIList(T e) {
		final List<T> ret = new ArrayList<>();
		ret.add(e);
		return Collections.unmodifiableList(ret);
	}

	public static <T> List<T> addAll(List<T> l, List<T> e) {
		final List<T> ret = new ArrayList<>(l);
		ret.addAll(e);
		return Collections.unmodifiableList(ret);
	}

	public static <T> List<T> add(List<T> l, T e) {
		final List<T> ret = new ArrayList<>(l);
		ret.add(e);
		return Collections.unmodifiableList(ret);
	}

	public static <T> Set<T> toISet(T e) {
		final Set<T> ret = new LinkedHashSet<>();
		ret.add(e);
		return Collections.unmodifiableSet(ret);
	}

	public static <T> Set<T> add(Set<T> s, T e) {
		final Set<T> ret = new LinkedHashSet<>(s);
		ret.add(e);
		return Collections.unmodifiableSet(ret);
	}

	public static <T> List<T> iList() {
		return Collections.emptyList();
	}

	public static <T> Set<T> iSet() {
		return Collections.emptySet();
	}

	private static String getResourcePath(NamedElement component) {
		Resource res = component.eResource();
		URI uri = res.getURI();
		IPath path = Util.toIFile(uri).getFullPath();
		return path.toPortableString();
	}

	public static String getUriFragment(EObject eobj) {
		System.out.println();
		Resource res = eobj.eResource();
		// return res.getURIFragment(eobj);
		return EcoreUtil.getURI(eobj).toString();
	}

	public static Position buildPosInfo(NamedElement elem) {

		final org.sireum.hamr.ir.AadlASTFactory factory = new org.sireum.hamr.ir.AadlASTFactory();
		EObject obj = elem;
		if (obj == null) {
			return null;
		}

		INode node = NodeModelUtils.findActualNodeFor(obj);

		if (node != null) {
			int startOffset = node.getOffset();
			int endOffset = node.getEndOffset();
			INode rnode = node.getRootNode();
			if (rnode.getSemanticElement() != null && (rnode.getSemanticElement() instanceof ErrorModelLibrary
					|| rnode.getSemanticElement() instanceof ErrorModelSubclause)) {

				AadlPackage pack = AadlUtil.getContainingPackage(node.getRootNode().getSemanticElement());
				INode ccNode = NodeModelUtils.findActualNodeFor(pack);
				rnode = ccNode.getRootNode();

			}

			LineAndColumn startLC = NodeModelUtils.getLineAndColumn(rnode, startOffset);
			LineAndColumn endLC = NodeModelUtils.getLineAndColumn(rnode, endOffset);

			return factory.flatPos(getResourcePath(elem), startLC.getLine(), startLC.getColumn(), endLC.getLine(),
					endLC.getColumn(), node.getOffset(), node.getLength());

		} else {
			EObject defaultannex = AadlUtil.getContainingDefaultAnnex(obj);
			if (defaultannex != null) {
				node = NodeModelUtils.findActualNodeFor(obj);
				if (node != null) {
					return factory.flatPos(getResourcePath(elem), node.getTotalStartLine(), node.getOffset(),
							node.getTotalEndLine(), node.getEndOffset(), node.getOffset(), node.getLength());
				}
			}
		}
		return null;
	}

}
