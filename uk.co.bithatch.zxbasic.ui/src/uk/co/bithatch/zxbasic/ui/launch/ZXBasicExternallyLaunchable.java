package uk.co.bithatch.zxbasic.ui.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.emuzx.DefaultPreparationContext;
import uk.co.bithatch.emuzx.api.IExternallyLaunchable;
import uk.co.bithatch.emuzx.debug.DezogDebugTarget;
import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTarget;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicBuilder;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class ZXBasicExternallyLaunchable implements IExternallyLaunchable {

	@Override
	public IDebugTarget createRemoteDebugTarget(ILaunchConfiguration configuration, ILaunch launch,
			DefaultPreparationContext prepCtx, IProcess eclipseProcess) throws CoreException {
		return new DezogDebugTarget(launch, configuration, eclipseProcess);
	}

	@Override
	public void compileForLaunch(String mode, DefaultPreparationContext prepCtx) throws CoreException {
		ZXBasicBuilder.compileForLaunch(prepCtx, mode, ZXBasicBuilder.DEFAULT_REPORTER);
	}

	@Override
	public IOutputFormat getOutputFormat(IProject prj) {
		return ZXBasicPreferencesAccess.get().getOutputFormat(prj);
	}

	@Override
	public IDebugTarget createDefaultDebugTarget(ILaunch launch, DefaultPreparationContext prepCtx,
			IProcess eclipseProcess) {
		return new ExternalEmulatorDebugTarget(launch, eclipseProcess);
	}

	@Override
	public IArchitecture getArchitecture(IProject proj) {
		return ZXBasicPreferencesAccess.get().getArchitecture(proj);
	}

	

}
