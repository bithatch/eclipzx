package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFileInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedCommandLineGenerator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;

import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;

public class Z88DKCmdLineGen extends ManagedCommandLineGenerator{

	private static final ILog LOG = ILog.of(Z88DKCmdLineGen.class);
	private static final String UK_CO_BITHATCH_ECLIPZ88DK_COMPILER = "uk.co.bithatch.eclipz88dk.compiler";

	private static final String OPT_CODESEG = "uk.co.bithatch.eclipz88dk.compiler.codeseg";
	private	static final String OPT_CONSTSEG = "uk.co.bithatch.eclipz88dk.compiler.constseg";
	private static final String OPT_DATASEG = "uk.co.bithatch.eclipz88dk.compiler.dataseg";
	private static final String OPT_PRAGMA_INCLUDE = "uk.co.bithatch.eclipz88dk.compiler.pragmainclude";
	private static final String OPT_STARTUP = "uk.co.bithatch.eclipz88dk.compiler.startup";

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

		/* CDT injects option values into flags[] using the 'command' prefix.
		 * We handle bank switching (codeseg/constseg/dataseg) per-file and
		 * linker options (pragma-include/startup) only during the link phase,
		 * so strip all of them from flags[] — we re-add them ourselves at
		 * the right point. */
		stripProgrammaticOptions(tool, merged);

		var pax = Z88DKPreferencesAccess.get();
		var sdk = pax.getSDK(project).get();
		
		command = new File(new File(sdk.location(), "bin"), command).getAbsolutePath();
		final String cmd = command;

		LOG.info("Z88DK generateCommandLineInfo: tool.getId()=" + tool.getId() + ", buildContext=" + Z88DKBuildContext.get());
		
		if(tool.getId().startsWith(UK_CO_BITHATCH_ECLIPZ88DK_COMPILER + ".") 
				|| tool.getId().equals(UK_CO_BITHATCH_ECLIPZ88DK_COMPILER)) {
			var buildCtx = Z88DKBuildContext.get();
			if (buildCtx.isEmpty()) {
				/* Normal build: compile/assemble only */
				merged.add("--assemble-only");
			} else {
				/* Launch build: produce final binary in the requested format */
				merged.add("-create-app");
				merged.add("-subtype=" + buildCtx.get().name().toLowerCase());
				
				/* Linker options — only emitted during the link/launch build */
				addStringOption(tool, OPT_PRAGMA_INCLUDE, "-pragma-include:", merged);
				addStringOption(tool, OPT_STARTUP, "-startup=", merged);
			}
			merged.add("+" + pax.getArchitecture(project).name().toLowerCase());
			merged.add("-clib=" + pax.getCLibrary(project));
			
			/* Add include paths, library paths, and libraries from CDT settings
			 * (including those inherited from referenced projects via 
			 * C/C++ General -> Paths and Symbols -> References) */
			addSettingsFromReferences(project, merged);
		}
		
		/* If CDT didn't pass any input sources (happens when sources are in
		 * subdirectories because CDT's per-directory makefile generation doesn't
		 * feed them back to the target tool), discover them from the project's
		 * configured source entries. */
		if ((inputResources == null || inputResources.length == 0) && project != null) {
			inputResources = discoverSources(project);
			if (inputResources.length > 0) {
				LOG.info("Z88DK: discovered " + inputResources.length + " source file(s) from project source entries");
			}
		}

		/*
		 * Bank switching support: scan per-resource configurations for files that
		 * have codeseg/constseg/dataseg options set.  Those files must be compiled
		 * separately from the main batch because each group needs different
		 * --codeseg/--constseg/--dataseg flags.
		 *
		 * This applies to both compile and link phases:
		 *  - Compile phase (--assemble-only): bank files compiled separately
		 *  - Link phase (-create-app): bank files compiled to .o with -c first,
		 *    then .o files fed into the final link alongside other sources
		 */
		var bankGroups = new LinkedHashMap<BankKey, List<String>>();
		var mainSources = new ArrayList<String>();
		boolean isLinkPhase = merged.contains("-create-app");
		boolean isCompilePhase = merged.contains("--assemble-only");
		
