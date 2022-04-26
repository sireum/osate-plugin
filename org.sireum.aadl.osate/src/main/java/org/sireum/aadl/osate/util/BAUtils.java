package org.sireum.aadl.osate.util;

import org.eclipse.emf.common.util.Enumerator;
import org.osate.ba.aadlba.BehaviorElement;
import org.osate.ba.aadlba.BinaryAddingOperator;
import org.osate.ba.aadlba.BinaryNumericOperator;
import org.osate.ba.aadlba.LogicalOperator;
import org.osate.ba.aadlba.MultiplyingOperator;
import org.osate.ba.aadlba.RelationalOperator;
import org.osate.ba.aadlba.UnaryAddingOperator;
import org.osate.ba.aadlba.UnaryBooleanOperator;
import org.osate.ba.aadlba.UnaryNumericOperator;
import org.osate.ba.utils.AadlBaLocationReference;
import org.sireum.hamr.ir.BTSBinaryOp;
import org.sireum.hamr.ir.BTSExp;
import org.sireum.hamr.ir.BTSUnaryExp;
import org.sireum.hamr.ir.BTSUnaryExp$;
import org.sireum.hamr.ir.BTSUnaryOp;
import org.sireum.message.Position;

public class BAUtils {

	public static org.sireum.Option<Position> buildPosInfo(BehaviorElement elem) {
		final org.sireum.hamr.ir.AadlASTFactory factory = new org.sireum.hamr.ir.AadlASTFactory();

		AadlBaLocationReference abr = elem.getAadlBaLocationReference();

		return SlangUtils.toSome(factory.flatPos(abr.getFilename(), //
				abr.getLine(), //
				abr.getColumn(), //

				abr.getLine(), // FIXME
				abr.getColumn(), // FIXME

				abr.getOffset(), //
				abr.getLength() //
		));
	}

	public static BTSUnaryExp convertToUnaryExp(BTSExp btsExp, Enumerator unaryOp) {
		return BTSUnaryExp$.MODULE$.apply(toUnaryOp(unaryOp), btsExp, btsExp.pos());
	}

	public static boolean isNoneEnumerator(Enumerator op) {
		return op.getName().equals("None");
	}

	public static BTSBinaryOp.Type toBinaryOp(Enumerator binaryOp) {
		assert (binaryOp instanceof BinaryAddingOperator || binaryOp instanceof BinaryNumericOperator
				|| binaryOp instanceof LogicalOperator || binaryOp instanceof MultiplyingOperator
				|| binaryOp instanceof RelationalOperator);

		return toBinaryOp(binaryOp.getLiteral());
	}

	public static BTSBinaryOp.Type toBinaryOp(String binaryOp) {
		// BinaryAddingOperator
		if (binaryOp.equalsIgnoreCase("+")) {
			return BTSBinaryOp.byName("PLUS").get();
		} //
		else if (binaryOp.equalsIgnoreCase("-")) {
			return BTSBinaryOp.byName("MINUS").get();
		}

		// BinaryNumericOperator
		else if (binaryOp.equalsIgnoreCase("**")) {
			return BTSBinaryOp.byName("EXP").get();
		} //

		// LogicalOperator
		else if (binaryOp.equalsIgnoreCase("and")) {
			return BTSBinaryOp.byName("AND").get();
		} //
		else if (binaryOp.equalsIgnoreCase("or")) {
			return BTSBinaryOp.byName("OR").get();
		} //
		else if (binaryOp.equalsIgnoreCase("xor")) {
			return BTSBinaryOp.byName("XOR").get();
		}

		// MultiplyingOperator
		else if (binaryOp.equalsIgnoreCase("*")) {
			return BTSBinaryOp.byName("MULT").get();
		} //
		else if (binaryOp.equalsIgnoreCase("/")) {
			return BTSBinaryOp.byName("DIV").get();
		} //
		else if (binaryOp.equalsIgnoreCase("mod")) {
			return BTSBinaryOp.byName("MOD").get();
		} //
		else if (binaryOp.equalsIgnoreCase("rem")) {
			return BTSBinaryOp.byName("REM").get();
		} //

		// RelationalOperator
		else if (binaryOp.equalsIgnoreCase("=")) {
			return BTSBinaryOp.byName("EQ").get();
		} //
		else if (binaryOp.equalsIgnoreCase("!=")) {
			return BTSBinaryOp.byName("NEQ").get();
		} //
		else if (binaryOp.equalsIgnoreCase("<")) {
			return BTSBinaryOp.byName("LT").get();
		} //
		else if (binaryOp.equalsIgnoreCase("<=")) {
			return BTSBinaryOp.byName("LTE").get();
		} //
		else if (binaryOp.equalsIgnoreCase(">")) {
			return BTSBinaryOp.byName("GT").get();
		} //
		else if (binaryOp.equalsIgnoreCase(">=")) {
			return BTSBinaryOp.byName("GTE").get();
		}
		// BRL literals for operator in Expression
		else if (binaryOp.equalsIgnoreCase("implies")) {
			return BTSBinaryOp.byName("IMPLIES").get();
		} //
		else if (binaryOp.equalsIgnoreCase("iff")) {
			return BTSBinaryOp.byName("IFF").get();
		} //

		// TODO: does BA have short circuit versions?
		/*
		 * else if (binaryOp.equalsIgnoreCase("then")) {
		 * return BTSBinaryOp.byName("ANDTHEN").get();
		 * } //
		 * else if (binaryOp.equalsIgnoreCase("else")) {
		 * return BTSBinaryOp.byName("ORELSE").get();
		 * }
		 */

		throw new RuntimeException("Binary operator '" + binaryOp + "' not supported");
	}

	public static BTSUnaryOp.Type toUnaryOp(Enumerator unaryOp) {
		assert (unaryOp instanceof UnaryAddingOperator || unaryOp instanceof UnaryBooleanOperator
				|| unaryOp instanceof UnaryNumericOperator);

		return toUnaryOp(unaryOp.getLiteral());
	}

	public static BTSUnaryOp.Type toUnaryOp(String unaryOp) {
		// UnaryAddingOperator
		if (unaryOp.equalsIgnoreCase("-")) {
			return BTSUnaryOp.byName("NEG").get();
		} //
		else if (unaryOp.equalsIgnoreCase("+")) {
			throw new RuntimeException("what is a unary '+' ba exp?");
		}

		// UnaryBooleanOperator
		else if (unaryOp.equalsIgnoreCase("!")) {
			return BTSUnaryOp.byName("NOT").get();
		} //

		// UnaryNumericOperator
		else if (unaryOp.equalsIgnoreCase("abs")) {
			return BTSUnaryOp.byName("ABS").get();
		} //

		throw new RuntimeException("Unary operator '" + unaryOp + "' not supported");
	}

}