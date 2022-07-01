package org.sireum.aadl.osate.awas.handlers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.ge.gef.ui.editor.AgeEditor;
import org.osate.ge.internal.diagram.runtime.DiagramElement;
import org.osate.ge.internal.services.DiagramService;
import org.osate.ge.internal.ui.util.SelectionUtil;
import org.sireum.aadl.osate.awas.util.AwasUtil;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;
import org.sireum.aadl.osate.util.SelectionHelper;
import org.sireum.aadl.osate.util.Util;
import org.sireum.awas.ast.Model;
import org.sireum.awas.awasfacade.AwasGraph;
import org.sireum.awas.awasfacade.AwasGraphImpl;
import org.sireum.awas.awasfacade.Collector;
import org.sireum.awas.flow.FlowEdge;
import org.sireum.awas.flow.FlowGraph;
import org.sireum.awas.flow.FlowNode;
import org.sireum.awas.symbol.SymbolTable;
import org.sireum.hamr.ir.Aadl;
import org.sireum.message.Reporter;
import org.sireum.util.ConsoleTagReporter;

public class ReachBackwardHandler extends AbstractSireumHandler {

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
			List<InstanceObject> io = new ArrayList<InstanceObject>();
			if (bo != null && bo instanceof InstanceObject) {
				io.add(((InstanceObject) bo));
			}
			return io.stream();
		}).collect(Collectors.toList());

		List<String> criterions = ios.stream().map(io -> io.getInstanceObjectPath()).collect(Collectors.toList());

		if (!ios.isEmpty()) {

			MessageConsole console = displayConsole("Awas Console");
			try {
				Reporter reporter = Util.createReporter();
				Aadl model = Util.getAir(ios.get(0).getSystemInstance(), true, console, reporter);
				if(reporter.hasError()) {
					// TODO should handle this.  Could convert errors to markers -- see hamr plugin
				}
				Model awasModel = org.sireum.awas.slang.Aadl2Awas$.MODULE$.apply(model);
				SymbolTable st = org.sireum.awas.symbol.SymbolTable$.MODULE$.apply(awasModel, new ConsoleTagReporter());
				FlowGraph<FlowNode, FlowEdge<FlowNode>> graph = org.sireum.awas.flow.FlowGraph$.MODULE$.apply(awasModel,
						st, false);
				AwasGraph awasgraph = new AwasGraphImpl(graph, st);
				String query = "t = reach backward " + "{" + String.join(",", criterions) + "}";
				Map<String, Collector> qres = awasgraph.queryEvaluator(query);
				if (qres.isEmpty()) {
					MessageDialog.openError(window.getShell(), "Sireum", "Empty result");
				} else {
					final DiagramService diagramService = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getService(DiagramService.class);
					SystemInstance si = ios.get(0).getSystemInstance();
					Resource resource = si.eResource();
					List<Collector> lc = new ArrayList<Collector>(qres.values());
					Set<AgeEditor> ads = AwasUtil.awasGraphUri2AgeDiagramEditor(lc.get(0).getGraph(),
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

	@Override
	protected IStatus runJob(Element arg0, IProgressMonitor arg1) {
		// TODO Auto-generated method stub
		return null;
	}
}
