package org.sireum.aadl.osate.architecture;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.ComponentClassifier;
import org.osate.aadl2.Element;
import org.osate.aadl2.ModelUnit;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.Namespace;
import org.osate.aadl2.PackageSection;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.annexsupport.AnnexUtil;
import org.sireum.aadl.osate.securitymodel.secMF.SMFClassification;
import org.sireum.aadl.osate.securitymodel.secMF.SMFDeclassification;
import org.sireum.aadl.osate.securitymodel.secMF.SecMFPackage;
import org.sireum.aadl.osate.securitymodel.secMF.SecModelLibrary;
import org.sireum.aadl.osate.securitymodel.secMF.SecModelSubclause;
import org.sireum.aadl.osate.securitymodel.secMF.SmfTypeDef;
import org.sireum.hamr.ir.Annex;
import org.sireum.hamr.ir.AnnexLib;
import org.sireum.hamr.ir.Name;
import org.sireum.hamr.ir.SmfClassification;
import org.sireum.hamr.ir.SmfDeclass;
import org.sireum.hamr.ir.SmfType;

public class SmfVisitor {

	final org.sireum.hamr.ir.AadlASTFactory factory = new org.sireum.hamr.ir.AadlASTFactory();
	final Set<SecModelLibrary> smfLib = new HashSet();
	static final String SMF_ID = "smf";

	private Visitor coreVisitor = null;

	public SmfVisitor(Visitor visitor) {
		this.coreVisitor = visitor;
	}

	public Annex visitSmfComp(ComponentInstance root, List<String> path) {
		List<SecModelSubclause> temp = getSecModelSubclause(root);
		List<SmfClassification> classes = VisitorUtil.iList();
		List<SmfDeclass> declasses = VisitorUtil.iList();
		if (!temp.isEmpty()) {
			classes = temp.get(0).getClassification().stream().map(it -> visitClassification(it, path))
					.collect(Collectors.toList());
			declasses = temp.get(0).getDeclassification().stream().map(it -> visitDeclass(it, path))
					.collect(Collectors.toList());
		}
		return new Annex(SMF_ID, factory.smfClause(classes, declasses));

	}

	public List<AnnexLib> visitSmfLib(Element root) {
		HashSet<SecModelLibrary> libs = getAllPackages(root);
		if (libs.size() > 1) {
			java.lang.System.err.println("More than one security library defined");
			return VisitorUtil.iList();
		}

		return libs.stream().map(it -> visitSmfLib(it)).collect(Collectors.toList());
	}

	private AnnexLib visitSmfLib(SecModelLibrary secModelLibrary) {
		List<SmfType> types = secModelLibrary.getTypes().stream().map(it -> visitSMFTypeDef(it))
				.collect(Collectors.toList());

		return factory.smfLibrary(types);

	}

	private SmfType visitSMFTypeDef(SmfTypeDef typeDef) {
		Name typeName = factory.name(VisitorUtil.toIList(typeDef.getFullName()), VisitorUtil.buildPosInfo(typeDef));
		Name parentName = null;
		if (typeDef.getType() != null) {
			parentName = factory.name(VisitorUtil.toIList(typeDef.getType().getFullName()),
					VisitorUtil.buildPosInfo(typeDef.getType()));
		}
		return factory.smfType(typeName, parentName);
	}

	private HashSet<SecModelLibrary> getAllPackages(Element root) {
		HashSet<SecModelLibrary> secLibs = new HashSet<SecModelLibrary>();
		// HashSet<> packages = new HashSet(); no need of seen set as the circular imports are not allowed

		Namespace tns = AadlUtil.getContainingTopLevelNamespace(((SystemInstance) root).getComponentClassifier());
		if (tns instanceof PackageSection) {
			PackageSection ps = (PackageSection) tns;
			EList<ModelUnit> worklist = ps.getImportedUnits();
			while (!worklist.isEmpty()) {
				ModelUnit head = worklist.remove(0);
				if (head != null && head instanceof AadlPackage) {
					secLibs.addAll(AnnexUtil
							.getAllActualAnnexLibraries((AadlPackage) head, SecMFPackage.eINSTANCE.getSecModelLibrary())
							.stream().map(al -> (SecModelLibrary) al).collect(Collectors.toList()));

					AadlPackage ap = (AadlPackage) head;
					if (ap.getPublicSection() != null) {
						worklist.addAll(ap.getPublicSection().getImportedUnits());
					} else if (ap.getPrivateSection() != null) {
						worklist.addAll(ap.getPrivateSection().getImportedUnits());
					}
				}
			}
		}
		return secLibs;
	}

	// private findAllModelUnits()

	private List<SecModelSubclause> getSecModelSubclause(ComponentInstance element) {
		ComponentClassifier cl = element.getComponentClassifier();
		EList<AnnexSubclause> asc = AnnexUtil.getAllAnnexSubclauses(cl, SecMFPackage.eINSTANCE.getSecModelSubclause());
		return asc.stream().map(it -> (SecModelSubclause) it).collect(Collectors.toList());
	}

	private SmfClassification visitClassification(SMFClassification smfc, List<String> path) {
		NamedElement pname = smfc.getFeature();
		NamedElement tname = smfc.getTypeRef();
		Name portName = factory.name(VisitorUtil.add(path, pname.getName()), VisitorUtil.buildPosInfo(pname));
		Name typeName = factory.name(VisitorUtil.add(path, tname.getName()), VisitorUtil.buildPosInfo(tname));

		return factory.smfClassification(portName, typeName);
	}

	private SmfDeclass visitDeclass(SMFDeclassification smfdc, List<String> path) {
		NamedElement fname = smfdc.getFlow();
		NamedElement sname = smfdc.getSrcName();
		NamedElement tname = smfdc.getSnkName();

		Name flowName = factory.name(VisitorUtil.add(path, fname.getName()), VisitorUtil.buildPosInfo(fname));
		Name srcType = null;
		if (sname != null) {
			srcType = factory.name(VisitorUtil.add(path, sname.getName()), VisitorUtil.buildPosInfo(sname));
		}

		Name snkType = factory.name(VisitorUtil.add(path, tname.getName()), VisitorUtil.buildPosInfo(tname));

		return factory.smfDeclass(flowName, srcType, snkType);
	}

}
