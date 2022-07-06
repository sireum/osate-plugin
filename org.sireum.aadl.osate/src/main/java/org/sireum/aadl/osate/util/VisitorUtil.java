package org.sireum.aadl.osate.util;

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
import org.osate.aadl2.AnnexLibrary;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.annexsupport.AnnexRegistry;
import org.osate.annexsupport.AnnexTextPositionResolverRegistry;
import org.sireum.IS;
import org.sireum.IS$;
import org.sireum.Option;
import org.sireum.message.Position;
import org.sireum.message.Reporter;

public class VisitorUtil {

	static AnnexTextPositionResolverRegistry textpositionresolverregistry = (AnnexTextPositionResolverRegistry) AnnexRegistry
			.getRegistry(AnnexRegistry.ANNEX_TEXTPOSITIONRESOLVER_EXT_ID);

	public static <T> List<T> isz2IList(org.sireum.IS<org.sireum.Z, T> isz) {
		return Collections.unmodifiableList(scala.jdk.javaapi.CollectionConverters.asJava(isz.elements()));
	}

	@SafeVarargs
	public static <T> IS<org.sireum.Z, T> toISZ(T... l) {
		return toISZ(java.util.Arrays.asList(l));
	}

	public static <T> IS<org.sireum.Z, T> toISZ(List<T> l) {
		scala.collection.Seq<T> seq = scala.jdk.javaapi.CollectionConverters.asScala(l);

		// Eclipse JDT hack. Eclipse reports seq.toSeq() is ambiguous so help compiler
		// out by casting to the interface type we want
		// -- see https://bugs.eclipse.org/bugs/show_bug.cgi?id=468276#c32
		scala.collection.immutable.Seq<T> iseq = ((scala.collection.IterableOnceOps<T, ?, ?>) seq).toSeq();

		return IS$.MODULE$.apply(iseq, org.sireum.Z$.MODULE$);
	}

	public static <T> List<T> toIList(List<T> l) {
		return Collections.unmodifiableList(l);
	}

	public static <T> List<T> toIList(T e) {
		final List<T> ret = new ArrayList<>();
		ret.add(e);
		return toIList(ret);
	}

	@SafeVarargs
	public static <T> List<T> toIList(T... l) {
		return toIList(java.util.Arrays.asList(l));
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

	private static String getResourcePath(EObject component) {
		Resource res = component.eResource();
		URI uri = res.getURI();
		IPath path = Util.toIFile(uri).getFullPath();
		return path.toPortableString();
	}

	public static String getUriFragment(EObject eobj) {
		return EcoreUtil.getURI(eobj).toString();
	}

	public static Option<Position> buildPositionOpt(EObject object) {
		Position p = VisitorUtil.buildPosition(object);
		return p == null ? SlangUtil.toNone() : SlangUtil.toSome(p);
	}

	public static Position buildPosition(EObject elem) {

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

			if (rnode.getSemanticElement() != null && //
					(rnode.getSemanticElement() instanceof AnnexLibrary
							|| rnode.getSemanticElement() instanceof AnnexSubclause)) {
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

	public static void reportError(String msg, String msgKind, Reporter reporter) {
		reportError(false, null, msg, msgKind, reporter);
	}

	public static void reportError(boolean cond, String msg, String msgKind, Reporter reporter) {
		reportError(cond, null, msg, msgKind, reporter);
	}

	/**
	 *
	 * @param cond report error if cond is false
	 * @param o used to get AADL position info if non-null
	 * @param msg the error message
	 * @param msgKind used for filtering (e.g. use the annex's name)
	 * @param reporter
	 */
	public static void reportError(boolean cond, EObject o, String msg, String msgKind, Reporter reporter) {
		if (!cond) {
			reporter.error(o == null ? SlangUtil.toNone() : buildPositionOpt(o), msgKind, msg);
		}
	}

	/**
	 * @return removes property associations in {@code properties} that have the same name, keeping
	 * only the final occurrence
	 */
	public static List<PropertyAssociation> removeShadowedProperties(List<PropertyAssociation> properties) {
		List<PropertyAssociation> ret = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for (int i = properties.size() - 1; i >= 0; i--) {
			PropertyAssociation p = properties.get(i);
			String name = p.getProperty().getName();
			if (!names.contains(name)) {
				ret.add(p);
				names.add(name);
			}
		}
		return ret;
	}

	public static class Pair<T1, T2> {
		public final T1 t1;
		public final T2 t2;

		public Pair(T1 t1, T2 t2) {
			this.t1 = t1;
			this.t2 = t2;
		}
	}
}
