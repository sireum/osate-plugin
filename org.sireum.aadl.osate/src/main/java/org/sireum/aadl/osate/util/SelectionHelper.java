package org.sireum.aadl.osate.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.Element;
import org.osate.aadl2.SystemImplementation;

public class SelectionHelper {
	private static EObjectAtOffsetHelper eObjectAtOffsetHelper = new EObjectAtOffsetHelper();

	public static ISelection getSelection() {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
		IWorkbenchPage page = win.getActivePage();
		IWorkbenchPart part = page.getActivePart();
		IEditorPart activeEditor = page.getActiveEditor();
		if (activeEditor == null) {
			throw new RuntimeException("Unexpected case. Unable to get active editor");
		}

		final ISelection selection;
		if (part instanceof ContentOutline) {
			selection = ((ContentOutline) part).getSelection();
		} else {
			selection = getXtextEditor().getSelectionProvider().getSelection();
		}

		return selection;
	}

	// Based on code in: org.osate.xtext.aadl2.ui.handlers.InstantiateHandler
	// Gets the selected model object
	public static EObject getSelectedObject() {
		return getEObjectFromSelection(getSelection());
	}

	public static EObject getEObjectFromSelection(final ISelection selection) {
		return getXtextEditor().getDocument().readOnly(resource -> {
			EObject targetElement = null;
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				Object eon = ss.getFirstElement();
				if (eon instanceof EObjectNode) {
					targetElement = ((EObjectNode) eon).getEObject(resource);
				}
			} else {
				targetElement = eObjectAtOffsetHelper.resolveElementAt(resource,
						((ITextSelection) selection).getOffset());
			}

			return targetElement;
		});
	}

	public static SystemImplementation getSelectedSystemImplementation() {
		try {
			return getSelectedSystemImplementation(getSelection());
		} catch (Exception e) {
			return null;
		}
	}

	// Returns the SystemImplementation that is currently selected. If the
	// current selection is an object inside a SystemImplementation, such as a
	// PropertyAssociation, the SystemImplementaiton
	// is returned. If the selection is not inside a SystemImplementation, then
	// null is returned
	public static SystemImplementation getSelectedSystemImplementation(ISelection selection) {
		EObject selectedObject = getEObjectFromSelection(selection);

		// Return the object if it is a system implementation
		if (selectedObject instanceof SystemImplementation) {
			return (SystemImplementation) selectedObject;
		}

		// Otherwise, check if it is contained in a system implementation. This
		// should work in cases where the selection is a property association,
		// etc
		if (selectedObject instanceof Element) {
			Element aadlObject = (Element) selectedObject;
			Classifier containingClassifier = aadlObject.getContainingClassifier();
			if (containingClassifier instanceof SystemImplementation) {
				return (SystemImplementation) containingClassifier;
			}
		}

		return null;
	}

	// Get the current project by selection on navigator, active window or current editor
	public static IProject getProject() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IProject result = null;
		if (window != null) {
			IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof IAdaptable) {
				result = ((IAdaptable) firstElement).getAdapter(IProject.class);
			}
			if (result == null) {
				IWorkbenchPage activePage = window.getActivePage();
				IEditorPart activeEditor = activePage.getActiveEditor();
				if (activeEditor != null) {
					IEditorInput input = activeEditor.getEditorInput();
					result = input.getAdapter(IProject.class);
					if (result == null) {
						IResource resource = input.getAdapter(IResource.class);
						if (resource != null) {
							result = resource.getProject();
						}
					}
				}
			}
		}
		return result;
	}

	public static ISelection getDiagramSelection() {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
		if (win == null) {
			return StructuredSelection.EMPTY;
		}
		return win.getSelectionService().getSelection();
	}

	public static XtextEditor getXtextEditor() {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
		IWorkbenchPage page = win.getActivePage();
		IEditorPart activeEditor = page.getActiveEditor();
		if (activeEditor == null) {
			throw new RuntimeException("Unexpected case. Unable to get active editor");
		}

		XtextEditor xtextEditor = activeEditor.getAdapter(XtextEditor.class);
		if (xtextEditor == null) {
			throw new RuntimeException("Unexpected case. Unable to get Xtext editor");
		}

		return xtextEditor;
	}
}
