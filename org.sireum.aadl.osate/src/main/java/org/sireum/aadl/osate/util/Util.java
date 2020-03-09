package org.sireum.aadl.osate.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.IntSupplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.ui.console.MessageConsole;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.IS;
import org.sireum.U8;
import org.sireum.Z;
import org.sireum.aadl.osate.architecture.Visitor;
import org.sireum.hamr.ir.Aadl;
import org.sireum.hamr.ir.JSON;
import org.sireum.hamr.ir.MsgPack;

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

	public static Aadl getAir(ComponentInstance root) {
		return getAir(root, true);
	}

	public static Aadl getAir(ComponentInstance root, boolean includeDataComponents) {
		return getAir(root, includeDataComponents, System.out);
	}

	public static Aadl getAir(ComponentInstance root, boolean includeDataComponents, MessageConsole console) {
		try (OutputStream out = console.newOutputStream()) {
			return getAir(root, includeDataComponents, out);
		} catch (Throwable t) {
			return null;
		}
	}

	public static Aadl getAir(ComponentInstance root, boolean includeDataComponents, OutputStream out) {
		try {
			return new Visitor().convert(root, includeDataComponents).get();
		} catch (Throwable t) {
			PrintStream p = new PrintStream(out);
			p.println("Error encountered while generating AIR");
			t.printStackTrace(p);
			p.close();
			return null;
		}
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
					System.err.println("Exception raised when invoking " + toolName);
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

	public static IFile toIFile(URI resourceURI) {
		/*
		 * Ideally we'd just call OsateResourceUtil.toIFile however that is not
		 * available in OSATE 2.4.x (which the CASE FM-IDE is based on). Workaround
		 * is to just replicate the current behavior of that method, refer to
		 * <a href=
		 * "https://github.com/osate/osate2/blob/bed18dd95fe3f3bf54d657911cd5e5da1ff2718b/core/org.osate.aadl2.modelsupport/src/org/osate/aadl2/modelsupport/resources/OsateResourceUtil.java#L62"
		 * >this</a>
		 */

		// return OsateResourceUtil.toIFile(resourceURI);

		if (resourceURI.isPlatform()) {
			return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(resourceURI.toPlatformString(true)));
		} else {
			return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(resourceURI.toFileString()));
		}
	}
}
