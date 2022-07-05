package org.sireum.aadl.osate.util;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.IntSupplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.ui.console.MessageConsole;
import org.osate.aadl2.ComponentClassifier;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.sireum.B;
import org.sireum.IS;
import org.sireum.Option;
import org.sireum.SireumApi;
import org.sireum.U8;
import org.sireum.Z;
import org.sireum.aadl.osate.PreferenceValues;
import org.sireum.aadl.osate.architecture.Visitor;
import org.sireum.hamr.ir.Aadl;
import org.sireum.hamr.ir.JSON;
import org.sireum.hamr.ir.MsgPack;
import org.sireum.message.Message;
import org.sireum.message.Position;
import org.sireum.message.Reporter;

import scala.Console;
import scala.Function0;
import scala.Function1;
import scala.runtime.BoxedUnit;

public class Util {

	public static Reporter createReporter() {
		return org.sireum.message.Reporter$.MODULE$.create();
	}

	public enum SerializerType {
		JSON, JSON_COMPACT, MSG_PACK
	}

	public static String serialize(Aadl model, SerializerType t) {
		switch (t) {
		case JSON:
			return JSON.fromAadl(model, false);
		case JSON_COMPACT:
			return JSON.fromAadl(model, true);
		case MSG_PACK:
			IS<Z, U8> x = MsgPack.fromAadl(model, true);
			String ret = org.sireum.conversions.String.toBase64(x).toString();
			return ret;
		default:
			return null;
		}
	}

	public static Aadl getAir(ComponentInstance root, Reporter reporter) {
		return getAir(root, true, reporter);
	}

	public static Aadl getAir(ComponentInstance root, boolean includeDataComponents, Reporter reporter) {
		return getAir(root, includeDataComponents, reporter, System.out);
	}

	public static Aadl getAir(ComponentInstance root, boolean includeDataComponents, MessageConsole console,
			Reporter reporter) {
		try (OutputStream out = console.newOutputStream()) {
			return getAir(root, includeDataComponents, reporter, out);
		} catch (Throwable t) {
			return null;
		}
	}

	public static Aadl getAir(ComponentInstance root, boolean includeDataComponents, Reporter reporter,
			OutputStream out) {
		try {
			return new Visitor(reporter).convert(root, includeDataComponents).get();
		} catch (Throwable t) {
			VisitorUtil.reportError("Error encountered while generating AIR", PreferenceValues.SIREUM_PLUGIN_ID,
					reporter);

			PrintStream p = new PrintStream(out);
			p.println("Error encountered while generating AIR");
			t.printStackTrace(p);
			p.close();
			return null;
		}
	}

