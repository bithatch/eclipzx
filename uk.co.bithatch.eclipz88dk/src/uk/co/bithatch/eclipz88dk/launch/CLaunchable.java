package uk.co.bithatch.eclipz88dk.launch;

import java.nio.file.Path;
import java.util.function.Predicate;

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
import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.eclipz88dk.toolchain.Z88DKBuildContext;
import uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes;
import uk.co.bithatch.emuzx.api.IExternallyLaunchable;
import uk.co.bithatch.emuzx.api.IInternallyLaunchable;
import uk.co.bithatch.emuzx.api.IProgramBuildOptionsFactory;
import uk.co.bithatch.emuzx.api.IWritablePreparationContext;
import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTarget;
import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTargetRegistry;

public class CLaunchable implements IExternallyLaunchable, IInternallyLaunchable {
	
	public final static ILog LOG = ILog.of(CLaunchable.class);

	@Override
	public IDebugTarget createRemoteDebugTarget(ILaunchConfiguration configuration, ILaunch launch,
			IWritablePreparationContext prepCtx, IProcess eclipseProcess) throws CoreException {

		var debugger = configuration.getAttribute(DebugLaunchConfigurationAttributes.DEBUGGER, "");
		if(debugger.equals("")) {
			LOG.warn("No debugger specified, using default emulator debug target.");
			return new ExternalEmulatorDebugTarget(launch, eclipseProcess);
		}

		var binaryFile = prepCtx.launchFile();
		var binaryPath = binaryFile != null ? binaryFile : null;
		var debugInfo = LanguageSystem.languageSystem(prepCtx.programFile()).createSourceAddressMap(binaryPath);
		
		return ExternalEmulatorDebugTargetRegistry.get(debugger).createDebugTargetFactory().createDebugTarget(launch, configuration, eclipseProcess, debugInfo);
	}

	@Override
	public void compileForLaunch(String mode, IWritablePreparationContext prepCtx, IProgressMonitor monitor, Predicate<IOutputFormat> formatFilter) throws CoreException {
		var file = prepCtx.programFile();
		var project = file.getProject();
		var sourceFile = file.getLocation().toFile();	
		var fmt = prepCtx.outputFormat();
		
		if (FileNames.hasExtensions(sourceFile, "c", "asm")) {
			if (project != null) {
				
				prepCtx.buildOptions(IProgramBuildOptionsFactory.accumulate(file));
				
				IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
				if (buildInfo == null) {
					throw new CoreException(Status.error("No CDT managed build information found for project: " + project.getName()));
				}
				
				IConfiguration buildCfg = buildInfo.getDefaultConfiguration();
				if (buildCfg == null) {
					throw new CoreException(Status.error("No default build configuration found for project: " + project.getName()));
				}
				
				var buildDir = project.getLocation().toPath().resolve(buildCfg.getName());
				var artifactName = ManagedBuildManager.getBuildMacroProvider()
						.resolveValueToMakefileFormat(buildCfg.getArtifactName(), "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, buildCfg);
				var outputFile = buildDir.resolve(artifactName + "." + fmt.extension().toLowerCase());

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
				
				prepCtx.launchFile(outputFile);
			}
		}
	}

	@Override
	public IOutputFormat getOutputFormat(IProject prj) {
		return Z88DKPreferencesAccess.get().getOutputFormat(prj);
	}

	@Override
	public IDebugTarget createDefaultDebugTarget(ILaunch launch, IWritablePreparationContext prepCtx,
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