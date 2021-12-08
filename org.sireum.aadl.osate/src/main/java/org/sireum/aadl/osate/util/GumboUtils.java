package org.sireum.aadl.osate.util;

import org.sireum.hamr.ir.BinaryOp;
import org.sireum.hamr.ir.UnaryOp;

public class GumboUtils {

	public static BinaryOp.Type toBinaryOp(String op) {

		if (op.equalsIgnoreCase("=>") || op.equalsIgnoreCase("implies")) {
			return BinaryOp.byName("Implies").get();
		} //
		else if (op.equalsIgnoreCase("<=>")) {
			return BinaryOp.byName("Equiv").get();
		} //
		else if (op.equalsIgnoreCase("or")) {
			return BinaryOp.byName("Or").get();
		} //
		else if (op.equalsIgnoreCase("orelse")) {
			return BinaryOp.byName("OrElse").get();
		} //
		else if (op.equalsIgnoreCase("and")) {
			return BinaryOp.byName("And").get();
		} //
		else if (op.equalsIgnoreCase("andthen")) {
			return BinaryOp.byName("AndThen").get();
		} //
		else if (op.equalsIgnoreCase("<")) {
			return BinaryOp.byName("Lt").get();
		} //
		else if (op.equalsIgnoreCase("<=")) {
			return BinaryOp.byName("Lte").get();
		} //
		else if (op.equalsIgnoreCase(">")) {
			return BinaryOp.byName("Gt").get();
		} //
		else if (op.equalsIgnoreCase(">=")) {
			return BinaryOp.byName("Gte").get();
		} //
		else if (op.equalsIgnoreCase("=")) {
			return BinaryOp.byName("Eq").get();
		} //
		else if (op.equalsIgnoreCase("<>")) {
			return BinaryOp.byName("Neq").get();
		} //
		else if (op.equalsIgnoreCase("+")) {
			return BinaryOp.byName("Plus").get();
		} //
		else if (op.equalsIgnoreCase("-")) {
			return BinaryOp.byName("Minus").get();
		} //
		else if (op.equalsIgnoreCase("*")) {
			return BinaryOp.byName("Mult").get();
		} //
		else if (op.equalsIgnoreCase("/")) {
			return BinaryOp.byName("Div").get();
		} //
		else if (op.equalsIgnoreCase("%")) {
			return BinaryOp.byName("Mod").get();
		} //
		else if (op.equalsIgnoreCase("^")) {
			return BinaryOp.byName("Exp").get();
		}

		throw new RuntimeException("Binary operator '" + op + "' not supported");
	}

	public static UnaryOp.Type toUnaryOp(String op) {
		if (op.equalsIgnoreCase("-")) {
			return UnaryOp.byName("Neg").get();
		} //
		else if (op.equalsIgnoreCase("not")) {
			return UnaryOp.byName("Not").get();
		}

		throw new RuntimeException("Unary operator '" + op + "' not supported");
	}
}
