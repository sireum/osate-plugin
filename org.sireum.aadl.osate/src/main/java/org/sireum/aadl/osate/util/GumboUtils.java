package org.sireum.aadl.osate.util;

import org.sireum.hamr.ir.GclBinaryOp;
import org.sireum.hamr.ir.GclUnaryOp;

public class GumboUtils {

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
}
