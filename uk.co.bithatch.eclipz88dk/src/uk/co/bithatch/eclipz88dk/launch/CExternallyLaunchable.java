package uk.co.bithatch.eclipz88dk.launch;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.eclipz88dk.toolchain.Z88DKBuildContext;
import uk.co.bithatch.emuzx.DefaultPreparationContext;
import uk.co.bithatch.emuzx.api.IExternallyLaunchable;
import uk.co.bithatch.emuzx.api.IProgramBuildOptionsFactory;
import uk.co.bithatch.emuzx.debug.DezogDebugTarget;
import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTarget;

public class CExternallyLaunchable implements IExternallyLaunchable {
	
	public final static ILog LOG = ILog.of(CExternallyLaunchable.class);

	@Override
	public IDebugTarget createRemoteDebugTarget(ILaunchConfiguration configuration, ILaunch launch,
			DefaultPreparationContext prepCtx, IProcess eclipseProcess) throws CoreException {
		return new DezogDebugTarget(launch, configuration, eclipseProcess);
	}

	@Override
	public void compileForLaunch(String mode, DefaultPreparationContext prepCtx, IProgressMonitor monitor) throws CoreException {
		var file = prepCtx.programFile();
		var project = file.getProject();
		var sourceFile = file.getLocation().toFile();	
		var fmt = prepCtx.outputFormat();
		
		if (FileNames.hasExtensions(sourceFile, "c", "asm")) {
			if (project != null) {
				
				prepCtx.buildOptions(IProgramBuildOptionsFactory.accumulate(file));
				
				
				/* Get the CDT managed build info for the project */
				IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
				if (buildInfo == null) {
					throw new CoreException(Status.error("No CDT managed build information found for project: " + project.getName()));
				}
				
				IConfiguration buildCfg = buildInfo.getDefaultConfiguration();
				if (buildCfg == null) {
					throw new CoreException(Status.error("No default build configuration found for project: " + project.getName()));
				}
				
				/* Determine the output directory and file from the build configuration.
				 * The artifact name/extension may have been customised in 
				 * C/C++ Build -> Settings -> Build Artifact */
				var buildDir = new File(project.getLocation().toFile(), buildCfg.getName());
				var artifactName = ManagedBuildManager.getBuildMacroProvider()
						.resolveValueToMakefileFormat(buildCfg.getArtifactName(), "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, buildCfg);
				var outputFile = new File(buildDir, artifactName + "." + fmt.extension().toLowerCase());

				/* Invoke the CDT build system (incremental build), passing the
				 * required output format via thread-local so Z88DKCmdLineGen
				 * can pick it up */
				LOG.info("Building project '" + project.getName() + "' with configuration '" + buildCfg.getName() + "' for format " + fmt.name());
				Z88DKBuildContext.set(fmt);
				try {
					project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
				} finally {
					Z88DKBuildContext.clear();
					var buildFolder = project.getFolder(buildCfg.getName());
					if (buildFolder.exists()) {
						buildFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
					}
				}
				
				/* Set output file for launch. */
				prepCtx.binaryFile(outputFile);
				
			}
		}
	}

	@Override
	public IOutputFormat getOutputFormat(IProject prj) {
		return Z88DKPreferencesAccess.get().getOutputFormat(prj);
	}

	@Override
	public IDebugTarget createDefaultDebugTarget(ILaunch launch, DefaultPreparationContext prepCtx,
			IProcess eclipseProcess) {
		return new ExternalEmulatorDebugTarget(launch, eclipseProcess);
	}

	@Override
	public IArchitecture getArchitecture(IProject proj) {
		return Z88DKPreferencesAccess.get().getArchitecture(proj);
	}

	@Override
	public Path getBinFile(Path srcfile, Path outputFolder, IOutputFormat outputFormat) {
		return FileNames.changeExtension(outputFolder.resolve(srcfile.getFileName().toString()),
				outputFormat.extension().toLowerCase());
	}

	

}