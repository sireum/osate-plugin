package org.sireum.aadl.osate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

public class MenuContributions extends CompoundContributionItem {

	@Override
	protected IContributionItem[] getContributionItems() {
		List<IContributionItem> l = new ArrayList<IContributionItem>();

		l.add(getItem("Serialize Slang AST to file", "org.sireum.commands.launchsireum",
				map("org.sireum.commands.launchsireum.generator", "serialize"), true));

		l.add(getItem("Generate Slang Embedded Code", "org.sireum.commands.launchsireum",
				map("org.sireum.commands.launchsireum.generator", "genslang"),
				classExists("org.sireum.aadl.arsit.Runner")));

		l.add(getItem("Generate AWAS Code", "org.sireum.commands.genawas",
				map("org.sireum.commands.genawas.generator", "json"),
				classExists("org.sireum.awas.AADLBridge.AadlHandler")));

		return l.toArray(new IContributionItem[0]);
	}

	private IContributionItem getItem(String label, String commandId, Map<String, String> params, boolean enabled) {

		return new CommandContributionItem(
				new CommandContributionItemParameter(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow(), // serviceLocator - non-null
						null, // id
						commandId, // commandId
						params, // parameters
						null, // icon
						null, // disabledIcon
						null, // hoverIcon
						label, // label
						null, // mnemonic
						null, // tooltip
						CommandContributionItem.STYLE_PUSH, // style,
						null, // helpContextId,
						false // visibleEnabled
				)) {

			@Override
			public boolean isEnabled() {
				return enabled;
			}
		};
	}

	private Map<String, String> map(String... args) {
		assert (args.length % 2 == 0);
		Map<String, String> m = new HashMap<String, String>();
		for (int i = 0; i < args.length; i += 2) {
			m.put(args[i], args[i + 1]);
		}
		return m;
	}

	private boolean classExists(String className) {
		try {
			Class<?> c = Class.forName(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
