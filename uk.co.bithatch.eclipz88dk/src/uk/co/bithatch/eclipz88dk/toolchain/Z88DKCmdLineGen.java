package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedCommandLineGenerator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;

import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;

/**
 * Per-file compiler command line generator. CDT invokes this once per source
 * file (.c or .asm) with the correct input and output paths mirroring the
 * source layout in the build directory (e.g. {@code Debug/src/main.o}).
 * <p>
 * For {@code .c} files, a compound command is emitted:
 * <ol>
 *   <li>{@code zcc --assemble-only ...flags... input.c} — produces {@code input.c.asm}</li>
 *   <li>{@code zcc -c -o output.o ...flags... input.c} — produces the {@code .o}</li>
 * </ol>
 * For {@code .asm} files (user-written assembly, not {@code .c.asm}):
 * <ul>
 *   <li>{@code zcc -c -o output.o ...flags... input.asm}</li>
 * </ul>
 * Bank switching flags ({@code --codeseg}, {@code --constseg}, {@code --dataseg})
 * are read from the per-resource configuration and added to the per-file command.
 */
public class Z88DKCmdLineGen extends ManagedCommandLineGenerator {

	private static final ILog LOG = ILog.of(Z88DKCmdLineGen.class);
	private static final String UK_CO_BITHATCH_ECLIPZ88DK_COMPILER = "uk.co.bithatch.eclipz88dk.compiler";

	private static final String OPT_CODESEG = "uk.co.bithatch.eclipz88dk.compiler.codeseg";
	private static final String OPT_CONSTSEG = "uk.co.bithatch.eclipz88dk.compiler.constseg";
	private static final String OPT_DATASEG = "uk.co.bithatch.eclipz88dk.compiler.dataseg";

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

		var project = projectFromTool(tool);

		var merged = new ArrayList<String>();
		if (flags != null)
			merged.addAll(Arrays.asList(flags));

		/* Strip bank switching options from flags — we re-add them ourselves
		 * from the per-resource configuration */
		stripProgrammaticOptions(tool, merged);

		var pax = Z88DKPreferencesAccess.get();
		var sdk = pax.getSDK(project).get();

		command = new File(new File(sdk.location(), "bin"), command).getAbsolutePath();
		final String cmd = command;

		LOG.info("Z88DK Compiler: tool.getId()=" + tool.getId()
				+ ", output=" + output
				+ ", inputs=" + (inputResources != null ? Arrays.toString(inputResources) : "null"));

		/* Add architecture and clib flags */
		merged.add("+" + pax.getArchitecture(project).name().toLowerCase());
		merged.add("-clib=" + pax.getCLibrary(project));

		/* Add include paths from referenced projects */
		addSettingsFromReferences(project, merged);

		/* Add bank switching flags from per-resource config */
		addBankSwitchingFlags(tool, merged);

		/* Determine input file */
		String input = (inputResources != null && inputResources.length > 0) ? inputResources[0] : "";
		boolean isCSource = input.toLowerCase().endsWith(".c");

		/*
		 * CDT's makefile generator constructs the recipe from
		 * ${COMMAND} ${FLAGS} ${OUTPUT_FLAG}${OUTPUT_PREFIX}${OUTPUT} ${INPUTS}
		 * using the individual getters, NOT getCommandLine(). For .c files we
		 * need a compound command:
		 *   zcc ... --assemble-only INPUT && zcc ... -c -o OUTPUT INPUT
		 *
		 * We embed the first pass + "&&" + second command into FLAGS so CDT
		 * generates the correct Make recipe. The input variable ($< in pattern
		 * rules) is appended by CDT as ${INPUTS} at the end.
		 */
		final String flagStr;
		if (isCSource) {
			var sb = new StringBuilder();
			/* First pass flags: --assemble-only with the input, then chain */
			for (String f : merged) {
				sb.append(f).append(' ');
			}
			sb.append("--assemble-only ").append(input);
			sb.append(" && ").append(cmd);
			/* Second pass flags: -c */
			for (String f : merged) {
				sb.append(' ').append(f);
			}
			sb.append(" -c");
			flagStr = sb.toString();
		} else {
			/* .asm file: single -c pass */
			var sb = new StringBuilder();
			for (String f : merged) {
				sb.append(f).append(' ');
			}
			sb.append("-c");
			flagStr = sb.toString();
		}

