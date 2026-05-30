package uk.co.bithatch.zxbasic.ui.launch;

import java.nio.file.Path;

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
import uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes;
import uk.co.bithatch.emuzx.api.IExternallyLaunchable;
import uk.co.bithatch.emuzx.api.IInternallyLaunchable;
import uk.co.bithatch.emuzx.api.IWritablePreparationContext;
import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTarget;
import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTargetRegistry;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicBuilder;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicOutputFormat;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;
import uk.co.bithatch.zxbasic.ui.tools.ZXBC;

public class ZXBasicLaunchable implements IExternallyLaunchable, IInternallyLaunchable {

	public final static ILog LOG = ILog.of(ZXBasicLaunchable.class);

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
		ZXBasicBuilder.compileForLaunch(prepCtx, mode, ZXBasicBuilder.DEFAULT_REPORTER);
	}

	@Override
	public IOutputFormat getOutputFormat(IProject prj) {
		return ZXBasicPreferencesAccess.get().getOutputFormat(prj);
	}

	@Override
	public IDebugTarget createDefaultDebugTarget(ILaunch launch, IWritablePreparationContext prepCtx,
			IProcess eclipseProcess) {
		return new ExternalEmulatorDebugTarget(launch, eclipseProcess);
	}

	@Override
	public IArchitecture getArchitecture(IProject proj) {
		return ZXBasicPreferencesAccess.get().getArchitecture(proj);
	}

	@Override
	public Path getBinFile(Path srcfile, Path outputFolder, IOutputFormat outputFormat) {
		return ZXBC.targetFile(srcfile.toFile(), outputFolder.toFile(), (BorielZXBasicOutputFormat) outputFormat)
				.toPath();
	}

}
