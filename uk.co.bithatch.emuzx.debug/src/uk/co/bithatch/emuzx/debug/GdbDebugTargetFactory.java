package uk.co.bithatch.emuzx.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;

import uk.co.bithatch.bitzx.ISourceAdressMap;
import uk.co.bithatch.emuzx.ui.IExternalEmulatorDebugTarget;
import uk.co.bithatch.emuzx.ui.IExternalEmulatorDebugTargetFactory;

public class GdbDebugTargetFactory implements IExternalEmulatorDebugTargetFactory {

	@Override
	public IExternalEmulatorDebugTarget createDebugTarget(ILaunch launch, ILaunchConfiguration configuration, IProcess emulatorProcess, ISourceAdressMap map) throws CoreException {
		return new GdbDebugTarget(launch,  configuration, emulatorProcess, map);
	}

}
