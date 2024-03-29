package org.sireum.aadl.osate.architecture;

import java.util.List;

import org.osate.aadl2.Classifier;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.hamr.ir.Annex;
import org.sireum.hamr.ir.AnnexLib;
import org.sireum.message.Reporter;

public interface AnnexVisitor {

	// currently used to provide feedback to users
	// e.g. ".. visitorX failed .."
	public String getVisitorName();

	// currently used to provide feedback to users
	// e.g. ".. visitorX that handles annexes (a1, a2) failed .."
	public List<String> getHandledAnnexes();

	public List<Annex> visit(Classifier c, List<String> path, Reporter reporter);

	// Annexes are attached to Classifiers so it's expected that this
	// will end up calling/returning visit(ci.getComponentClassifier, path)
	public List<Annex> visit(ComponentInstance ci, List<String> path, Reporter reporter);

	public List<AnnexLib> buildAnnexLibraries(Element root, Reporter reporter);
}
