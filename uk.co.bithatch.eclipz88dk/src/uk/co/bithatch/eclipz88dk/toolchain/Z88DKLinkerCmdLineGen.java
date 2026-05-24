package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedCommandLineGenerator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;

/**
 * Linker command line generator. CDT invokes this once with all {@code .o}
 * files collected from the mirrored build directory structure as inputs.
 * <p>
 * When a {@link Z88DKBuildContext} is set (launch build), the linker emits
 * {@code -create-app -subtype=<fmt>} to produce the final distributable
 * binary (e.g. {@code .nex}, {@code .tap}). Otherwise it produces a plain
 * {@code .bin}.
 * <p>
 * Linker-specific options ({@code -pragma-include:}, {@code -startup=})
 * are read from the linker tool's options and emitted here.
 * 
 * 
 * TODO more options from https://www.z88dk.org/wiki/doku.php?id=platform:zx
 */
public class Z88DKLinkerCmdLineGen extends ManagedCommandLineGenerator {

	private static final ILog LOG = ILog.of(Z88DKLinkerCmdLineGen.class);

	private static final String OPT_PRAGMA_INCLUDE = "uk.co.bithatch.eclipz88dk.linker.pragmainclude";
	private static final String OPT_STARTUP = "uk.co.bithatch.eclipz88dk.linker.startup";

	@Override
	public IManagedCommandLineInfo generateCommandLineInfo(
			ITool tool,
			String command,
			String[] flags,
			String outputFlag,
			String outputPrefix,
			String output,
			String[] inputResources,
			String commandLinePattern) {

		var project = Z88DKCmdLineGen.projectFromTool(tool);

		var merged = new ArrayList<String>();
		if (flags != null)
			merged.addAll(Arrays.asList(flags));

		/* Strip linker options from flags — we re-add them ourselves */
		stripLinkerOptions(tool, merged);

		var pax = Z88DKPreferencesAccess.get();
		var sdk = pax.getSDK(project).get();

		command = new File(new File(sdk.location(), "bin"), command).getAbsolutePath();
		final String cmd = command;

		LOG.info("Z88DK Linker: tool.getId()=" + tool.getId()
				+ ", output=" + output
				+ ", inputs=" + (inputResources != null ? Arrays.toString(inputResources) : "null")
				+ ", buildContext=" + Z88DKBuildContext.get());

		/* Architecture and clib */
		merged.add("+" + pax.getArchitecture(project).name().toLowerCase());
		merged.add("-clib=" + pax.getCLibrary(project));

		/* Add library paths/libs from referenced projects */
		Z88DKCmdLineGen.addSettingsFromReferences(project, merged);

		/* Add library paths and libraries from the current project's
		 * language settings (configured via Paths and Symbols) */
		addProjectLibrarySettings(project, merged);

		/* Linker-specific options */
		addStringOption(tool, OPT_PRAGMA_INCLUDE, "-pragma-include:", merged);
		addStringOption(tool, OPT_STARTUP, "-startup=", merged);

		/* Debug build: add -m (map file) and -s (symbol file) for debugger support */
		var ri = tool.getParentResourceInfo();
		if (ri != null) {
			var cfg = ri.getParent();
			if (cfg != null && cfg.getName().toLowerCase().contains("debug")) {
				merged.add("-m");
				merged.add("-s");
				merged.add("--list");
			}
		}

		/* Build context: launch build vs normal build.
		 * Note: -o must always point to the .bin output — zcc -create-app
		 * produces the .sna/.tap/.nex alongside it automatically. */
		var buildCtx = Z88DKBuildContext.get();
		if (buildCtx.isPresent()) {
			merged.add("-create-app");
			merged.add("-subtype=" + buildCtx.get().name().toLowerCase());
		}
		String linkOutput = output;

		/* Build the command line manually so we can control quoting/globbing.
		 * CDT passes .o inputs from the mirrored directory structure. */
		var sb = new StringBuilder();
		sb.append(cmd);
		for (String f : merged) {
			sb.append(' ').append(f);
		}
		sb.append(' ').append(outputFlag).append(' ').append(linkOutput);

		/* Add all .o inputs */
		if (inputResources != null) {
			for (String inp : inputResources) {
				sb.append(' ').append(inp);
			}
		}

		var finalCmd = sb.toString();
		var flagStr = String.join(" ", merged);
		var inputStr = (inputResources != null) ? String.join(" ", inputResources) : "";

		return new IManagedCommandLineInfo() {
			@Override public String getCommandLine() { return finalCmd; }
			@Override public String getCommandLinePattern() { return commandLinePattern; }
			@Override public String getCommandName() { return cmd; }
			@Override public String getFlags() { return flagStr; }
			@Override public String getOutputFlag() { return outputFlag; }
			@Override public String getOutputPrefix() { return outputPrefix; }
			@Override public String getOutput() { return linkOutput; }
			@Override public String getInputs() { return inputStr; }
		};
	}

