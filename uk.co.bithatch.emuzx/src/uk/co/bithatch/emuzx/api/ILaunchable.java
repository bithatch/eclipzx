package uk.co.bithatch.emuzx.api;

import static uk.co.bithatch.emuzx.IEmulatorLaunchConfigurationAttributes.OUTPUT_FORMAT;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.IOutputFormat;

public interface ILaunchable {

	IDebugTarget createRemoteDebugTarget(ILaunchConfiguration configuration, ILaunch launch,
			IWritablePreparationContext prepCtx, IProcess eclipseProcess) throws CoreException;

	void compileForLaunch(String mode, IWritablePreparationContext prepCtx, IProgressMonitor monitor, Predicate<IOutputFormat> formatFilter) throws CoreException;

	IOutputFormat getOutputFormat(IProject prj);
	
	default IOutputFormat getLaunchFormat(ILaunchConfiguration configuration, IFile file) throws CoreException {
		return getLaunchFormat(configuration, file, f -> true);
	}
	
	default List<? extends IOutputFormat> getLaunchFormats(IFile file, Predicate<IOutputFormat> filter) {
		var arch = getArchitecture(file.getProject());
		if(arch != null) {
			return arch.supportedFormats().stream().filter(f -> filter.test(f)).toList();
		}
		return Collections.emptyList();		
	}
	
	default IOutputFormat getLaunchFormat(ILaunchConfiguration configuration, IFile file, Predicate<IOutputFormat> filter) throws CoreException {
		var fmt = configuration.getAttribute(OUTPUT_FORMAT, "");
		var formats = getLaunchFormats(file, filter);
		var sel = formats.stream().filter(f -> f.name().equalsIgnoreCase(fmt)).findFirst();
		if(sel.isPresent()) {
			return sel.get();
		}
		else {
			return formats.stream().findFirst().orElseThrow(() -> new CoreException(Status.error("No support launch formats.")));			
		}

	}

	IDebugTarget createDefaultDebugTarget(ILaunch launch, IWritablePreparationContext prepCtx,
			IProcess eclipseProcess);

	IArchitecture getArchitecture(IProject proj);

	default Path getBinFile(Path srcfile, Path outputFolder, IOutputFormat outputFormat) {
		return outputFolder.resolve(FileNames.changeExtension(srcfile, outputFormat.extension()));
	}
}