	public static int callWrapper(String toolName, MessageConsole ms, IntSupplier f) {
		int[] ret = { -1 };

		PrintStream out = new PrintStream(ms.newMessageStream());
		PrintStream outOld = System.out;
		PrintStream errOld = System.err;

		System.setOut(out);
		System.setErr(out);

		Console.withOut(System.out, (Function0<Object>) () -> {
			Console.withErr(System.err, (Function0<Object>) () -> {

				try {
					ret[0] = f.getAsInt();
				} catch (Throwable t) {
					System.err.println("Exception raised when invoking " + toolName);
					t.printStackTrace(out);
				} finally {
					out.flush();
					try {
						if (out != null) {
							out.close();
						}
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}

				return BoxedUnit.UNIT;
			});
			return BoxedUnit.UNIT;
		});

		System.setOut(outOld);
		System.setErr(errOld);

		return ret[0];
	}

	public static IFile toIFile(URI resourceURI) {
		/*
		 * Ideally we'd just call OsateResourceUtil.toIFile however that is not
		 * available in OSATE 2.4.x (which the CASE FM-IDE is based on). Workaround
		 * is to just replicate the current behavior of that method, refer to
		 * <a href=
		 * "https://github.com/osate/osate2/blob/bed18dd95fe3f3bf54d657911cd5e5da1ff2718b/core/org.osate.aadl2.modelsupport/src/org/osate/aadl2/modelsupport/resources/OsateResourceUtil.java#L62"
		 * >this</a>
		 */

		// return OsateResourceUtil.toIFile(resourceURI);

		if (resourceURI.isPlatform()) {
			return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(resourceURI.toPlatformString(true)));
		} else {
			return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(resourceURI.toFileString()));
		}
	}

	public static boolean emitSireumVersion(MessageConsole ms) {
		PrintStream out = new PrintStream(ms.newMessageStream());
		boolean ret = emitSireumVersion(out);
		out.close();
		return ret;
	}

	public static boolean emitSireumVersion(PrintStream out) {
		String propName = "org.sireum.home";
		String propValue = System.getProperty(propName);
		if (propValue != null) {
			File sireum_jar = new File(propValue, "bin/sireum.jar");
			if (!sireum_jar.exists()) {
				out.print("sireum.jar not found. Expecting it to be at: " + sireum_jar.getAbsolutePath() //
						+ "\n" //
						+ "\n" //
						+ "Ensure that the '" + propName + "' Java system property (current value is '" + propValue
						+ "') is set \n"
						+ "to the absolute path to your Sireum installation (sireum.jar should be in its 'bin' directory). \n"
						+ "You must restart OSATE in order for changes to osate.ini to take effect.\n");
				return false;
			} else {
				out.print("Sireum Version: " + SireumApi.version() + " located at " + sireum_jar.getAbsolutePath()
						+ "\n");
				return true;
			}
		} else {
			out.print("Java system property '" + propName + "' not set. \n" //
					+ "\n" //
					+ "The prefered way of setting this is by installing the HAMR plugin via Phantom.  Run \n" //
					+ "the following from the command line for more information\n" //
					+ "\n" //
					+ "    $SIREUM_HOME/bin/sireum hamr phantom -h \n" //
					+ "\n" //
					+ "If you don't have Sireum installed then refer to https://github.com/sireum/kekinian#installing \n"
					+ "\n" //
					+ "\n" //
					+ "To set this property manually, in your osate.ini file locate the line containing '-vmargs' and \n"
					+ "add the following on a new line directly after that \n" + "\n" //
					+ "    -D" + propName + "=<path-to-sireum>\n" //
					+ "\n" //
					+ "replacing <path-to-sireum> with the absolute path to your Sireum installation \n"
					+ "(sireum.jar should be under its 'bin' directory).  Then restart OSATE. \n" //
					+ "\n" //
					+ "Alternatively, start OSATE using the vmargs option.  For example: \n" //
					+ "\n" //
					+ "    <path-to-osate>/osate -vmargs " + propName + "=<path-to-sireum>\n");
			return false;
		}
	}

	public enum SeverityLevel {
		Info, Warning, Error
	}

	/**
	 * Adds any message with position info to the OSATE problems view
	 * @param markerId marker's unique id e.g. org.sireum.aadl.osate.marker
	 * @param severityToDisplay messages to be displayed.  Displays all if null or empty
	 * @param si
	 * @param reporter
	 */
	public static void addMarkers(String markerId, List<SeverityLevel> severityToDisplay, SystemInstance si,
			Reporter reporter) {

		Util.clearMarkers(si, markerId, true);

		IS<Z, Message> messages = reporter.messages().filter((Function1<Message, B>) (Message message) -> { //
			if (severityToDisplay == null || severityToDisplay.isEmpty()) {
				return new org.sireum.B(true);
			} else {
				return new org.sireum.B(//
						(message.isError() && severityToDisplay.contains(SeverityLevel.Error)) //
								|| (message.isInfo() && severityToDisplay.contains(SeverityLevel.Info)) //
								|| (message.isWarning() && severityToDisplay.contains(SeverityLevel.Warning)));
			}
		});

		for (int i = 0; i < messages.size().toInt(); i++) {

			Message m = messages.apply(SlangUtil.toZ(i));

			IResource _iresource = null;
			Option<Position> _posOpt = SlangUtil.toNone();

			if (m.getPosOpt() != null && m.getPosOpt().nonEmpty() && m.getPosOpt().get().uriOpt().nonEmpty()) {
				String uri = "/resource" + m.getPosOpt().get().uriOpt().get().value();
				_posOpt = m.getPosOpt();

				for (Resource cand : si.eResource().getResourceSet().getResources()) {
					if (cand.getURI().path().equals(uri)) {
						_iresource = Util.toIFile(cand.getURI());
						break;
					}
				}
			} else {
				ComponentClassifier sysC = si.getComponentImplementation() != null ? si.getComponentImplementation()
						: si.getComponentClassifier();
				_posOpt = VisitorUtil.buildPositionOpt(sysC);
				_iresource = Util.toIFile(sysC.eResource().getURI());
			}

			final IResource iresource = _iresource;
			final Option<Position> posOpt = _posOpt;

			try {
				IWorkspaceRunnable runnable = monitor -> {
					IMarker marker = iresource.createMarker(markerId);

					marker.setAttribute(IMarker.MESSAGE, m.getText().toString());

					if (m.isError() || m.isInternalError()) {
						marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					} else if (m.isWarning()) {
						marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
					} else if (m.isInfo()) {
						marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_LOW);
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
					}

					if (posOpt.nonEmpty()) {
						marker.setAttribute(IMarker.LINE_NUMBER, posOpt.get().beginLine().toInt());
						if (posOpt.get().offset().toInt() != 0 && posOpt.get().length().toInt() > 0) {
							marker.setAttribute(IMarker.CHAR_START, posOpt.get().offset().toInt());
							marker.setAttribute(IMarker.CHAR_END,
									posOpt.get().offset().toInt() + posOpt.get().length().toInt());
						}
					}
				};

				iresource.getWorkspace().run(runnable, null, IWorkspace.AVOID_UPDATE, null);

			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	public static void clearAllSireumMarkers() {
		clearMarkers(PreferenceValues.SIREUM_MARKER_ID, true);
	}

	public static void clearMarkers(SystemInstance si, String markerId, boolean includeSubtypes) {
		ResourceSet rs = si.eResource().getResourceSet();
		for (Resource r : rs.getResources()) {
			IFile i = Util.toIFile(r.getURI());
			try {
				i.deleteMarkers(markerId, includeSubtypes, IResource.DEPTH_INFINITE);
			} catch (CoreException e) {
				// e.printStackTrace();
			}
		}
	}

	public static void clearMarkers(String markerId, boolean includeSubtypes) {
		IProject project = SelectionHelper.getProject();
		if (project != null) {
			try {
				for (IResource r : project.members()) {
					r.deleteMarkers(markerId, includeSubtypes, IResource.DEPTH_INFINITE);
				}
			} catch (CoreException e) {
				// e.printStackTrace();
			}
		}
	}
}
