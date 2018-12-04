package org.sireum.aadl.osate.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.osate.aadl2.Aadl2Package;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.assure.tests.FullAlisaInjectorProvider;
import org.osate.core.OsateCorePlugin;
import org.osate.pluginsupport.PluginSupportUtil;
import org.osate.testsupport.Aadl2InjectorProvider;
import org.osate.xtext.aadl2.properties.ui.internal.PropertiesActivator;
import org.sireum.aadl.osate.util.Util;

import com.google.inject.Injector;

@SuppressWarnings("restriction")
public class GenerateAir implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		context.applicationRunning();

		OsateCorePlugin.getDefault().start(Activator.getContext());
		Aadl2InjectorProvider aip = new FullAlisaInjectorProvider();
		Injector injector = aip.getInjector();

		PropertiesActivator.getInstance();

//		Injector injector = PropertiesActivator.getInstance()
//				.getInjector(PropertiesActivator.ORG_OSATE_XTEXT_AADL2_PROPERTIES_PROPERTIES);
//		OsateCorePlugin.getDefault()
//				.registerInjectorFor(PropertiesActivator.ORG_OSATE_XTEXT_AADL2_PROPERTIES_PROPERTIES, injector);
//		final ResourceDescriptionsProvider resourceDescProvider = injector
//				.getInstance(ResourceDescriptionsProvider.class);
//		injector.getAllBindings().keySet().forEach(it -> System.out.println(it.toString()));

		injector.injectMembers(this);
		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);

		if (resourceSet != null) {
			resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
			final Map<?, ?> args = context.getArguments();
			final String[] appArgs = (String[]) args.get("application.args");
			for (final String arg : appArgs) {
				System.out.println(arg);
			}
			String sys = null;
			File r = new File(appArgs[0]);
			ArrayList<String> l = new ArrayList<>();
			for (File f : r.listFiles()) {
				if (f.getName().endsWith(".aadl")) {
					if (f.getName().equals(appArgs[1])) {
						sys = readFile(f);
					} else {
						l.add(readFile(f));
					}
				}
			}

			Resource resource = loadFiles(appArgs[0], appArgs[1], resourceSet);
			if (resource != null) {
				AadlPackage ap = (AadlPackage) resource.getContents().get(0);
				SystemImplementation sysImpl = (SystemImplementation) getResourceByName(appArgs[2],
						ap.getOwnedPublicSection().getOwnedClassifiers());
				org.osate.aadl2.ComponentImplementation ci;
				SystemInstance instance = InstantiateModel.instantiate(sysImpl);
				String ir = Util.getAir(instance);
				Files.write(Paths.get(appArgs[3]), ir.getBytes());
				Aadl2Package.eINSTANCE.eClass();
				System.out.println("SUCCESS!! Wrote " + appArgs[3]);
			} else {
				System.out.println("Failed! Unable to load main package file");
			}
		} else {
			System.out.println("Failed! Unable to create resource set");
		}
		return IApplication.EXIT_OK;
	}

	String readFile(File f) throws IOException {
		return new String(Files.readAllBytes(Paths.get(f.toURI())));
	}

	Classifier getResourceByName(String name, EList<Classifier> l) {
		for (Classifier oc : l) {
			if (oc.getName().equals(name)) {
				return oc;
			}
		}
		return null;
	}

	public Resource loadFiles(String dirPath, String fileName, ResourceSet rs) {
		try {
			HashSet<String> files = new HashSet<String>(Files.list(Paths.get(dirPath)).map(java.nio.file.Path::toFile)
					.map(File::getName).filter(p -> p.endsWith(".aadl")).collect(Collectors.toList()));

			if (files.contains(fileName)) {
				files.remove(fileName);
			}

			try {
				PluginSupportUtil.getContributedAadl().stream().forEach(f -> rs.createResource(f));
			} catch (Exception e) {
				System.out.println(e);
				System.out.flush();
			}
			files.stream().forEach(f -> loadFile(dirPath + File.separator + f, rs));

			return loadFile(dirPath + File.separator + fileName, rs);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(e);
			e.printStackTrace();
		}
		return null;
	}

	private Resource loadFile(String filePath, ResourceSet rs) {
		try {
			// This way of constructing the URL works in JUnit plug-in and standalone tests
			URL url = new URL("file:" + filePath);
			InputStream stream = url.openConnection().getInputStream();
			Resource res = rs.createResource(URI.createURI(filePath));
			if (res != null) {
				res.load(stream, Collections.EMPTY_MAP);
			}
			return res;
		} catch (IOException e) {
			System.out.println("Unable to load file: " + filePath);
			return null;
		}
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
