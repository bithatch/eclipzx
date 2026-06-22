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
 * <p>
 * Since CDT always passes make automatic variables ({@code $<}, {@code $@})
 * rather than real filenames, we use GNU Make's {@code $(filter)} function
 * to conditionally emit the {@code --c-code-in-asm} pass only for {@code .c}
 * files. This is cross-platform (evaluated by make, not the shell).
 */
public class Z88DKCmdLineGen extends ManagedCommandLineGenerator {

	private static final ILog LOG = ILog.of(Z88DKCmdLineGen.class);

	/* (removed stale caching field — we now detect C sources from the output name) */

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

		/* CDT always passes make automatic variables ($< and $@) as input
		 * and output, so we cannot detect C vs ASM sources here. Instead,
		 * for debug builds we emit a shell 'case' conditional that only runs
		 * the --c-code-in-asm pass when the input file ends in .c.
		 * Make expands $< to the real filename before the shell sees it. */
		String rawInput = (inputResources != null && inputResources.length > 0) ? inputResources[0] : "";
		final String input = normalizePathSeparators(rawInput);
		final String normalizedOutput = normalizePathSeparators(output);

		/* Check if this is a Debug configuration */
		boolean debugCfg = false;
		var ri2 = tool.getParentResourceInfo();
		if (ri2 != null) {
			var cfg2 = ri2.getParent();
			debugCfg = cfg2 != null && cfg2.getName().toLowerCase().contains("debug");
			LOG.info("Z88DK Compiler: config name='" + (cfg2 != null ? cfg2.getName() : "null") + "', isDebug=" + debugCfg);
		} else {
			LOG.info("Z88DK Compiler: parentResourceInfo is null, assuming release");
		}
		final boolean isDebug = debugCfg;

		/*
		 * CDT uses getCommandLine() from the returned IManagedCommandLineInfo
		 * to construct the makefile recipe.
		 *
		 * For Debug builds we need to run an extra --c-code-in-asm pass for
		 * .c files (but not .asm files). Since CDT always passes $< and $@
		 * (make automatic variables) we cannot detect the source type in Java.
		 *
		 * We use GNU Make's $(filter) function to conditionally prepend the
		 * extra pass. $(filter) is evaluated by make itself (cross-platform,
		 * no shell dependency), and $< is expanded before the filter runs:
		 *
		 *   $(if $(filter %.c,$<),zcc --c-code-in-asm FLAGS --assemble-only $< &&) zcc FLAGS -c -o $@ $<
		 *
		 * For .c files: the filter matches, so the --c-code-in-asm pass runs
		 *   first, then the normal -c compile.
		 * For .asm files: the filter doesn't match, so only the normal compile
		 *   runs (the $(if) expands to nothing).
		 *
		 * For Release builds: zcc FLAGS -c -o $@ $<
		 */
		final String mergedFlags = String.join(" ", merged);

		return new IManagedCommandLineInfo() {
			@Override public String getCommandLine() {
				if (isDebug) {
					return "$(if $(filter %.c," + input + "),"
							+ cmd + " --c-code-in-asm " + mergedFlags
							+ " --assemble-only " + input
							+ " && " + moveCommand() + " " + moveArg(input + ".asm") + " . &&) "
							+ cmd + " " + mergedFlags + " -c "
							+ outputFlag + " " + normalizedOutput + " " + input;
				} else {
					return cmd + " " + mergedFlags + " -c "
							+ outputFlag + " " + normalizedOutput + " " + input;
				}
			}
			@Override public String getCommandLinePattern() { return commandLinePattern; }
			@Override public String getCommandName() { return cmd; }
			@Override public String getFlags() { return mergedFlags + " -c"; }
			@Override public String getOutputFlag() { return outputFlag; }
			@Override public String getOutputPrefix() { return outputPrefix; }
			@Override public String getOutput() { return normalizedOutput; }
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

	/**
	 * Return the platform-appropriate move command: {@code move} on Windows,
	 * {@code mv} elsewhere.
	 */
	private static String moveCommand() {
		return isWindows() ? "move" : "mv";
	}

	/**
	 * Wrap a path argument for the move command. On Windows, {@code move}
	 * does not accept forward slashes, so we use GNU Make's {@code $(subst)}
	 * to convert them to backslashes at make-expansion time.
	 */
	private static String moveArg(String path) {
		return isWindows() ? "$(subst /,\\," + path + ")" : path;
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}

	private static String normalizePathSeparators(String path) {
		if (path == null || path.isEmpty()) {
			return path;
		}
		char other = File.separatorChar == '/' ? '\\' : '/';
		return path.replace(other, File.separatorChar);
	}
}