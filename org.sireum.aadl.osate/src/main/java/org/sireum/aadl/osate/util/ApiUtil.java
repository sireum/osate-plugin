package org.sireum.aadl.osate.util;

import java.io.PrintStream;
import java.lang.reflect.Method;

public class ApiUtil {

	/**
	 * Invoking HAMR codegen via Phantom CLI effectively calls Sireum from the
	 * command line whereas codegen's OSATE plugin calls org.sireum.cli.HAMR.codeGenR.
	 * This can cause an issue if the master branch version of
	 * org.sireum.cli.HAMR.codeGenR is modified but an updated codegen plugin is not
	 * also released.
	 * HAMR codegen regression tests use Phantom CLI so this method is now called
	 * from phantom (and codegen's plugin) to catch this issue.
	 * @return true if the sireum.jar being used has the expected HAMR CLI method.
	 */
	public static boolean hamrCliApiCompatible(PrintStream out) {

		String msg = null;
		try {
			Class<?> clsHAMR = Class.forName("org.sireum.cli.HAMR");

			Class<?> clsAadl = Class.forName("org.sireum.hamr.ir.Aadl");
			Class<?> clsBool = boolean.class;
			Class<?> clsPlatform = Class.forName("org.sireum.Cli$SireumHamrCodegenHamrPlatform$Type");
			Class<?> clsOption = Class.forName("org.sireum.Option");
			Class<?> clsIS = Class.forName("org.sireum.IS");
			Class<?> clsZ = Class.forName("org.sireum.Z");

			Method m = clsHAMR.getMethod("codeGenR", //
					clsAadl, // model
					//
					clsBool, // verbose
					clsPlatform, // platform
					clsOption, // slangOutputDir
					clsOption, // slangPackageName
					//
					clsBool, // noProyekIve
					clsBool, // noEmbedArt
					clsBool, // devicesAsThreads
					//
					clsIS, // slangAuxCodeDir
					clsOption, // slangOutputCDirectory
					clsBool, // excludeComponentImpl
					clsZ, // bitWidth
					clsZ, // maxStringSize
					clsZ, // maxArraySize
					clsBool, // runTranspiler
					//
					clsOption, // camkesOutputDirectory
					clsIS, // camkesAuxCodeDirs
					clsOption, // aadlRootDir
					//
					clsIS // experimentalOptions
			);

			Class<?> reporter = Class.forName("org.sireum.message.Reporter");
			if (m.getReturnType() != reporter) {
				throw new NoSuchMethodException("Expecting HAMR's cli to return a " + reporter.getName()
						+ " but it's returning a " + m.getReturnType().getName());
			}
			return true;
		} catch (Exception e) {
			msg = e.getClass().getSimpleName() + ": " + e.getMessage();
		}
		out.println("\nCannot run HAMR Codegen due to:");
		out.println("  " + msg);
		out.println("");
		out.println("Run Phantom to update HAMR's OSATE plugin (\"$SIREUM_HOME/bin/sireum hamr phantom -u\"). ");
		out.println("If that does not resolve the issue then please report it.\n");
		return false;
	}

}
