package org.sireum.aadl.osate.util;

import java.io.PrintStream;
import java.util.function.IntSupplier;

import org.eclipse.ui.console.MessageConsole;
import org.osate.aadl2.instance.SystemInstance;
import org.sireum.IS;
import org.sireum.U8;
import org.sireum.Z;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.ir.JSON;
import org.sireum.aadl.ir.MsgPack;
import org.sireum.aadl.osate.architecture.JavaVisitor;

import scala.Console;
import scala.Function0;
import scala.runtime.BoxedUnit;

public class Util {

	public enum SerializerType {
		JSON, JSON_COMPACT, MSG_PACK
	}

	public static String serialize(Aadl model, SerializerType t) {
		switch (t) {
		case JSON:
			return JSON.fromAadl(model, false);
		case JSON_COMPACT:
			return JSON.fromAadl(model, true);
		case MSG_PACK:
			IS<Z, U8> x = MsgPack.fromAadl(model, true);
			String ret = org.sireum.conversions.String.toBase64(x).toString();
			return ret;
		default:
			return null;
		}
	}

	public static String getAir(SystemInstance si) {
		Aadl ir = (new JavaVisitor()).convert(si,  true).get();
		return serialize(ir, SerializerType.JSON_COMPACT);
	}

	public static int callWrapper(String toolName, MessageConsole ms, IntSupplier f) {
		int[] ret = {-1};

		PrintStream out = new PrintStream(ms.newMessageStream());
		PrintStream outOld = System.out;
		PrintStream errOld = System.err;

		System.setOut(out);
		System.setErr(out);

		Console.withOut(System.out, (Function0<Object>) () -> {
            Console.withErr(System.err, (Function0<Object>) ()  -> {

            	try {
            		ret[0] = f.getAsInt();
            	} catch (Throwable t) {
            		System.out.println("Exception rasied when invoking " + toolName);
            		t.printStackTrace(out);
            	} finally {
            		out.flush();
            		try { if(out != null) {
						out.close();
					} }
            		catch (Throwable t) { t.printStackTrace(); }
            	}

            	return BoxedUnit.UNIT;
            });
            return BoxedUnit.UNIT;
        });

		System.setOut(outOld);
		System.setErr(errOld);

		return ret[0];
	}
}
