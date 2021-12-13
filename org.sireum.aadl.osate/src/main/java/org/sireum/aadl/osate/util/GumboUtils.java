package org.sireum.aadl.osate.util;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.Port;
import org.sireum.Option;
import org.sireum.aadl.osate.architecture.VisitorUtil;
import org.sireum.hamr.ir.AadlASTFactory;
import org.sireum.hamr.ir.GclBinaryOp;
import org.sireum.hamr.ir.GclUnaryOp;
import org.sireum.hamr.ir.Name;
import org.sireum.message.Position;

public class GumboUtils {

	protected final static AadlASTFactory factory = new AadlASTFactory();

	public static GclBinaryOp.Type toBinaryOp(String op) {

		if (op.equalsIgnoreCase("=>") || op.equalsIgnoreCase("implies")) {
			return GclBinaryOp.byName("Implies").get();
		} //
		else if (op.equalsIgnoreCase("<=>")) {
			return GclBinaryOp.byName("Equiv").get();
		} //
		else if (op.equalsIgnoreCase("or")) {
			return GclBinaryOp.byName("Or").get();
		} //
		else if (op.equalsIgnoreCase("orelse")) {
			return GclBinaryOp.byName("OrElse").get();
		} //
		else if (op.equalsIgnoreCase("and")) {
			return GclBinaryOp.byName("And").get();
		} //
		else if (op.equalsIgnoreCase("andthen")) {
			return GclBinaryOp.byName("AndThen").get();
		} //
		else if (op.equalsIgnoreCase("<")) {
			return GclBinaryOp.byName("Lt").get();
		} //
		else if (op.equalsIgnoreCase("<=")) {
			return GclBinaryOp.byName("Lte").get();
		} //
		else if (op.equalsIgnoreCase(">")) {
			return GclBinaryOp.byName("Gt").get();
		} //
		else if (op.equalsIgnoreCase(">=")) {
			return GclBinaryOp.byName("Gte").get();
		} //
		else if (op.equalsIgnoreCase("=")) {
			return GclBinaryOp.byName("Eq").get();
		} //
		else if (op.equalsIgnoreCase("<>")) {
			return GclBinaryOp.byName("Neq").get();
		} //
		else if (op.equalsIgnoreCase("+")) {
			return GclBinaryOp.byName("Plus").get();
		} //
		else if (op.equalsIgnoreCase("-")) {
			return GclBinaryOp.byName("Minus").get();
		} //
		else if (op.equalsIgnoreCase("*")) {
			return GclBinaryOp.byName("Mult").get();
		} //
		else if (op.equalsIgnoreCase("/")) {
			return GclBinaryOp.byName("Div").get();
		} //
		else if (op.equalsIgnoreCase("%")) {
			return GclBinaryOp.byName("Mod").get();
		} //
		else if (op.equalsIgnoreCase("^")) {
			return GclBinaryOp.byName("Exp").get();
		}

		throw new RuntimeException("Binary operator '" + op + "' not supported");
	}

	public static GclUnaryOp.Type toUnaryOp(String op) {
		if (op.equalsIgnoreCase("-")) {
			return GclUnaryOp.byName("Neg").get();
		} //
		else if (op.equalsIgnoreCase("not")) {
			return GclUnaryOp.byName("Not").get();
		}

		throw new RuntimeException("Unary operator '" + op + "' not supported");
	}

	public static Name toName(List<String> _path) {
		return factory.name(_path, null);
	}

	public static Name toName(String n, List<String> _path) {
		return toName(VisitorUtil.add(_path, n));
	}

	public static Name toName(String n) {
		return toName(n, VisitorUtil.iList());
	}

	public static Name toName(Port p) {
		return toName(p.getName());
	}

	public static Name emptyName() {
		return toName(VisitorUtil.iList());
	}

	public static Option<Position> buildPosInfo(EObject object) {
		Position p = VisitorUtil.buildPosInfo(object);
		return p == null ? SlangUtils.toNone() : SlangUtils.toSome(p);
	}
}
