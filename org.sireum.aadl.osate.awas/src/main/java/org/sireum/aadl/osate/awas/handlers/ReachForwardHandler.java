package org.sireum.aadl.osate.awas.handlers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.osate.aadl2.Element;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;

public class ReachForwardHandler extends AbstractSireumHandler {
/*
	@SuppressWarnings("restriction")
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Boolean isImplDiagram = false;
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		List<DiagramElement> des = SelectionUtil.getSelectedDiagramElements(SelectionHelper.getDiagramSelection(),
				true);// getSelectedDiagramElem(event)
		List<InstanceObject> ios = des.stream().flatMap(de -> {
			Object bo = de.getBusinessObject();
			List<InstanceObject> io = new ArrayList();
			if (bo != null && bo instanceof InstanceObject) {
				io.add(((InstanceObject) bo));
			}
			return io.stream();
		}).collect(Collectors.toList());

		List<String> criterions = ios.stream().map(io -> io.getInstanceObjectPath()).collect(Collectors.toList());

		if (!ios.isEmpty()) {

			MessageConsole console = displayConsole("Awas Console");
			try {
				Aadl model = Util.getAir(ios.get(0).getSystemInstance(), true, console);
				Model awasModel = org.sireum.awas.slang.Aadl2Awas$.MODULE$.apply(model);
				SymbolTable st = org.sireum.awas.symbol.SymbolTable$.MODULE$.apply(awasModel, new ConsoleTagReporter());
				FlowGraph<FlowNode, FlowEdge<FlowNode>> graph = org.sireum.awas.flow.FlowGraph$.MODULE$.apply(awasModel,
						st, false);
				AwasGraph awasgraph = new AwasGraphImpl(graph, st);
				String query = "t = reach forward " + "{" + String.join(",", criterions) + "}";
				Map<String, Collector> qres = awasgraph.queryEvaluator(query);
				if (qres.isEmpty()) {
					MessageDialog.openError(window.getShell(), "Sireum", "Empty result");
				} else {
					final DiagramService diagramService = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getService(DiagramService.class);
					SystemInstance si = ios.get(0).getSystemInstance();
					Resource resource = si.eResource();
					ComponentImplementation cii = InstanceUtil.getComponentImplementation(si, 0, null);
					// cii.eResource().getResourceSet().get
					List<Collector> lc = new ArrayList<Collector>(qres.values());
					Set<AgeDiagramEditor> ads = AwasUtil.awasGraphUri2AgeDiagramEditor(lc.get(0).getGraph(),
							isImplDiagram, st, resource, diagramService);
					AwasUtil.highlightDiagrams(ads, lc.get(0), isImplDiagram, st, resource);
				}
			} catch (URISyntaxException e1) {
//				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (Exception e3) {
				e3.printStackTrace();
				String m2 = "Could not invoke visualizer.  Please make sure Awas is configured correctly.\n\n"
						+ e3.getLocalizedMessage();
				MessageDialog.openError(window.getShell(), "Sireum", m2);
			}

		} else {
			String m3 = "Please select a component or port from the instance diagram";
			MessageDialog.openError(window.getShell(), "Sireum", m3);
		}

		return null;
	}
*/
	@Override
	protected IStatus runJob(Element arg0, IProgressMonitor arg1) {
		// TODO Auto-generated method stub
		return null;
	}

}
