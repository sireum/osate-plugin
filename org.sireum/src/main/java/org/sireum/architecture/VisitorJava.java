package org.sireum.architecture;

import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.osate.aadl2.Aadl2Package;
import org.osate.aadl2.AnnexLibrary;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.aadl2.instance.InstancePackage;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instance.SystemOperationMode;
import org.osate.aadl2.instance.util.InstanceSwitch;
import org.osate.aadl2.util.Aadl2Switch;

public abstract class VisitorJava<L> {

	protected Aadl2Switch<L> aadl2Switch = null;// new Aadl2Switch<L>();

	protected InstanceSwitch<L> instanceSwitch = null;// new InstanceSwitch<L>();

	public abstract void initSwitches();

	public void visitRoot(final Element root) {
		if (root != null) {
			// System.out.println("visitRoot with " + root);

			L ret = processObject(root);

			EList<Element> kids = root.getChildren();

			final int nChildren = kids.size();

			// System.out.println(" ret was " + ret + " num kids = " + nChildren);

			if (ret == null) {
				for (int i = 0; (i < nChildren); i++) {
					visitRoot(kids.get(i));
				}
			}
		}
	}

	public final L processObject(final Element theElement) {
		if (theElement instanceof InstanceObject) {
			InstanceObject io = (InstanceObject) theElement;
			List<SystemOperationMode> modes = io.getExistsInModes();

			if (modes == null) {
				return process(io);
			} else {
				SystemInstance root = io.getSystemInstance();
				SystemOperationMode som = root.getCurrentSystemOperationMode();

				if (som == null || modes.contains(som)) {
					return process(io);
				}
			}
		} else {
			return process(theElement);
		}

		throw new RuntimeException("unexpected 1");
	}

	public final L process(final Element theElement) {
		final EClass theEClass;
		/**
		 * This checks to make sure we only invoke doSwitch with non-null
		 * objects This is necessary as some feature retrieval methods may
		 * return null
		 */
		if (theElement == null) {
			return null;
		}

		theEClass = theElement.eClass();
		if (aadl2Switch != null && (theEClass.eContainer() == Aadl2Package.eINSTANCE
				|| theElement instanceof AnnexLibrary || theElement instanceof AnnexSubclause)) {
			// System.out.println("It's an aadl model");
			return aadl2Switch.doSwitch(theElement);
		} else if (instanceSwitch != null && theEClass.eContainer() == InstancePackage.eINSTANCE) {
			// System.out.println("It's an instance model");
			return instanceSwitch.doSwitch(theElement);
		}

		throw new RuntimeException("unexpected 2");
	}
}
