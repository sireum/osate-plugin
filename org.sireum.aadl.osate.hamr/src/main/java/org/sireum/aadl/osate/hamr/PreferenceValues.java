package org.sireum.aadl.osate.hamr;

import java.io.File;
import java.util.Optional;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;

public class PreferenceValues {

	public static final String PLUGIN_ID = "HAMR";

	public static final BoolOption HAMR_DEVICES_AS_THREADS_OPT = new BoolOption(//
			"HAMR_DEVICES_AS_THREADS_OPT", //
			"Treat AADL devices as threads", //
			Optional.of("Devices will be ignored if not checked"), //
			false);

	public static final BoolOption HAMR_EMBED_ART_OPT = new BoolOption(//
			"HAMR_EMBED_ART_OPT", //
			"Embed ART", //
			Optional.of(
					"If checked then ART will be downloaded, otherwise the ART source will be placed in an 'art' directory"), //
			true);

	public static final BoolOption HAMR_RUN_TRANSPILER_OPT = new BoolOption(//
			"HAMR_RUN_TRANSPILER_OPT", //
			"Run Transpiler", //
			Optional.of("Automatically run transpiler for Slang projects being taken down to C/CAmkES"), //
			true);

	public static final BoolOption HAMR_SERIALIZE_AIR_OPT = new BoolOption(//
			"HAMR_SERIALIZE_AIR_OPT", //
			"Serialize AIR to JSON (non-compact) when generating code", //
			Optional.empty(), //
			true);

	public static final StringOption HAMR_AIR_OUTPUT_FOLDER_OPT = new StringOption(//
			"HAMR_AIR_OUTPUT_FOLDER_OPT", //
			"AIR output folder", //
			Optional.of("Directory where serialized AIR model will be stored"), //
			".slang");

	public static final BoolOption HAMR_VERBOSE_OPT = new BoolOption(//
			"HAMR_VERBOSE_OPT", //
			"Verbose Output", //
			Optional.empty(), //
			true);

	public static final BoolOption HAMR_RUN_PROYEK_IVE_OPT = new BoolOption(//
			"HAMR_RUN_PROYEK_IVE_OPT", //
			"Generate IVE project", //
			Optional.of("Generate IVE project using Proyek IVE"), //
			true);

	public static final BoolOption HAMR_PROOF_GENERATE = new BoolOption(//
			"HAMR_PROOF_GENERATE", //
			"Generate Information Flow Preservation Proof", //
			Optional.of("Generate Information Flow Preservation Proof"), //
			false);

	public static final BoolOption HAMR_PROOF_CHECK = new BoolOption(//
			"HAMR_PROOF_CHECK", //
			"Check Information Flow Preservation Proof", //
			Optional.of("Check Information Flow Preservation Proof"), //
			false);

	public static String sireumCVC4() {
		String shome = System.getProperty("org.sireum.home");
		if (shome != null && (new File(shome).exists())) {

			String os = "";
			String ext = "";
			if(org.sireum.Os.isLinux()) {
				os = "linux";
			} else if(org.sireum.Os.isWin()) {
				os = "win";
				ext = ".exe";
			} else if (org.sireum.Os.isMac()) {
				os = "mac";
			} else {
				// unsupported OS
				return "";
			}

			File f = new File(shome, "bin/" + os + "/cvc4" + ext);

			if (f.exists() && f.isFile() && f.canExecute()) {
				return f.getAbsolutePath();
			}
		}
		return "";
	}

	public static final PathOption HAMR_SMT2_PATH = new PathOption(//
			"HAMR_SMT2_PATH",
			"SMT2 Solver",
			Optional.of("Location of SMT2 solver executable"),
			sireumCVC4()
	);

	public static final StringOption HAMR_SMT2_OPTIONS = new StringOption(//
			"HAMR_SMT2_OPTIONS", //
			"Solver Options", //
			Optional.of("Options to pass to the SMT2 solver"), //
			"--incremental --finite-model-find");

	public enum Generators {
		HAMR_GENERATOR
	}

	public static abstract class OsateOption {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		String key;
		String name;
		Optional<String> tooltip;
	}

	public static class PathOption extends OsateOption {
		final String defaultValue;

		public PathOption(String key, String name, Optional<String> tooltip, String defaultValue) {
			this.key = key;
			this.name = name;
			this.tooltip = tooltip;
			this.defaultValue = defaultValue;

			store.setDefault(key, defaultValue);
		}

		public File getValue() {
			File f = new File(store.getString(key));
			if (f.exists() && f.isFile() && f.canExecute()) {
				return f;
			} else {
				return null;
			}
		}

		public FileFieldEditor getEditor(Composite parent) {
			FileFieldEditor ret = new FileFieldEditor(key, name, parent);
			if (this.tooltip.isPresent()) {
				ret.getTextControl(parent).setToolTipText(this.tooltip.get());
				ret.getLabelControl(parent).setToolTipText(this.tooltip.get());
			}
			return ret;
		}
	}

	public static class BoolOption extends OsateOption {
		final boolean defaultValue;

		public BoolOption(String key, String name, Optional<String> tooltip, boolean defaultValue) {
			this.key = key;
			this.name = name;
			this.tooltip = tooltip;
			this.defaultValue = defaultValue;

			store.setDefault(key, defaultValue);
		}

		public boolean getValue() {
			return store.getBoolean(key);
		}

		public BooleanFieldEditor getEditor(Composite parent) {
			BooleanFieldEditor ret = new BooleanFieldEditor(key, name, parent);
			if (this.tooltip.isPresent()) {
				ret.getDescriptionControl(parent).setToolTipText(this.tooltip.get());
			}
			return ret;
		}
	}

	public static class StringOption extends OsateOption {
		final String defaultValue;

		public StringOption(String key, String name, Optional<String> tooltip, String defaultValue) {
			this.key = key;
			this.name = name;
			this.tooltip = tooltip;
			this.defaultValue = defaultValue;

			store.setDefault(key, this.defaultValue);
		}

		public String getValue() {
			return store.getString(key);
		}

		public StringFieldEditor getEditor(Composite parent) {
			StringFieldEditor ret = new StringFieldEditor(key, name, parent);
			if (this.tooltip.isPresent()) {
				ret.getTextControl(parent).setToolTipText(this.tooltip.get());
				ret.getLabelControl(parent).setToolTipText(this.tooltip.get());
			}
			return ret;
		}
	}
}

