package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedCommandLineGenerator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILog;

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
		}
		
		return super.generateCommandLineInfo(
		        tool,
		        command,
		        merged.toArray(String[]::new),
		        outputFlag,
		        outputPrefix,
		        output,
		        inputResources,
		        commandLinePattern);
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
