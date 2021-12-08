package org.sireum.aadl.osate.architecture;

import java.util.List;

import org.osate.aadl2.Classifier;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.ComponentInstance;
import org.sireum.hamr.ir.Annex;
import org.sireum.hamr.ir.AnnexLib;

public interface AnnexVisitor {

	public List<Annex> visit(Classifier c, List<String> path);

	// Annexes are attache dot Classifiers so it's expected that this
	// will end up calling/returning visit(ci.getComponentClassifier, path)
	public List<Annex> visit(ComponentInstance ci, List<String> path);

	public List<AnnexLib> buildAnnexLibraries(Element root);
}