		return new IManagedCommandLineInfo() {
			@Override public String getCommandLine() {
				/* Build the full command explicitly for contexts that DO use it */
				if (isCSource) {
					return cmd + " " + String.join(" ", merged) + " --assemble-only " + input
							+ " && " + cmd + " " + String.join(" ", merged) + " -c "
							+ outputFlag + " " + output + " " + input;
				} else {
					return cmd + " " + String.join(" ", merged) + " -c "
							+ outputFlag + " " + output + " " + input;
				}
			}
			@Override public String getCommandLinePattern() { return commandLinePattern; }
			@Override public String getCommandName() { return cmd; }
			@Override public String getFlags() { return flagStr; }
			@Override public String getOutputFlag() { return outputFlag; }
			@Override public String getOutputPrefix() { return outputPrefix; }
			@Override public String getOutput() { return output; }
			@Override public String getInputs() { return input; }
		};
	}

	/**
	 * Add bank switching flags (--codeseg/--constseg/--dataseg) from the
	 * tool's per-resource configuration. CDT calls us per-file, so the tool
	 * already has the correct per-file option values.
	 */
	private static void addBankSwitchingFlags(ITool tool, List<String> flags) {
		addStringOption(tool, OPT_CODESEG, "--codeseg", flags);
		addStringOption(tool, OPT_CONSTSEG, "--constseg", flags);
		addStringOption(tool, OPT_DATASEG, "--dataseg", flags);
	}

	/**
	 * Remove option values that CDT injected into the flags list for options
	 * we handle programmatically.
	 */
	private static final String[][] PROGRAMMATIC_OPTION_PREFIXES = {
		{ OPT_CODESEG,    "--codeseg" },
		{ OPT_CONSTSEG,   "--constseg" },
		{ OPT_DATASEG,    "--dataseg" },
	};

	private static void stripProgrammaticOptions(ITool tool, List<String> flags) {
		for (String[] entry : PROGRAMMATIC_OPTION_PREFIXES) {
			String optId = entry[0];
			String prefix = entry[1];
			String val = getStringOptionValue(tool, optId);
			if (val != null && !val.isBlank()) {
				flags.remove(prefix + val.trim());
				flags.remove(val.trim());
			}
		}
	}

	/**
	 * Read a string option value from a tool, returning null if not found or empty.
	 */
	static String getStringOptionValue(ITool tool, String optionId) {
		try {
			IOption opt = tool.getOptionBySuperClassId(optionId);
			if (opt == null) opt = tool.getOptionById(optionId);
			if (opt != null) {
				return opt.getStringValue();
			}
		} catch (Exception e) {
			LOG.warn("Z88DK: failed to read option " + optionId, e);
		}
		return null;
	}

	/**
	 * Read a string option from the tool and add it to the flags with the given
	 * prefix. Does nothing if the option is empty or not found.
	 */
	private static void addStringOption(ITool tool, String optionId, String prefix, List<String> flags) {
		String val = getStringOptionValue(tool, optionId);
		if (val != null && !val.isBlank()) {
			flags.add(prefix + val.trim());
		}
	}

	/**
	 * Collect include paths, library paths and library names from referenced
	 * projects (configured via C/C++ General → Paths and Symbols → References).
	 */
	static void addSettingsFromReferences(IProject project, List<String> flags) {
		try {
			ICProjectDescription projDesc = CoreModel.getDefault().getProjectDescription(project, false);
			if (projDesc == null) return;

			ICConfigurationDescription cfgDesc = projDesc.getActiveConfiguration();
			if (cfgDesc == null) return;

			var refMap = cfgDesc.getReferenceInfo();
			LOG.info("Z88DK: referenced projects for '" + project.getName() + "': " + refMap);

			for (var entry : refMap.entrySet()) {
				String refProjName = entry.getKey();
				IProject refProject = project.getWorkspace().getRoot().getProject(refProjName);
				if (refProject == null || !refProject.isAccessible()) {
					LOG.warn("Z88DK: referenced project '" + refProjName + "' not accessible");
					continue;
				}

				var refLocation = refProject.getLocation();
				var refDir = refLocation.toFile();

				/* 1. CDT language settings (managed-build projects) */
				ICProjectDescription refProjDesc = CoreModel.getDefault().getProjectDescription(refProject, false);
				if (refProjDesc != null) {
					String refCfgId = entry.getValue();
					ICConfigurationDescription refCfgDesc = null;
					if (refCfgId != null && !refCfgId.isEmpty()) {
						refCfgDesc = refProjDesc.getConfigurationById(refCfgId);
					}
					if (refCfgDesc == null) {
						refCfgDesc = refProjDesc.getActiveConfiguration();
					}
					if (refCfgDesc != null) {
						ICFolderDescription refRoot = refCfgDesc.getRootFolderDescription();
						if (refRoot != null) {
							for (ICLanguageSetting lang : refRoot.getLanguageSettings()) {
								collectPaths(lang, ICSettingEntry.INCLUDE_PATH, "-I", refLocation, refProjName, flags);
								collectPaths(lang, ICSettingEntry.LIBRARY_PATH, "-L", refLocation, refProjName, flags);
								collectPaths(lang, ICSettingEntry.LIBRARY_FILE, "-l", refLocation, refProjName, flags);
							}
						}
					}
				}

				/* 2. Conventional directories */
				addIfDir(new java.io.File(refDir, "include"), "-I", refProjName, flags);

				var clib = Z88DKPreferencesAccess.get().getCLibrary(project);
				var clibSubdir = clibToLibSubdir(clib);
				if (clibSubdir != null) {
					addIfDir(new java.io.File(new java.io.File(refDir, "lib"), clibSubdir), "-L", refProjName, flags);
				}
				addIfDir(new java.io.File(refDir, "lib"), "-L", refProjName, flags);
			}
		} catch (Exception e) {
			LOG.error("Failed to resolve settings from referenced projects for: " + project.getName(), e);
		}
	}

	private static String clibToLibSubdir(String clib) {
		if (clib == null) return null;
		return switch (clib.toLowerCase()) {
			case "new"      -> "sccz80";
			case "sdcc_ix"  -> "sdcc_ix";
			case "sdcc_iy"  -> "sdcc_iy";
			case "classic"  -> "sccz80";
			default         -> clib.toLowerCase();
		};
	}

	private static void addIfDir(java.io.File dir, String prefix, String refProjName, List<String> flags) {
		if (dir.isDirectory()) {
			String flag = prefix + dir.getAbsolutePath();
			if (!flags.contains(flag)) {
				LOG.info("Z88DK: adding " + prefix + " from '" + refProjName + "' (conventional dir): " + dir.getAbsolutePath());
				flags.add(flag);
			}
		}
	}

	private static void collectPaths(ICLanguageSetting lang, int kind, String prefix,
			IPath refLocation, String refProjName, List<String> flags) {
		for (ICLanguageSettingEntry se : lang.getResolvedSettingEntries(kind)) {
			String value = se.getValue();
			if (value == null || value.isEmpty()) continue;

			if (kind == ICSettingEntry.LIBRARY_FILE) {
				String flag = prefix + value;
				if (!flags.contains(flag)) {
					LOG.info("Z88DK: adding " + prefix + " from '" + refProjName + "': " + value);
					flags.add(flag);
				}
				continue;
			}

			java.io.File resolved;
			if (new java.io.File(value).isAbsolute()) {
				resolved = new java.io.File(value);
			} else {
				resolved = new java.io.File(refLocation.toFile(), value);
			}

			if (!resolved.isDirectory()) {
				LOG.info("Z88DK: skipping non-existent path from '" + refProjName + "': " + resolved);
				continue;
			}

			String absPath = resolved.getAbsolutePath();
			String flag = prefix + absPath;
			if (!flags.contains(flag)) {
				LOG.info("Z88DK: adding " + prefix + " from '" + refProjName + "': " + absPath);
				flags.add(flag);
			}
		}
	}

	public static IProject projectFromTool(ITool tool) {
		if (tool == null) return null;

		IResourceInfo ri = tool.getParentResourceInfo();
		if (ri != null) {
			IConfiguration cfg = ri.getParent();
			IManagedProject mp = (cfg != null) ? cfg.getManagedProject() : null;
			return (mp != null) ? (IProject) mp.getOwner() : null;
		}

		IBuildObject p = tool.getParent();
		IConfiguration cfg = null;

		if (p instanceof IConfiguration) {
			cfg = (IConfiguration) p;
		} else if (p instanceof IToolChain) {
			cfg = ((IToolChain) p).getParent();
		} else if (p instanceof IResourceInfo) {
			cfg = ((IResourceInfo) p).getParent();
		}

		IManagedProject mp = (cfg != null) ? cfg.getManagedProject() : null;
		return (mp != null) ? (IProject) mp.getOwner() : null;
	}
}
