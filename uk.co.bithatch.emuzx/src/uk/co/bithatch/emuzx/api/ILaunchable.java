package uk.co.bithatch.emuzx.api;

import static uk.co.bithatch.emuzx.IEmulatorLaunchConfigurationAttributes.OUTPUT_FORMAT;

import java.nio.file.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.LanguageSystem;

public interface ILaunchable {

	
	IDebugTarget createRemoteDebugTarget(ILaunchConfiguration configuration, ILaunch launch,
			IWritablePreparationContext prepCtx, IProcess eclipseProcess) throws CoreException;

	void compileForLaunch(String mode, IWritablePreparationContext prepCtx, IProgressMonitor monitor) throws CoreException;

	IOutputFormat getOutputFormat(IProject prj);
	
	default IOutputFormat getLaunchFormat(ILaunchConfiguration configuration, IFile file) throws CoreException {
		return LanguageSystem.outputFormatOrDefault(file.getProject(), configuration.getAttribute(OUTPUT_FORMAT, ""));
	}

	IDebugTarget createDefaultDebugTarget(ILaunch launch, IWritablePreparationContext prepCtx,
			IProcess eclipseProcess);

	IArchitecture getArchitecture(IProject proj);

	default Path getBinFile(Path srcfile, Path outputFolder, IOutputFormat outputFormat) {
		return outputFolder.resolve(FileNames.changeExtension(srcfile, outputFormat.extension()));
	}
}
