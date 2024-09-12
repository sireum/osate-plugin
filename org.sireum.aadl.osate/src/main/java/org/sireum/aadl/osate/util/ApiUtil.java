package org.sireum.aadl.osate.util;

import java.io.PrintStream;

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

		// Paste the following into a java program if you want to ensure the known keys match.
		// To regenerate this, run '$SIREUM_HOME/hamr/codegen/build.cmd regen-cli'
		// If this does fail then the CLI arguments being constructed for codegen will need
		// to be updated (that could be delayed if only new options were added).
		
		scala.collection.Seq<org.sireum.String> seq = scala.jdk.javaapi.CollectionConverters
				.asScala(new java.util.ArrayList<org.sireum.String>());
		scala.collection.immutable.Seq<org.sireum.String> iseq = ((scala.collection.IterableOnceOps<org.sireum.String, ?, ?>) seq)
				.toSeq();
		org.sireum.IS<org.sireum.Z, org.sireum.String> knownKeys = org.sireum.IS$.MODULE$
				.apply(iseq, org.sireum.Z$.MODULE$)
				.$colon$plus(new org.sireum.String("msgpack"))
				.$colon$plus(new org.sireum.String("verbose"))
				.$colon$plus(new org.sireum.String("runtime-monitoring"))
				.$colon$plus(new org.sireum.String("platform"))
				.$colon$plus(new org.sireum.String("parseable-messages"))
				.$colon$plus(new org.sireum.String("slang-output-dir"))
				.$colon$plus(new org.sireum.String("package-name"))
				.$colon$plus(new org.sireum.String("no-proyek-ive"))
				.$colon$plus(new org.sireum.String("no-embed-art"))
				.$colon$plus(new org.sireum.String("devices-as-thread"))
				.$colon$plus(new org.sireum.String("sbt-mill"))
				.$colon$plus(new org.sireum.String("aux-code-dirs"))
				.$colon$plus(new org.sireum.String("output-c-dir"))
				.$colon$plus(new org.sireum.String("exclude-component-impl"))
				.$colon$plus(new org.sireum.String("bit-width"))
				.$colon$plus(new org.sireum.String("max-string-size"))
				.$colon$plus(new org.sireum.String("max-array-size"))
				.$colon$plus(new org.sireum.String("run-transpiler"))
				.$colon$plus(new org.sireum.String("camkes-output-dir"))
				.$colon$plus(new org.sireum.String("camkes-aux-code-dirs"))
				.$colon$plus(new org.sireum.String("workspace-root-dir"))
				.$colon$plus(new org.sireum.String("strict-aadl-mode"))
				.$colon$plus(new org.sireum.String("ros2-output-workspace-dir"))
				.$colon$plus(new org.sireum.String("ros2-dir"))
				.$colon$plus(new org.sireum.String("ros2-nodes-language"))
				.$colon$plus(new org.sireum.String("ros2-launch-language"))
				.$colon$plus(new org.sireum.String("experimental-options"));
		boolean sameKeys = org.sireum.hamr.codegen.KeyUtil.allLongKeys().equals(knownKeys);

		if (!sameKeys) {
			out.println(
					"Cannot run HAMR Codegen as the expected codegen options do not match the actual codegen options");
			out.println("");
			out.println("Run Phantom to update HAMR's OSATE plugin (\"$SIREUM_HOME/bin/sireum hamr phantom -u\"). ");
			out.println("If that does not resolve the issue then please report it.\n");
		}
		return sameKeys;
		
		// old way of doing this
		/*
		 * String msg = null;
		 * try {
		 * Class<?> clsHAMR = Class.forName("org.sireum.cli.HAMR");
		 *
		 * Class<?> clsAadl = Class.forName("org.sireum.hamr.ir.Aadl");
		 * Class<?> clsBool = boolean.class;
		 * Class<?> clsPlatform = Class.forName("org.sireum.Cli$SireumHamrCodegenHamrPlatform$Type");
		 * Class<?> clsOption = Class.forName("org.sireum.Option");
		 * Class<?> clsIS = Class.forName("org.sireum.IS");
		 * Class<?> clsZ = Class.forName("org.sireum.Z");
		 * Class<?> clsReporter = Class.forName("org.sireum.message.Reporter");
		 *
		 * Method m = clsHAMR.getMethod("codeGenR", //
		 * clsAadl, // model
		 * //
		 * clsBool, // verbose
		 * clsBool, // runtimeMonitoring
		 * clsPlatform, // platform
		 * clsOption, // slangOutputDir
		 * clsOption, // slangPackageName
		 * //
		 * clsBool, // noProyekIve
		 * clsBool, // noEmbedArt
		 * clsBool, // devicesAsThreads
		 * clsBool, // genSbtMill
		 * //
		 * clsIS, // slangAuxCodeDir
		 * clsOption, // slangOutputCDirectory
		 * clsBool, // excludeComponentImpl
		 * clsZ, // bitWidth
		 * clsZ, // maxStringSize
		 * clsZ, // maxArraySize
		 * clsBool, // runTranspiler
		 * //
		 * clsOption, // camkesOutputDirectory
		 * clsIS, // camkesAuxCodeDirs
		 * clsOption, // aadlRootDir
		 * //
		 * clsIS, // experimentalOptions
		 * //
		 * clsReporter // reporter
		 * );
		 *
		 * if (m.getReturnType() != clsZ) {
		 * throw new NoSuchMethodException("Expecting HAMR's cli to return " + clsZ.getName()
		 * + " but it's returning a " + m.getReturnType().getName());
		 * }
		 * return true;
		 * } catch (Exception e) {
		 * msg = e.getClass().getSimpleName() + ": " + e.getMessage();
		 * }
		 */
	}

}
