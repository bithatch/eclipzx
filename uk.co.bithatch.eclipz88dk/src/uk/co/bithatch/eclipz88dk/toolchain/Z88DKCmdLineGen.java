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
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
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
	private static final String UK_CO_BITHATCH_ECLIPZ88DK_LINKER = "uk.co.bithatch.eclipz88dk.linker";

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

		var pax = Z88DKPreferencesAccess.get();
		var sdk = pax.getSDK(project).get();
		
		command = new File(new File(sdk.location(), "bin"), command).getAbsolutePath();

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

		var result = super.generateCommandLineInfo(
		        tool,
		        command,
		        merged.toArray(String[]::new),
		        outputFlag,
		        outputPrefix,
		        output,
		        inputResources,
		        commandLinePattern);

		/* When using --assemble-only, zcc leaves intermediate .c.asm files
		 * next to the original .c sources.  Append a cleanup command to move
		 * them into the build output directory so they don't pollute the
		 * source tree and don't get picked up as source files on subsequent
		 * builds. */
		if (merged.contains("--assemble-only") && project != null && inputResources != null) {
			var cmdLine = result.getCommandLine();
			var sb = new StringBuilder(cmdLine);
			for (String inp : inputResources) {
				if (inp.toLowerCase().endsWith(".c")) {
					/* The .c.asm is created next to the .c file */
					String casmPath = inp + ".asm";
					sb.append(" ; mv -f ").append(casmPath).append(" . 2>/dev/null");
				}
			}
			var finalCmd = sb.toString();
			/* Return a wrapper that overrides getCommandLine() */
			var orig = result;
			result = new IManagedCommandLineInfo() {
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

		return result;
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
