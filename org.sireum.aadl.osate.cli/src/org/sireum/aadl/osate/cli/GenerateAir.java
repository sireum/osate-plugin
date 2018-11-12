package org.sireum.aadl.osate.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
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
import org.osate.alisa.workbench.AlisaStandaloneSetup;
import org.osate.categories.CategoriesStandaloneSetup;
import org.osate.xtext.aadl2.Aadl2StandaloneSetupGenerated;
import org.osate.xtext.aadl2.errormodel.ErrorModelStandaloneSetup;
import org.sireum.aadl.osate.util.TestUtil;

import com.google.inject.Injector;
import com.rockwellcollins.atc.resolute.ResoluteStandaloneSetup;

public class GenerateAir implements IApplication {

	@SuppressWarnings("restriction")
	@Override
	public Object start(IApplicationContext context) throws Exception {

		Injector injector = new Aadl2StandaloneSetupGenerated().createInjectorAndDoEMFRegistration();

		ErrorModelStandaloneSetup.doSetup();
		CategoriesStandaloneSetup.doSetup();
		AlisaStandaloneSetup.doSetup();
		ResoluteStandaloneSetup.doSetup();


//		Aadl2InjectorProvider aip = new Aadl2InjectorProvider();
//		aip.setupRegistry();
//		Injector injector = aip.getInjector();

		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		// System.out.println("Tetsing :" + resourceSet);
		if (resourceSet != null) {
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
			Aadl2Package.eINSTANCE.eClass();
			// testHelper = new TestHelper<AadlPackage>();
			// Injector injector = Guice.createInjector();

			final Map<?, ?> args = context.getArguments();
			final String[] appArgs = (String[]) args.get("application.args");
			for (final String arg : appArgs) {
				System.out.println(arg);
			}

			Resource resource = loadFiles(appArgs[0], appArgs[1], resourceSet);
			if (resource != null) {
				AadlPackage ap = (AadlPackage) resource.getContents().get(0);
				SystemImplementation sysImpl = (SystemImplementation) getResourceByName(appArgs[2],
						ap.getOwnedPublicSection().getOwnedClassifiers());
				SystemInstance instance = InstantiateModel.instantiate(sysImpl);
				String ir = TestUtil.getAir(instance);
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
			HashSet<String> files = new HashSet<String>(Files.list(Paths.get(dirPath)).map(Path::toFile)
					.map(File::getName).filter(p -> p.endsWith(".aadl")).collect(Collectors.toList()));

			if (files.contains(fileName)) {
				files.remove(fileName);
			}
			files.stream().forEach(f -> System.out.println(dirPath + f));
			System.out.flush();
			files.stream().forEach(f -> loadFile(dirPath + File.separator + f, rs));

			return loadFile(dirPath + File.separator + fileName, rs);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Resource loadFile(String filePath, ResourceSet rs) {
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
			return null;
		}
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
