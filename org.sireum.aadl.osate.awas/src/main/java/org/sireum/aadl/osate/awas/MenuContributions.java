package org.sireum.aadl.osate.awas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.sireum.aadl.osate.awas.PreferenceValues.Generators;

public class MenuContributions extends CompoundContributionItem {

	@Override
	protected IContributionItem[] getContributionItems() {
		List<IContributionItem> l = new ArrayList<IContributionItem>();

		l.add(getItem("Generate AWAS Visualizer", "org.sireum.commands.launchawas",
				map("org.sireum.commands.launchawas.generator", Generators.GEN_AWAS.toString())));

		l.add(getItem("Generate Risk Analysis Report",
				"org.sireum.commands.launchriskanalysis",
				map("org.sireum.commands.launchriskanalysis.generator1", Generators.GEN_AWAS.toString())));

		l.add(getItem("Forward Reachability", "org.sireum.commands.forward",
				map("org.sireum.commands.forward.generator", Generators.GEN_AWAS.toString())));

		l.add(getItem("Backward Reachability", "org.sireum.commands.backward",
				map("org.sireum.commands.backward.generator", Generators.GEN_AWAS.toString())));

		l.add(getItem("Erase Reachability", "org.sireum.commands.erase",
				map("org.sireum.commands.erase.generator", Generators.GEN_AWAS.toString())));

		return l.toArray(new IContributionItem[0]);
	}

	private IContributionItem getItem(String label, String commandId, Map<String, String> params) {

		return new CommandContributionItem(
				new CommandContributionItemParameter(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), // serviceLocator - non-null
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

}
