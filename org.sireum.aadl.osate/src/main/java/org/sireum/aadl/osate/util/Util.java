package org.sireum.aadl.osate.util;

import java.io.File;
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
import org.sireum.SireumApi;
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

	public static boolean emitSireumVersion(MessageConsole ms) {
		PrintStream out = new PrintStream(ms.newMessageStream());
		boolean ret = emitSireumVersion(out);
		out.close();
		return ret;
	}

	public static boolean emitSireumVersion(PrintStream out) {
		String propName = "org.sireum.home";
		String propValue = System.getProperty(propName);
		if (propValue != null) {
			File sireum_jar = new File(propValue, "bin/sireum.jar");
			if (!sireum_jar.exists()) {
				out.print("sireum.jar not found. Expecting it to be at: " + sireum_jar.getAbsolutePath() //
						+ "\n" //
						+ "\n" //
						+ "Ensure that the '" + propName + "' Java system property (current value is '" + propValue
						+ "') is set \n"
						+ "to the absolute path to your Sireum installation (sireum.jar should be in its 'bin' directory). \n"
						+ "You must restart OSATE in order for changes to osate.ini to take effect.\n");
				return false;
			} else {
				out.print(
						"Sireum Version: " + SireumApi.version() + " located at " + sireum_jar.getAbsolutePath()
								+ "\n");
				return true;
			}
		} else {
			out.print("Java system property '" + propName + "' not set. \n" //
					+ "\n" //
					+ "The prefered way of setting this is by installing the HAMR plugin via Phantom.  Run \n" //
					+ "the following from the command line for more information\n" //
					+ "\n" //
					+ "    $SIREUM_HOME/bin/sireum hamr phantom -h \n" //
					+ "\n" //
					+ "If you don't have Sireum installed then refer to https://github.com/sireum/kekinian#installing \n"
					+ "\n" //
					+ "\n" //
					+ "To set this property manually, in your osate.ini file locate the line containing '-vmargs' and \n"
					+ "add the following on a new line directly after that \n" + "\n" //
					+ "    -D" + propName + "=<path-to-sireum>\n" //
					+ "\n" //
					+ "replacing <path-to-sireum> with the absolute path to your Sireum installation \n"
					+ "(sireum.jar should be under its 'bin' directory).  Then restart OSATE. \n" //
					+ "\n" //
					+ "Alternatively, start OSATE using the vmargs option.  For example: \n" //
					+ "\n" //
					+ "    <path-to-osate>/osate -vmargs " + propName + "=<path-to-sireum>\n");
			return false;
		}
	}

}
