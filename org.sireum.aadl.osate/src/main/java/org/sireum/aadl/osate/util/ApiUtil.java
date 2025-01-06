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
		// To regenerate this, run '$SIREUM_HOME/hamr/codegen/build.cmd regen-cli'.
		// If this does fail then the CLI arguments being constructed for codegen will need
		// to be updated (that could be delayed if only new options were added).

		scala.collection.Seq<org.sireum.String> seq = scala.jdk.javaapi.CollectionConverters
				.asScala(new java.util.ArrayList<org.sireum.String>());
		scala.collection.immutable.Seq<org.sireum.String> iseq = ((scala.collection.IterableOnceOps<org.sireum.String, ?, ?>) seq)
				.toSeq();
		org.sireum.IS<org.sireum.Z, org.sireum.String> knownKeys = org.sireum.IS$.MODULE$
				.apply(iseq, org.sireum.Z$.MODULE$)
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.msgpack()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.verbose()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.runtimeMonitoring()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.platform()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.outputDir()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.parseableMessages()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Slang_slangOutputDir()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Slang_packageName()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Slang_noProyekIve()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Slang_noEmbedArt()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Slang_devicesAsThreads()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Slang_genSbtMill()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Transpiler_slangAuxCodeDirs()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Transpiler_slangOutputCDir()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Transpiler_excludeComponentImpl()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Transpiler_bitWidth()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Transpiler_maxStringSize()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Transpiler_maxArraySize()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Transpiler_runTranspiler()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.CAmkES_Microkit_sel4OutputDir()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.CAmkES_Microkit_sel4AuxCodeDirs()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.CAmkES_Microkit_workspaceRootDir()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.ROS2_strictAadlMode()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.ROS2_ros2OutputWorkspaceDir()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.ROS2_ros2Dir()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.ROS2_ros2NodesLanguage()))
				.$colon$plus(new org.sireum.String(org.sireum.hamr.codegen.LongKeys.ROS2_ros2LaunchLanguage()))
				.$colon$plus(
						new org.sireum.String(org.sireum.hamr.codegen.LongKeys.Experimental_experimentalOptions()));

		boolean sameKeys = org.sireum.hamr.codegen.LongKeys.sameKeys(knownKeys);

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
