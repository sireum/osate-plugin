package org.sireum.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osate.aadl2.ComponentClassifier;
import org.osate.aadl2.Element;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.SystemSubcomponent;
import org.osate.aadl2.instance.AnnexInstance;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.ConnectionInstanceEnd;
import org.osate.aadl2.instance.EndToEndFlowInstance;
import org.osate.aadl2.instance.FeatureInstance;
import org.osate.aadl2.instance.FlowSpecificationInstance;
import org.osate.aadl2.instance.ModeInstance;
import org.osate.aadl2.instance.util.InstanceSwitch;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.aadl2.util.Aadl2Switch;
import org.sireum.architecture.Visitor;
import org.sireum.architecture.VisitorJava;
import org.sireum.util.SelectionHelper;

public abstract class AbstractSireumHandler extends AbstractHandler {

	private String generator = null;
	private SystemImplementation systemImplementation;

	Object o = null;

	void push(Object _o) {
		assert (o == null);
		o = _o;
	}

	Object pop() {
		assert (o != null);
		Object t = o;
		o = null;
		return t;
	}

	@Override
	public Object execute(ExecutionEvent e) throws ExecutionException {
		if(this.generator == null) {
			throw new RuntimeException("Generator is null");
		}

		Element root = AadlUtil.getElement(getCurrentSelection(e));

		if (root == null) {
			root = SelectionHelper.getSelectedSystemImplementation();
		}

		// System.out.println("root = " + root);

		final IWorkbench wb = PlatformUI.getWorkbench();
		final IWorkbenchWindow window = wb.getActiveWorkbenchWindow();

		if (root != null) {
			Visitor t = new Visitor();
			Object _r = t.visit(root);


			final VisitorJava<Boolean> v = new VisitorJava<Boolean>() {

				//Visitor<Boolean> _v = this;

				@Override
				public void initSwitches() {
					this.aadl2Switch = new Aadl2Switch<Boolean>() {
						@Override
						public Boolean caseSystemImplementation(SystemImplementation o) {
							System.out.println("visiting SystemImplementation: " + o.getQualifiedName());

							return null;
							// return Boolean.TRUE;
						}

						@Override
						public Boolean caseSystemSubcomponent(SystemSubcomponent o) {
							System.out.println("visiting SystemSubcomponent " + o);
							return null;
						}
					};

					/**
					 * INSTANCE SWITCH
					 **/

					final VisitorJava<Boolean> _v = this;

					instanceSwitch = new InstanceSwitch<Boolean>() {
						void visit(Element e) {
							_v.visitRoot(e);
						}

						@Override
						public Boolean caseComponentInstance(ComponentInstance obj) {

							String identifier = obj.getFullName();
							System.out.println(identifier);

							System.out.println("\nClassifier\n");
							// classifier
							ComponentClassifier cls = obj.getClassifier();
							System.out.println(cls);

							System.out.println("\nFeatures\n");
							// features?
							for(FeatureInstance fi: obj.getFeatureInstances()) {
								System.out.println(fi);
							}

							System.out.println("\nComponents\n");
							// subcomponents?
							for(ComponentInstance ci: obj.getComponentInstances()) {
								System.out.println(ci);
							}

							System.out.println("\nConnections\n");
							// connections??
							for(ConnectionInstance ci : obj.getConnectionInstances()) {
								System.out.println(ci);
								ConnectionInstanceEnd cies = ci.getSource();
								ConnectionInstanceEnd cied = ci.getDestination();

								System.out.println("  " + cies.getComponentInstancePath());
								System.out.println("  " + cied.getComponentInstancePath());

								for(PropertyAssociation pa : ci.getOwnedPropertyAssociations()) {
									System.out.println("  " + pa);
								}
							}

							System.out.println("\nProperties\n");
							// properties??
							for(PropertyAssociation pa : obj.getOwnedPropertyAssociations()) {
								System.out.println(pa);
							}

							System.out.println("\nFlows\n");
							// flows??
							for(FlowSpecificationInstance fs : obj.getFlowSpecifications()) {
								System.out.println(fs);
							}

							System.out.println("\nModes\n");
							// modes??
							for(ModeInstance mi : obj.getInModes()) {
								System.out.println(mi);
							}

							System.out.println("\nAnnexes\n");
							// annexes??
							for(AnnexInstance ai : obj.getAnnexInstances()) {
								System.out.println(ai);
							}


							switch (obj.getCategory()) {
							case SYSTEM:
								System.out.println("It's a system " + obj);
								break;
							case THREAD:
								System.out.println("It's a thread " + obj);
								break;
							case PROCESS:
								System.out.println("It's a process " + obj);
								break;
							case PROCESSOR:
								System.out.println("It's a process " + obj);
								break;
							case VIRTUAL_PROCESSOR:
								System.out.println("It's a virtual_processor " + obj.getFullName());
								break;
							case MEMORY:
								System.out.println("It's a memory " + obj.getFullName());
								break;
							case BUS:
								System.out.println("It's a bus " + obj.getFullName());
								break;
							case VIRTUAL_BUS:
								System.out.println("It's a virtual_bus " + obj.getFullName());
								break;
							case DEVICE:
								System.out.println("It's a device " + obj.getFullName());
								break;
							default:
								System.out.println("Hit default case " + obj);
								break;
							}

							return Boolean.FALSE;
						}

						@Override
						public Boolean caseConnectionInstance(ConnectionInstance ci) {
							System.out.println("It's a ConnectionInstance " + ci.getQualifiedName());
							return Boolean.TRUE;
						}

						@Override
						public Boolean caseEndToEndFlowInstance(EndToEndFlowInstance ci) {
							System.out.println("It's a EndToEndFlowInstance " + ci.getQualifiedName());
							return Boolean.TRUE;
						}
					};

				}
			};

			v.initSwitches();
			// v.visitRoot(root);

		} else {
			MessageDialog.openError(window.getShell(), "Sireum",
					"Please select a System Implementation (** don't put the cursor in the system's name **");
		}

		return null;
	}

	protected Object getCurrentSelection(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1) {
			Object object = ((IStructuredSelection) selection).getFirstElement();
			return object;
		} else {
			return null;
		}
	}

	protected void setGenerator(String v) {
		this.generator = v;
	}
}
