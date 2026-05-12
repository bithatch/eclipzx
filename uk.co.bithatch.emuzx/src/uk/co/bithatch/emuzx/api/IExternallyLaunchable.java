package uk.co.bithatch.emuzx.api;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.emuzx.DefaultPreparationContext;

public interface IExternallyLaunchable {
	
	IDebugTarget createRemoteDebugTarget(ILaunchConfiguration configuration, ILaunch launch,
			DefaultPreparationContext prepCtx, IProcess eclipseProcess) throws CoreException;

	void compileForLaunch(String mode, DefaultPreparationContext prepCtx) throws CoreException;

	IOutputFormat getOutputFormat(IProject prj);

	IDebugTarget createDefaultDebugTarget(ILaunch launch, DefaultPreparationContext prepCtx,
			IProcess eclipseProcess);

	IArchitecture getArchitecture(IProject proj);
}
