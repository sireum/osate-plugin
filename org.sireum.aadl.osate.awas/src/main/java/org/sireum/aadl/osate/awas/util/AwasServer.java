package org.sireum.aadl.osate.awas.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.ui.IWorkbenchPage;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.modelsupport.EObjectURIWrapper;
import org.osate.ge.internal.diagram.runtime.DiagramElement;
import org.osate.ge.internal.services.DiagramService.DiagramReference;
import org.osate.ge.internal.ui.editor.AgeDiagramEditor;
import org.osate.ge.internal.ui.util.EditorUtil;
import org.sireum.aadl.osate.awas.handlers.AwasServerHandler;
import org.sireum.awas.ast.AwasSerializer;
import org.sireum.awas.ast.Model;
import org.sireum.awas.peti.PetiImpl;
import org.sireum.awas.peti.Protocol;
import org.sireum.awas.symbol.SymbolTable;
import org.sireum.awas.symbol.SymbolTableHelper;
import org.sireum.util.ConsoleTagReporter;

import scala.Option;
import scala.collection.JavaConverters;


public class AwasServer extends PetiImpl {
	Model awasModel = null;
	SystemInstance si = null;
//	IProject project = null;
//	DiagramService diagramService = null;
	IWorkbenchPage page = null;

	public AwasServer(Model awasModel, SystemInstance si, IWorkbenchPage page) {
		this.awasModel = awasModel;
		this.si = si;
//		this.diagramService = diagramService;
//		this.project = project;
		this.page = page;

		startServer();
		this.main(new String[0]);
		// this.handlePing();

	}

	public void updateModel(Model awasModel) {
		this.awasModel = awasModel;
		String awasJson = AwasSerializer.apply(awasModel);
//		SHA3 sha = SHA3.init512();
//		org.sireum.IS<org.sireum.Z, U8> hash = sha.finalise();
//		sha.update(org.sireum.conversions.String.toU8is(awasJson));
//		List<U8> hlist = VisitorUtil.isz2IList(hash);
//		StringBuilder sb = new StringBuilder("");
//		hlist.forEach(it -> sb.append(it.string()));

		try {
			String res = toHexString(getSHA(awasJson));

			this.sendHash(res);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// this.sendHash(sha.finalise(hstring));
	}


	@Override
	public Option<Model> getModel() {
		return new scala.Some<Model>(awasModel);
	}

	@Override
	public Option<Protocol> handleFindDef(String hash, scala.collection.immutable.Set<String> uris) {
		SymbolTable st = org.sireum.awas.symbol.SymbolTable$.MODULE$.apply(awasModel, new ConsoleTagReporter());
		String uri = JavaConverters.asJavaCollectionConverter(uris).asJavaCollection().iterator().next();
		Resource resource = si.eResource();
		Option<org.sireum.awas.ast.Node> t = SymbolTableHelper.uri2Node(uri, st);
		if (t.isDefined() && t.get().auriFrag().isDefined()) {
			EObject eo = resource.getResourceSet().getEObject(URI.createURI(t.get().auriFrag().get()), true);
			if (eo instanceof InstanceObject) {
				org.osate.ui.UiUtil.gotoInstanceObjectSource(page, (InstanceObject) eo);
			}
		}
		return Option.empty();
	}

	@SuppressWarnings("restriction")
	@Override
	public Option<Protocol> handleHighlight(scala.collection.immutable.Map<String, String> urisColor) {
		Map<String, String> toHighlight = new HashMap<>(JavaConverters.mapAsJavaMapConverter(urisColor).asJava());
		SymbolTable st = org.sireum.awas.symbol.SymbolTable$.MODULE$.apply(awasModel, new ConsoleTagReporter());
		Resource resource = si.eResource();
//		final DiagramService diagramService = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
//				.getService(DiagramService.class);

		//AgeDiagramEditor ade = diagramService.openOrCreateDiagramForBusinessObject(si, true, true);

		// toHighlight.entrySet().stream()

		Set<InstanceObject> ios = new HashSet<InstanceObject>();

		Map<URI, String> iUri = toHighlight.entrySet().stream().flatMap(kkk -> {
			Option<org.sireum.awas.ast.Node> t = SymbolTableHelper.uri2Node(kkk.getKey(), st);
			Map<URI, String> res = new HashMap<URI, String>();
			if (t.isDefined() && t.get().auriFrag().isDefined()) {
				EObject eo = resource.getResourceSet().getEObject(URI.createURI(t.get().auriFrag().get()), true);
				if (eo instanceof InstanceObject) {
					res.put(new EObjectURIWrapper(eo).getUri(), kkk.getValue());
					ios.add((InstanceObject) eo);
				}
			}
			return res.entrySet().stream();
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Set<DiagramElement> des = new HashSet<DiagramElement>();
		//des.addAll(AwasUtil.getAllDiagramElements(ade.getDiagramBehavior().getAgeDiagram()));

//		Set<IProject> projects = new HashSet();
//		projects.add(project);

		AwasServerHandler.highlightInstanceDiagram(iUri, si);



//		ade.getDiagramBehavior().updateDiagramWhenVisible();
//		ade.doSave(new NullProgressMonitor());

		return Option.empty();

	}

	@SuppressWarnings("restriction")
	@Override
	public Option<Protocol> handleClear(scala.collection.immutable.Set<String> uris) {
		Set<String> toClear = new HashSet<String>();
		toClear.addAll(JavaConverters.setAsJavaSet(uris));
		SymbolTable st = org.sireum.awas.symbol.SymbolTable$.MODULE$.apply(awasModel, new ConsoleTagReporter());
		Resource resource = si.eResource();

		Set<URI> iUri = toClear.stream().flatMap(mapper -> {
			Option<org.sireum.awas.ast.Node> t = SymbolTableHelper.uri2Node(mapper, st);
			Set<URI> res = new HashSet();
			if (t.isDefined() && t.get().auriFrag().isDefined()) {
				EObject eo = resource.getResourceSet().getEObject(URI.createURI(t.get().auriFrag().get()), true);
				if (eo instanceof InstanceObject) {
					res.add(new EObjectURIWrapper(eo).getUri());
				}
			}
			return res.stream();
		}).collect(Collectors.toSet());


		// des.addAll(AwasUtil.getAllDiagramElements(ade.getDiagramBehavior().getAgeDiagram()));

//		Set<IProject> projects = new HashSet();
//		projects.add(project);


		AwasServerHandler.clearInstanceDiagram(iUri, si);



		return Option.empty();
	}



	public static byte[] getSHA(String input) throws NoSuchAlgorithmException {
		// Static getInstance method is called with hashing SHA
		MessageDigest md = MessageDigest.getInstance("SHA-256");

		// digest() method called
		// to calculate message digest of an input
		// and return array of byte
		return md.digest(input.getBytes(StandardCharsets.UTF_8));
	}

	public static String toHexString(byte[] hash) {
		// Convert byte array into signum representation
		BigInteger number = new BigInteger(1, hash);

		// Convert message digest into hex value
		StringBuilder hexString = new StringBuilder(number.toString(16));

		// Pad with leading zeros
		while (hexString.length() < 32) {
			hexString.insert(0, '0');
		}

		return hexString.toString();
	}

	private AgeDiagramEditor getAgeDiagramEditor(DiagramReference diagramRef) {
		if (diagramRef.isOpen()) {
			return diagramRef.getEditor();
		} else {
			return EditorUtil.openEditor(diagramRef.getFile(), false);
		}

	}
}
