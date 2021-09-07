package org.sireum.aadl.osate.awas.handlers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osate.aadl2.Element;
import org.osate.ge.gef.ui.editor.AgeEditor;
import org.osate.ge.graphics.Style;
import org.osate.ge.internal.services.ActionExecutor.ExecutionMode;
import org.osate.ge.internal.services.DiagramService;
import org.sireum.aadl.osate.awas.util.AwasUtil;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;
import org.sireum.aadl.osate.util.SelectionHelper;

public class ReachEraseHandler extends AbstractSireumHandler {

	@SuppressWarnings({ "restriction", "restriction" })
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		final DiagramService diagramService = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getService(DiagramService.class);

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IWorkbenchPage page = window.getActivePage();

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		IProject currProject = SelectionHelper.getProject();

		if (currProject != null) {
			Set<IProject> projects = new HashSet();
			projects.add(currProject);

			diagramService.findDiagrams(projects).forEach(dr -> {
				if (dr.isValid() && dr.isOpen()) {
					if (AwasUtil.getAgeDiagramEditor(dr) == page.getActiveEditor()) {
						AgeEditor agede = (AgeEditor) AwasUtil.getAgeDiagramEditor(dr);
						AwasUtil.getAllDiagramElements(agede.getDiagram())
								.forEach(de -> de.setStyle(Style.DEFAULT));

						agede.updateDiagram();// .getDiagramBehavior().updateDiagramWhenVisible();
						// agede.doSave(new NullProgressMonitor());
						agede.getActionExecutor().execute("highlight diagram", ExecutionMode.NORMAL, () -> {
							agede.updateNowIfModelHasChanged();
							agede.updateDiagram();
							agede.getGefDiagram().refreshDiagramStyles();
							agede.doSave(new NullProgressMonitor());
							return null;
						});
					}
					}

			});


		} else {
			String m3 = "Please select a project to erase diagrams";
			MessageDialog.openError(window.getShell(), "Sireum", m3);
		}

		return null;
	}

	@Override
	protected IStatus runJob(Element arg0, IProgressMonitor arg1) {
		// TODO Auto-generated method stub
		return null;
	}


//	private AgeDiagramEditor getAgeDiagramEditor(DiagramReference diagramRef) {
//		if (diagramRef.isOpen()) {
//			return diagramRef.getEditor();
//		} else {
//			return EditorUtil.openEditor(diagramRef.getFile(), false);
//		}
//
//	}
}