	private static final String[][] LINKER_OPTION_PREFIXES = {
		{ OPT_PRAGMA_INCLUDE, "-pragma-include:" },
		{ OPT_STARTUP,        "-startup=" },
	};

	private static void stripLinkerOptions(ITool tool, java.util.List<String> flags) {
		for (String[] entry : LINKER_OPTION_PREFIXES) {
			String optId = entry[0];
			String prefix = entry[1];
			String val = Z88DKCmdLineGen.getStringOptionValue(tool, optId);
			if (val != null && !val.isBlank()) {
				flags.remove(prefix + val.trim());
				flags.remove(val.trim());
			}
		}
	}

	private static void addStringOption(ITool tool, String optionId, String prefix, java.util.List<String> flags) {
		String val = Z88DKCmdLineGen.getStringOptionValue(tool, optionId);
		if (val != null && !val.isBlank()) {
			flags.add(prefix + val.trim());
		}
	}

	/**
	 * Read library paths ({@code -L}) and library files ({@code -l}) from the
	 * current project's CDT language settings (Paths and Symbols → Libraries /
	 * Library Paths). These are stored as {@link ICSettingEntry} entries, not as
	 * tool options.
	 */
	private static void addProjectLibrarySettings(IProject project, java.util.List<String> flags) {
		try {
			ICProjectDescription projDesc = CoreModel.getDefault().getProjectDescription(project, false);
			if (projDesc == null) return;
			ICConfigurationDescription cfgDesc = projDesc.getActiveConfiguration();
			if (cfgDesc == null) return;
			ICFolderDescription root = cfgDesc.getRootFolderDescription();
			if (root == null) return;

			var projLocation = project.getLocation();

			for (ICLanguageSetting lang : root.getLanguageSettings()) {
				/* Library paths (-L) */
				for (ICLanguageSettingEntry se : lang.getResolvedSettingEntries(ICSettingEntry.LIBRARY_PATH)) {
					String value = se.getValue();
					if (value == null || value.isEmpty()) continue;
					java.io.File resolved;
					if (new java.io.File(value).isAbsolute()) {
						resolved = new java.io.File(value);
					} else {
						resolved = new java.io.File(projLocation.toFile(), value);
					}
					String flag = "-L" + resolved.getAbsolutePath();
					if (!flags.contains(flag)) {
						LOG.info("Z88DK Linker: adding -L from project settings: " + resolved.getAbsolutePath());
						flags.add(flag);
					}
				}
				/* Library files (-l) */
				for (ICLanguageSettingEntry se : lang.getResolvedSettingEntries(ICSettingEntry.LIBRARY_FILE)) {
					String value = se.getValue();
					if (value == null || value.isEmpty()) continue;
					String flag = "-l" + value;
					if (!flags.contains(flag)) {
						LOG.info("Z88DK Linker: adding -l from project settings: " + value);
						flags.add(flag);
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Z88DK Linker: failed to read project library settings for: " + project.getName(), e);
		}
	}
}
