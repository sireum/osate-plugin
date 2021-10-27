package org.sireum.aadl.osate.hamr.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import org.sireum.aadl.osate.hamr.PreferenceValues;

public class ProofUtil {

	protected static File lastSMT2Proof = null;

	public static int checkProof() {
		return checkProof(getLastGeneratedSMT2Proof());
	}

	public static int checkProof(File smt2Proof) {
		return checkProof(smt2Proof, null);
	}

	public static int checkProof(PrintStream out) {
		return checkProof(getLastGeneratedSMT2Proof(), out);
	}

	public static int checkProof(File smt2Proof, PrintStream out) {

		if (smt2Proof == null || !smt2Proof.exists() || !smt2Proof.canRead()) {
			outPrintln("Cannot read from file: " + smt2Proof, out);
			return 1;
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