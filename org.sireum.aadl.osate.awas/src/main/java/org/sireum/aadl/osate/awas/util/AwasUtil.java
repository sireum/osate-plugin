package org.sireum.aadl.osate.awas.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instance.util.InstanceUtil;
import org.osate.aadl2.modelsupport.EObjectURIWrapper;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.ge.GraphicalEditor;
import org.osate.ge.gef.AgeGefRuntimeException;
import org.osate.ge.gef.ui.editor.AgeEditor;
import org.osate.ge.graphics.StyleBuilder;
import org.osate.ge.internal.diagram.runtime.AgeDiagram;
import org.osate.ge.internal.diagram.runtime.DiagramElement;
import org.osate.ge.internal.services.ActionExecutor.ExecutionMode;
import org.osate.ge.internal.services.DiagramService;
import org.osate.ge.internal.services.DiagramService.DiagramReference;
import org.osate.ge.internal.ui.editor.InternalDiagramEditor;
import org.osate.ge.internal.ui.util.EditorUtil;
import org.osate.ui.UiUtil;
import org.sireum.awas.ast.Node;
import org.sireum.awas.awasfacade.Collector;
import org.sireum.awas.collector.ResultType;
import org.sireum.awas.flow.NodeType;
import org.sireum.awas.symbol.SymbolTable;
import org.sireum.awas.symbol.SymbolTableHelper;

import scala.Option;

public class AwasUtil {

	public static Set<EObject> awasUri2EObject(Set<String> uris, SymbolTable st, Resource resource) {
		Set<EObject> ios = uris.stream().flatMap(it -> {
			Option<Node> t = SymbolTableHelper.uri2Node(it, st);
			Set<EObject> res = new HashSet<EObject>();
			if (t.isDefined() && t.get().auriFrag().isDefined()) {
				EObject eo = resource.getResourceSet().getEObject(URI.createURI(t.get().auriFrag().get()), true);
				// EObject eo = resource.getEObject(t.get().auriFrag().get());
				res.add(eo);
			}
			return res.stream();
		}).collect(Collectors.toSet());
		return ios;
	}
	public static Set<InstanceObject> awasUri2AadlInstObj(Set<String> uris, SymbolTable st, Resource resource) {
		return awasUri2EObject(uris, st, resource).stream().filter(it -> (it instanceof InstanceObject))
				.map(it -> (InstanceObject) it).collect(Collectors.toSet());
	}

	public static Set<Element> instObjs2Elements(Set<InstanceObject> instObjs) {
		return instObjs.stream().map(it -> AadlUtil.getInstanceOrigin(it)).collect(Collectors.toSet());
	}

	@SuppressWarnings("restriction")
	public static void highlightDiagrams(Set<AgeEditor> ads, Collector qres, Boolean isImpleDiagram,
			SymbolTable st,
			Resource resource) {

		// AtomicBoolean isNode = new AtomicBoolean(true);
		Set<String> uris = new HashSet<String>();
		if (qres.getResultType().isPresent()) {
			if (qres.getResultType().get() == ResultType.Node()) {
				uris.addAll(qres.getNodes().stream().map(it -> it.getUri()).collect(Collectors.toSet()));
			} else {
				uris.addAll(qres.getPorts());
				uris.addAll(qres.getFlows());
				uris.addAll(qres.getNodes().stream().filter(it -> it.getResourceType() == NodeType.CONNECTION())
						.map(it -> it.getUri()).collect(Collectors.toSet()));
				// isNode.getAndSet(false);
			}
		} else if (!qres.getNodes().isEmpty()) {
			uris.addAll(qres.getNodes().stream().map(it -> it.getUri()).collect(Collectors.toSet()));
		}

		Set<InstanceObject> ios = awasUri2AadlInstObj(uris, st, resource);

		final EObjectURIWrapper.Factory factory = new EObjectURIWrapper.Factory(UiUtil.getModelElementLabelProvider());

		Set<URI> aadlUris = new HashSet<URI>();
		if (isImpleDiagram) {
			Set<Element> elems = instObjs2Elements(ios);
			aadlUris.addAll(elems.stream()
					.map(it -> factory.createWrapperFor(it).getUri())
					.collect(Collectors.toSet()));
		} else {
			aadlUris.addAll(ios.stream().map(it -> factory.createWrapperFor(it).getUri()).collect(Collectors.toSet()));
		}

		Set<DiagramElement> des = new HashSet<DiagramElement>();

		ads.forEach(ad -> {
			des.addAll(getAllDiagramElements(ad.getDiagram()));// getDiagram()));
		});

		des.forEach(de -> {
			if (de.getBusinessObject() instanceof EObject
					&& aadlUris.contains(factory.createWrapperFor((EObject) de.getBusinessObject()).getUri())) {
				de.setStyle(StyleBuilder.create(de.getStyle()).backgroundColor(org.osate.ge.graphics.Color.ORANGE)
						// .fontColor(org.osate.ge.graphics.Color.ORANGE)
						.outlineColor(org.osate.ge.graphics.Color.MAGENTA)
						.build());
			}
		});
		ads.forEach(ad -> {
			ad.getActionExecutor().execute("highlight diagram", ExecutionMode.NORMAL, () -> {
				ad.updateNowIfModelHasChanged();
				ad.updateDiagram();
				ad.getGefDiagram().refreshDiagramStyles();
				ad.doSave(new NullProgressMonitor());
				return null;
			});
		});
	}

