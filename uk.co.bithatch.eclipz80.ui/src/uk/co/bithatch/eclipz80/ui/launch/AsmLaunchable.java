package uk.co.bithatch.eclipz80.ui.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.eclipz80.ui.builder.AsmBuilder;
import uk.co.bithatch.eclipz80.ui.language.AsmArchitecture;
import uk.co.bithatch.eclipz80.ui.language.AsmOutputFormat;
import uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes;
import uk.co.bithatch.emuzx.api.IExternallyLaunchable;
import uk.co.bithatch.emuzx.api.IInternallyLaunchable;
import uk.co.bithatch.emuzx.api.IWritablePreparationContext;
import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTarget;
import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTargetRegistry;

public class AsmLaunchable implements IExternallyLaunchable, IInternallyLaunchable {

	public final static ILog LOG = ILog.of(AsmLaunchable.class);

	@Override
	public IDebugTarget createRemoteDebugTarget(ILaunchConfiguration configuration, ILaunch launch,
			IWritablePreparationContext prepCtx, IProcess eclipseProcess) throws CoreException {

		var debugger = configuration.getAttribute(DebugLaunchConfigurationAttributes.DEBUGGER, "");
		if (debugger.equals("")) {
			LOG.warn("No debugger specified, using default emulator debug target.");
			return new ExternalEmulatorDebugTarget(launch, eclipseProcess);
		}

		var binaryFile = prepCtx.launchFile();
		var binaryPath = binaryFile != null ? binaryFile : null;
		var debugInfo = LanguageSystem.languageSystem(prepCtx.programFile()).createSourceAddressMap(binaryPath);

		return ExternalEmulatorDebugTargetRegistry.get(debugger).createDebugTargetFactory().createDebugTarget(launch,
				configuration, eclipseProcess, debugInfo);
	}

	@Override
	public void compileForLaunch(String mode, IWritablePreparationContext prepCtx, IProgressMonitor monitor)
			throws CoreException {
		prepCtx.launchFile(AsmBuilder.prepareForGenericLaunch(prepCtx.programFile(), mode));
	}

	@Override
	public IOutputFormat getOutputFormat(IProject prj) {
		/* Only one at the moment */
		return AsmOutputFormat.BIN;
	}

	@Override
	public IDebugTarget createDefaultDebugTarget(ILaunch launch, IWritablePreparationContext prepCtx,
			IProcess eclipseProcess) {
		return new ExternalEmulatorDebugTarget(launch, eclipseProcess);
	}

	@Override
	public IArchitecture getArchitecture(IProject proj) {
		/* Only one at the moment */
		return AsmArchitecture.ZX;
	}

}
