package uk.co.bithatch.emuzx.emulator.zesarux;

import java.nio.file.Path;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;

import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTarget;

public class ZRPCDebugTarget extends ExternalEmulatorDebugTarget {

	public ZRPCDebugTarget(ILaunch launch, ILaunchConfiguration configuration,
			IProcess emulatorProcess, Path binaryPath) throws CoreException {
		super(launch, emulatorProcess);
	}

}