	public static Set<DiagramElement> getAllDiagramElements(AgeDiagram diagram) {
		Set<DiagramElement> result = new HashSet();

		List<DiagramElement> worklist = new ArrayList();

		worklist.addAll(diagram.getChildren());

		while (!worklist.isEmpty()) {
			DiagramElement curr = worklist.remove(0);
			result.add(curr);
			worklist.addAll(curr.getChildren());
		}
		return result;
	}

	@SuppressWarnings("restriction")
	public static Set<AgeEditor> awasGraphUri2AgeDiagramEditor(Set<String> graphs, Boolean isImpleDiagram,
			SymbolTable st, Resource resource, DiagramService diagramService) {

		List<String> graphFrags = graphs.stream().flatMap(it -> {
			Option<Node> t = SymbolTableHelper.uri2Node(it, st);
			List<String> res = new ArrayList<String>();
			if (t.isDefined() && t.get().auriFrag().isDefined()) {
				res.add(t.get().auriFrag().get());
			}
			return res.stream();
		}).collect(Collectors.toList());

		final List<EObject> cis = graphFrags.stream()
				.map(it -> resource.getResourceSet().getEObject(URI.createURI(it), true)).collect(Collectors.toList());

		Set<AgeEditor> ads = new HashSet<AgeEditor>();
		for (EObject ci : cis) {
			if (ci instanceof ComponentInstance) {
				GraphicalEditor ade = null;
				if (isImpleDiagram) {
					ComponentImplementation cii = InstanceUtil.getComponentImplementation((ComponentInstance) ci, 0,
							null);
//				AgeDiagramEditor ade = openOrCreateDiagramForBusinessObject(cii, diagramService);
					ade = diagramService.openOrCreateDiagramForBusinessObject(cii, false, false);
				} else {
					ade = diagramService.openOrCreateDiagramForBusinessObject(
							((ComponentInstance) ci).getSystemInstance(), true, true);
				}

				if (!(ade instanceof AgeEditor)) {
					throw new AgeGefRuntimeException("Unexpected editor type. Editor must be of type " + AgeEditor.class);
				}
				ads.add((AgeEditor) ade);
			}
		}
		return ads;
	}

	@SuppressWarnings("restriction")
	public static Set<GraphicalEditor> awasGraphUri2AgeDiagramEditor(SystemInstance si, SymbolTable st,
			Resource resource, DiagramService diagramService) {


		Set<GraphicalEditor> ads = new HashSet<GraphicalEditor>();

		ads.add(diagramService.openOrCreateDiagramForBusinessObject(si, true, true));

		return ads;
	}

	@SuppressWarnings("restriction")
	public static InternalDiagramEditor getAgeDiagramEditor(DiagramReference diagramRef) {

		return EditorUtil.openEditor(diagramRef.getFile(), false);

//		if (diagramRef.isOpen()) {
//			return diagramRef.getEditor();
//		} else {
//			return EditorUtil.openEditor(diagramRef.getFile(), false);
//		}

	}



	public static org.osate.ge.graphics.Color hex2Rgb(String colorStr) {
		return new org.osate.ge.graphics.Color(Integer.valueOf(colorStr.substring(1, 3), 16),
				Integer.valueOf(colorStr.substring(3, 5), 16), Integer.valueOf(colorStr.substring(5, 7), 16));
	}

}
