package org.sireum.aadl.osate.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class ProofUtil {

	public static boolean checkResult(String expectedCategory, String resultCategory, String proofResult,
			String proofExpected,
			PrintStream out) {
		String category = expectedCategory.substring(1);
		boolean cresult = resultCategory.startsWith(expectedCategory);
		if (!cresult) {
			out.println(category + "Expected line to start with '" + expectedCategory + "' but received '"
					+ resultCategory + "'");
		}
		boolean presult = proofResult.equals(proofExpected);
		if (!presult) {
			out.println(category + "Expected '" + proofExpected + "' but received '" + proofResult + "'");
		}
		return cresult && presult;
	}

	public static int checkProof(File smt2Solver, List<String> solverOptions, File smt2Proof, PrintStream out) {
		try {
			List<String> args = new ArrayList<>();
			args.add(smt2Solver.getAbsolutePath());
			args.addAll(solverOptions);
			args.add(smt2Proof.getAbsolutePath());

			ProcessBuilder pb = new ProcessBuilder(args.toArray(new String[args.size()]));
			pb.redirectErrorStream(true);

			out.println("Checking Information Flow Preservation Proof ...");

			Process p = pb.start();

	        final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

	        StringJoiner sj = new StringJoiner(System.getProperty("line.separator"));
	        reader.lines().iterator().forEachRemaining(sj::add);

	        String result = sj.toString();

	        p.waitFor();
	        p.destroy();

			if (p.exitValue() == 0) {
				String[] lines = result.split(System.lineSeparator());
				if (lines.length == 10) {
					boolean passing = true;

					passing &= checkResult("\"RefinementProof:", lines[0], lines[1], "sat", out);
					passing &= checkResult("\"AADLWellFormedness:", lines[2], lines[3], "unsat", out);
					passing &= checkResult("\"CAmkESWellFormedness:", lines[4], lines[5], "unsat", out);
					passing &= checkResult("\"ConnectionPreservation:", lines[6], lines[7], "unsat", out);
					passing &= checkResult("\"NoNewConnections:", lines[8], lines[9], "unsat", out);

					out.println("Information Flow Preservation Proof: " + (passing ? "Succeeded" : "Failed"));
					return passing ? 0 : 1;
				} else {
					out.println("Expected 10 lines to be returned by the smt2 solver, but received " + result);
					return 1;
				}
			} else {
				out.println(result);
				return p.exitValue();
			}

		} catch (Exception e) {
			e.printStackTrace(out);
			return 1;
		}
	}
}