		if (inputResources != null && project != null && (isCompilePhase || isLinkPhase)) {
			var cfg = configFromTool(tool);
			if (cfg != null) {
				Map<String, BankKey> fileBanks = collectBankSettings(cfg);
				LOG.info("Z88DK: per-file bank settings: " + fileBanks);
				
				for (String src : inputResources) {
					/* src is like "../src/util.c" — strip leading "../" to get project-relative path */
					String projRel = src.startsWith("../") ? src.substring(3) : src;
					BankKey bank = fileBanks.get(projRel);
					if (bank != null) {
						bankGroups.computeIfAbsent(bank, k -> new ArrayList<>()).add(src);
					} else {
						mainSources.add(src);
					}
				}
			} else {
				mainSources.addAll(Arrays.asList(inputResources));
			}
		} else if (inputResources != null) {
			mainSources.addAll(Arrays.asList(inputResources));
		}

		/*
		 * For the link phase, all .o files already exist from the compile phase.
		 * Just use *.o to link them all.
		 */

		/* Build the compound command line */
		var sb = new StringBuilder();
		
		if (isLinkPhase) {
			/* Link phase: just link all .o files from the build directory.
			 * The compile phase already produced them.
			 * We build the command manually because:
			 *  - *.o must NOT be quoted (shell glob)
			 *  - output extension should match the subtype (e.g. .nex) */
			var buildCtx = Z88DKBuildContext.get();
			String ext = buildCtx.isPresent() ? buildCtx.get().name().toLowerCase() : "bin";
			String linkOutput = output.replaceAll("\\.[^.]+$", "." + ext);
			
			sb.append(command);
			for (String f : merged) {
				sb.append(' ').append(f);
			}
			sb.append(' ').append(outputFlag).append(' ').append(linkOutput);
			sb.append(" *.o");
			
			var finalCmd = sb.toString();
			return new IManagedCommandLineInfo() {
				@Override public String getCommandLine() { return finalCmd; }
				@Override public String getCommandLinePattern() { return commandLinePattern; }
				@Override public String getCommandName() { return cmd; }
				@Override public String getFlags() { return String.join(" ", merged); }
				@Override public String getOutputFlag() { return outputFlag; }
				@Override public String getOutputPrefix() { return outputPrefix; }
				@Override public String getOutput() { return linkOutput; }
				@Override public String getInputs() { return "*.o"; }
			};
		}
		
		/* Compile phase: bank-switched files compiled separately */
		for (var entry : bankGroups.entrySet()) {
			var bank = entry.getKey();
			var srcs = entry.getValue();
			
			var bankFlags = new ArrayList<>(merged);
			if (bank.codeseg != null && !bank.codeseg.isBlank())
				bankFlags.add("--codeseg" + bank.codeseg.trim());
			if (bank.constseg != null && !bank.constseg.isBlank())
				bankFlags.add("--constseg" + bank.constseg.trim());
			if (bank.dataseg != null && !bank.dataseg.isBlank())
				bankFlags.add("--dataseg" + bank.dataseg.trim());
			
			/* First: --assemble-only pass (produces .c.asm intermediates) */
			sb.append(command);
			for (String f : bankFlags) {
				sb.append(' ').append(f);
			}
			for (String src : srcs) {
				sb.append(' ').append(src);
			}
			
			/* Cleanup .c.asm for bank-switched files */
			for (String src : srcs) {
				if (src.toLowerCase().endsWith(".c")) {
					sb.append(" ; mv -f ").append(src).append(".asm . 2>/dev/null");
				}
			}
			
			/* Second: -c pass (produces .o files in build dir for linking) */
			var bankCompileFlags = new ArrayList<>(bankFlags);
			bankCompileFlags.remove("--assemble-only");
			bankCompileFlags.add(0, "-c");
			for (String src : srcs) {
				String baseName = uniqueOName(src, srcs);
				sb.append(" && ");
				sb.append(command);
				for (String f : bankCompileFlags) {
					sb.append(' ').append(f);
				}
				sb.append(" -o ").append(baseName).append(' ').append(src);
			}
			sb.append(" && ");
		}
		
		/* Main compilation (non-bank-switched files) */
		var mainResult = super.generateCommandLineInfo(
		        tool, command, merged.toArray(String[]::new),
		        outputFlag, outputPrefix, output,
		        mainSources.toArray(String[]::new), commandLinePattern);

		sb.append(mainResult.getCommandLine());
		
