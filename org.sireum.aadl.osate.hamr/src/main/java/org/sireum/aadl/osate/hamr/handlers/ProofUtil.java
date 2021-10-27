package org.sireum.aadl.osate.hamr.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import org.sireum.aadl.osate.hamr.PreferenceValues;

public class ProofUtil {

	protected static File lastSMT2Proof = null;

	public static int checkProof() {
		return checkProof(getLastGeneratedSMT2Proof());
	}

	public static int checkProof(File f) {
		return checkProof(f, null);
	}

	public static int checkProof(PrintStream out) {
		return checkProof(getLastGeneratedSMT2Proof(), out);
	}

	/**
	 * @param f either an smt2 file or,
	 *          HAMR's org.sireum.aadl.hamr.prefs setting file or,
	 *          the project's .settings directory or,
	 *          the root AADL project directory
	 */
	public static int checkProof(File f, PrintStream out) {

		if (f == null || !f.exists() || !f.canRead()) {
			outPrintln("Cannot read from file: " + f, out);
			return 1;
		}

		File smt2Proof = null;
		if (f.getName().endsWith(".smt2")) {
			smt2Proof = f;
		} else {
			String sep = File.separator;
			File propFile = null;
			String propName = "org.sireum.aadl.hamr.prefs";
			if (f.isDirectory()) {
				if(f.getName().equals(".settings") && new File(f, propName).exists()) {
					propFile = new File(f, propName);
				} else if (new File(f, ".settings" + sep + propName).exists()) {
					propFile = new File(f, ".settings" + sep + propName);
				}
			} else if (f.getName().equals(propName)) {
				propFile = f;
			}

			if (propFile != null) {
				try (InputStream i = new FileInputStream(propFile)) {
					Properties p = new Properties();
					p.load(i);

					if (p.getProperty("camkes.output.directory") != null) {
						smt2Proof = new File(new File(p.getProperty("camkes.output.directory")),
								"proof" + sep + "smt2_case.smt2");
					} else if (p.getProperty("slang.output.directory") != null) {
						smt2Proof = new File(new File(p.getProperty("slang.output.directory")),
								"src" + sep + "c" + sep + "camkes" + sep + "proof" + sep + "smt2_case.smt2");
					} else {
						outPrintln("Couldn't determine location of SMT2 proof file from: " + propFile.getAbsolutePath(),
								out);
						return 1;
					}
				} catch (Exception e) {
					outPrintln(e.getMessage(), out);
					return 1;
				}
			} else {
				outPrintln("Pass in a smt2 file or HAMR's setting file", out);
				return 1;
			}
		}

		File smt2Solver = PreferenceValues.HAMR_SMT2_PATH.getValue();
		if (smt2Solver != null) {
			String[] solverOptions = PreferenceValues.HAMR_SMT2_OPTIONS.getValue().split(" ");
			int timeout = PreferenceValues.HAMR_SMT2_TIMEOUT.getValue();

			return checkProof(smt2Solver, Arrays.asList(solverOptions), smt2Proof, timeout, out);
		} else {
			outPrintln("Location of SMT2 solver not specified", out);
			return 1;
		}
	}

	public static int checkProof(File smt2Solver, List<String> solverOptions, File smt2Proof, int timeout,
			PrintStream out) {
		if (smt2Solver == null || !smt2Solver.exists() || !smt2Solver.canExecute()) {
			outPrintln("Error with passed in solver: " + smt2Solver, out);
			return 1;
		}
		if (smt2Proof == null || !smt2Proof.exists() || !smt2Proof.canRead()) {
			outPrintln("Cannot read from file: " + smt2Proof, out);
			return 1;
		}
		try {
			List<String> args = new ArrayList<>();
			args.add(smt2Solver.getAbsolutePath());
			args.addAll(solverOptions);
			args.add(smt2Proof.getAbsolutePath());

			ProcessBuilder pb = new ProcessBuilder(args.toArray(new String[args.size()]));
			pb.redirectErrorStream(true);

			outPrintln(out);
			outPrintln("Checking Information Flow Preservation Proof in file", out);
			outPrintln(smt2Proof.getAbsolutePath(), out);

			Process p = pb.start();

			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			StringJoiner sj = new StringJoiner(System.lineSeparator());
			reader.lines().iterator().forEachRemaining(sj::add);

			String result = sj.toString();

			p.waitFor(timeout, TimeUnit.MILLISECONDS);
			p.destroy();

			boolean isTimeout = p.exitValue() == 6 || p.exitValue() == -101 || p.exitValue() == -100;
			if (result.length() == 0 || p.exitValue() != 0 || isTimeout) {
				outPrintln(out);
				outPrintln("Error when running " + smt2Solver.getName() + " query:", out);
				outPrintln("File: " + smt2Proof.getAbsolutePath(), out);
				outPrintln("Exit Code: " + p.exitValue(), out);
				outPrintln("Timed Out: " + isTimeout, out);
				outPrintln(result, out);
				return p.exitValue();
			}

			String[] lines = result.split(System.lineSeparator());
			if (lines.length == 10) {
				boolean passing = true;

				passing &= checkResult("\"RefinementProof:", lines[0], lines[1], "sat", out);
				passing &= checkResult("\"AADLWellFormedness:", lines[2], lines[3], "unsat", out);
				passing &= checkResult("\"CAmkESWellFormedness:", lines[4], lines[5], "unsat", out);
				passing &= checkResult("\"ConnectionPreservation:", lines[6], lines[7], "unsat", out);
				passing &= checkResult("\"NoNewConnections:", lines[8], lines[9], "unsat", out);

				outPrintln(out);
				outPrintln("Information Flow Preservation Proof: " + (passing ? "Succeeded" : "Failed"), out);
				return passing ? 0 : 1;
			} else {
				outPrintln(out);
				outPrintln("Expected 10 lines to be returned by the smt2 solver, but received " + result, out);
				return 1;
			}

		} catch (Exception e) {
			e.printStackTrace(out);
			return 1;
		}
	}

	private static boolean checkResult(String expectedCategory, String resultCategory, String proofResult,
			String proofExpected, PrintStream out) {
		String category = expectedCategory.substring(1);
		boolean cresult = resultCategory.startsWith(expectedCategory);
		if (!cresult) {
			outPrintln(out);
			outPrintln(category + " Expected line to start with '" + expectedCategory + "' but received '"
					+ resultCategory + "'", out);
		}
		boolean presult = proofResult.equals(proofExpected);
		if (!presult) {
			outPrintln(out);
			outPrintln(category + " Expected '" + proofExpected + "' but received '" + proofResult + "'", out);
		}
		return cresult && presult;
	}

	public static File getLastGeneratedSMT2Proof() {
		// or perhaps get the file path from the eclipse store
		return lastSMT2Proof;
	}

	private static void outPrintln(PrintStream out) {
		outPrintln("", out);
	}

	private static void outPrintln(String s, PrintStream out) {
		if (out != null) {
			out.println(s);
		}
	}

}