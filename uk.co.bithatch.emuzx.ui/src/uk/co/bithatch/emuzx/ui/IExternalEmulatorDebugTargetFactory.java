package uk.co.bithatch.emuzx.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;

import uk.co.bithatch.bitzx.ISourceAdressMap;

public interface IExternalEmulatorDebugTargetFactory {

	IExternalEmulatorDebugTarget createDebugTarget(ILaunch launch, ILaunchConfiguration configuration,
			IProcess emulatorProcess, ISourceAdressMap map) throws CoreException;
}