		/* Cleanup .c.asm files for main sources */
		if (isCompilePhase && project != null) {
			for (String inp : mainSources) {
				if (inp.toLowerCase().endsWith(".c")) {
					sb.append(" ; mv -f ").append(inp).append(".asm . 2>/dev/null");
				}
			}
			
			/* Also compile main sources to .o in build dir for the link phase */
			var compileFlags = new ArrayList<>(merged);
			compileFlags.remove("--assemble-only");
			compileFlags.add(0, "-c");
			for (String src : mainSources) {
				String baseName = uniqueOName(src, mainSources);
				sb.append(" && ").append(command);
				for (String f : compileFlags) {
					sb.append(' ').append(f);
				}
				sb.append(" -o ").append(baseName).append(' ').append(src);
			}
		}

		var finalCmd = sb.toString();
		var orig = mainResult;
		return new IManagedCommandLineInfo() {
			@Override public String getCommandLine() { return finalCmd; }
			@Override public String getCommandLinePattern() { return orig.getCommandLinePattern(); }
			@Override public String getCommandName() { return orig.getCommandName(); }
			@Override public String getFlags() { return orig.getFlags(); }
			@Override public String getOutputFlag() { return orig.getOutputFlag(); }
			@Override public String getOutputPrefix() { return orig.getOutputPrefix(); }
			@Override public String getOutput() { return orig.getOutput(); }
			@Override public String getInputs() { return orig.getInputs(); }
		};
	}

	/**
	 * Compute a unique .o filename for a source file within a group of sources.
	 * Normally "foo.c" → "foo.o", but if there's also a "foo.asm" in the same
	 * group, the .asm file becomes "foo_asm.o" to avoid collision.
	 */
	private static String uniqueOName(String src, List<String> allSources) {
		String fileName = src.substring(src.lastIndexOf('/') + 1);
		String baseName = fileName.replaceAll("\\.[^.]+$", "");
		String ext = "";
		int dot = fileName.lastIndexOf('.');
		if (dot >= 0) ext = fileName.substring(dot + 1).toLowerCase();
		
		/* Check if another file in the group has the same base name but different extension */
		boolean conflict = false;
		for (String other : allSources) {
			if (other.equals(src)) continue;
			String otherFile = other.substring(other.lastIndexOf('/') + 1);
			String otherBase = otherFile.replaceAll("\\.[^.]+$", "");
			if (otherBase.equals(baseName)) {
				conflict = true;
				break;
			}
		}
		
		if (conflict) {
			return baseName + "_" + ext + ".o";
		}
		return baseName + ".o";
	}

	/**
	 * Key for grouping files by their bank switching settings.
	 */
	private record BankKey(String codeseg, String constseg, String dataseg) {}
	
	/**
	 * Scan all per-resource (file-level) configurations in the given CDT
	 * configuration and collect bank switching option values.
	 * 
	 * @return map from project-relative file path to BankKey
	 */
	private static Map<String, BankKey> collectBankSettings(IConfiguration cfg) {
		var result = new LinkedHashMap<String, BankKey>();
		for (IResourceInfo ri : cfg.getResourceInfos()) {
			if (ri instanceof IFileInfo fileInfo) {
				/* Get the project-relative path of this file resource */
				var path = fileInfo.getPath();
				if (path == null || path.isEmpty()) continue;
				/* path typically looks like "src/util.c" */
				String projRelPath = path.toString();
				/* Remove leading slash if present */
				if (projRelPath.startsWith("/")) projRelPath = projRelPath.substring(1);
				
				/* Find the compiler tool in this file's resource info */
				for (ITool fileTool : fileInfo.getTools()) {
					if (fileTool.getId().startsWith(UK_CO_BITHATCH_ECLIPZ88DK_COMPILER + ".")
							|| fileTool.getId().equals(UK_CO_BITHATCH_ECLIPZ88DK_COMPILER)) {
						String codeseg = getStringOptionValue(fileTool, OPT_CODESEG);
						String constseg = getStringOptionValue(fileTool, OPT_CONSTSEG);
						String dataseg = getStringOptionValue(fileTool, OPT_DATASEG);
						
						if ((codeseg != null && !codeseg.isBlank())
								|| (constseg != null && !constseg.isBlank())
								|| (dataseg != null && !dataseg.isBlank())) {
							result.put(projRelPath, new BankKey(codeseg, constseg, dataseg));
							LOG.info("Z88DK: bank settings for '" + projRelPath + "': codeseg=" + codeseg 
									+ " constseg=" + constseg + " dataseg=" + dataseg);
						}
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Remove option values that CDT injected into the flags list for options
	 * we handle programmatically.  CDT emits {@code command + value} as a
	 * single token (e.g. {@code --codesegPAGE_36}, {@code -pragma-include:zpragma.inc}).
	 * We strip these because we re-add them ourselves at the correct phase
	 * (bank switching per-file, linker options only during link).
	 */
	private static final String[][] PROGRAMMATIC_OPTION_PREFIXES = {
		{ OPT_CODESEG,         "--codeseg" },
		{ OPT_CONSTSEG,        "--constseg" },
		{ OPT_DATASEG,         "--dataseg" },
		{ OPT_PRAGMA_INCLUDE,  "-pragma-include:" },
		{ OPT_STARTUP,         "-startup=" },
	};
	
	private static void stripProgrammaticOptions(ITool tool, List<String> flags) {
		for (String[] entry : PROGRAMMATIC_OPTION_PREFIXES) {
			String optId = entry[0];
			String prefix = entry[1];
			String val = getStringOptionValue(tool, optId);
			if (val != null && !val.isBlank()) {
				flags.remove(prefix + val.trim());
				/* Also remove bare value in case CDT emitted it without prefix
				 * (can happen if plugin.xml was cached without command attr) */
				flags.remove(val.trim());
			}
		}
	}

	/**
	 * Read a string option value from a tool, returning null if not found or empty.
	 */
	private static String getStringOptionValue(ITool tool, String optionId) {
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
	 * prefix (e.g. "-pragma-include:" or "-startup="). Does nothing if the
	 * option is empty or not found.
	 */
	private static void addStringOption(ITool tool, String optionId, String prefix, List<String> flags) {
		String val = getStringOptionValue(tool, optionId);
		if (val != null && !val.isBlank()) {
			flags.add(prefix + val.trim());
		}
	}

	/**
	 * Get the IConfiguration from a tool instance.
	 */
	private static IConfiguration configFromTool(ITool tool) {
		IResourceInfo ri = tool.getParentResourceInfo();
		if (ri != null) return ri.getParent();
		
		IBuildObject p = tool.getParent();
		if (p instanceof IConfiguration cfg) return cfg;
		if (p instanceof IToolChain tc) return tc.getParent();
		if (p instanceof IResourceInfo ri2) return ri2.getParent();
		return null;
	}

	/**
	 * Collect include paths, library paths and library names from referenced
	 * projects (configured via C/C++ General → Paths and Symbols → References).
	 * 
	 * We use a two-pronged approach:
	 * <ol>
	 *   <li>Query the referenced project's CDT language settings for
	 *       include/library entries (works well for managed-build projects).</li>
	 *   <li>Check for conventional {@code include/} and {@code lib/}
	 *       directories in the referenced project root (a reliable fallback
	 *       for makefile projects whose CDT settings may be stale or
	 *       incomplete).</li>
	 * </ol>
	 * All paths are validated against the local filesystem before being added.
	 */
	private static void addSettingsFromReferences(IProject project, List<String> flags) {
		try {
			ICProjectDescription projDesc = CoreModel.getDefault().getProjectDescription(project, false);
			if (projDesc == null) return;
			
			ICConfigurationDescription cfgDesc = projDesc.getActiveConfiguration();
			if (cfgDesc == null) return;
			
			/* Get projects referenced by this configuration */
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
				
				/* 2. Conventional directories — check for include/ and lib/
				 *    in the referenced project root.  This is the reliable
				 *    fallback for makefile projects. */
				addIfDir(new File(refDir, "include"), "-I", refProjName, flags);
				
				/* For library paths, check the clib-specific subdirectory first
				 * (e.g. lib/sccz80 for "new", lib/sdcc_ix for "sdcc_ix"), then
				 * fall back to lib/ itself. */
				var clib = Z88DKPreferencesAccess.get().getCLibrary(project);
				var clibSubdir = clibToLibSubdir(clib);
				if (clibSubdir != null) {
					addIfDir(new File(new File(refDir, "lib"), clibSubdir), "-L", refProjName, flags);
				}
				addIfDir(new File(refDir, "lib"), "-L", refProjName, flags);
			}
		} catch (Exception e) {
			LOG.error("Failed to resolve settings from referenced projects for: " + project.getName(), e);
		}
	}
	
	/**
	 * Map a Z88DK C library name to the conventional subdirectory under
	 * {@code lib/} where compiled libraries are placed.
	 * 
	 * @return the subdirectory name, or {@code null} if no mapping is known
	 */
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

	/** Add a flag for a directory if it exists and isn't already in the list. */
	private static void addIfDir(File dir, String prefix, String refProjName, List<String> flags) {
		if (dir.isDirectory()) {
			String flag = prefix + dir.getAbsolutePath();
			if (!flags.contains(flag)) {
				LOG.info("Z88DK: adding " + prefix + " from '" + refProjName + "' (conventional dir): " + dir.getAbsolutePath());
				flags.add(flag);
			}
		}
	}
	
	/**
	 * Collect path entries of a given kind from a language setting, resolve
	 * them to absolute paths, and add them to the flags list — but only if
	 * the path actually exists on the local filesystem.
	 */
	private static void collectPaths(ICLanguageSetting lang, int kind, String prefix,
			IPath refLocation, String refProjName, List<String> flags) {
		for (ICLanguageSettingEntry se : lang.getResolvedSettingEntries(kind)) {
			String value = se.getValue();
			if (value == null || value.isEmpty()) continue;
			
			/* For library files (-l), just pass the name without path validation */
			if (kind == ICSettingEntry.LIBRARY_FILE) {
				String flag = prefix + value;
				if (!flags.contains(flag)) {
					LOG.info("Z88DK: adding " + prefix + " from '" + refProjName + "': " + value);
					flags.add(flag);
				}
				continue;
			}
			
			/* Resolve relative paths against the referenced project root */
			File resolved;
			if (new File(value).isAbsolute()) {
				resolved = new File(value);
			} else {
				resolved = new File(refLocation.toFile(), value);
			}
			
			/* Only add paths that actually exist on this system — filters out
			 * stale Windows paths from .cproject files, SDK paths already
			 * handled elsewhere, etc. */
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

	/**
	 * Discover C and ASM source files from the project's CDT source entries.
	 * Returns workspace-relative or project-relative paths suitable for the
	 * command line.
	 */
	private static String[] discoverSources(IProject project) {
		List<String> sources = new ArrayList<>();
		try {
			ICProjectDescription projDesc = CoreModel.getDefault().getProjectDescription(project, false);
			if (projDesc == null) return new String[0];
			
			ICConfigurationDescription cfgDesc = projDesc.getActiveConfiguration();
			if (cfgDesc == null) return new String[0];
			
			ICSourceEntry[] sourceEntries = cfgDesc.getSourceEntries();
			
			project.accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource.getType() == IResource.FILE) {
						String ext = resource.getFileExtension();
						if (ext != null 
								&& (ext.equalsIgnoreCase("c") || ext.equalsIgnoreCase("asm"))
								&& !resource.getName().endsWith(".c.asm")) {
							/* Check this file is within a configured source entry
							 * and not excluded */
							if (CDataUtil.isExcluded(resource.getFullPath(), sourceEntries)) {
								return false;
							}
							/* Prepend "../" because CDT runs the build from the
							 * output directory (e.g. Debug/), not the project root */
							IPath relPath = resource.getProjectRelativePath();
							sources.add("../" + relPath.toOSString());
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			LOG.error("Failed to discover source files for project: " + project.getName(), e);
		}
		
		return sources.toArray(String[]::new);
	}

  public static IProject projectFromTool(ITool tool) {
	    if (tool == null) return null;

	    IResourceInfo ri = tool.getParentResourceInfo();
	    if (ri != null) {
	      IConfiguration cfg = ri.getParent();
	      IManagedProject mp = (cfg != null) ? cfg.getManagedProject() : null;
	      return (mp != null) ? (IProject) mp.getOwner() : null;
	    }

	    // Fallback: walk up from the build object parent
	    IBuildObject p = tool.getParent();
	    IConfiguration cfg = null;

	    if (p instanceof IConfiguration) {
	      cfg = (IConfiguration) p;
	    } else if (p instanceof IToolChain) {
	      cfg = ((IToolChain) p).getParent();                  // -> IConfiguration
	    } else if (p instanceof IResourceInfo) {
	      cfg = ((IResourceInfo) p).getParent();               // -> IConfiguration
	    }

	    IManagedProject mp = (cfg != null) ? cfg.getManagedProject() : null;
	    return (mp != null) ? (IProject) mp.getOwner() : null;            // IProject
	  }
}
