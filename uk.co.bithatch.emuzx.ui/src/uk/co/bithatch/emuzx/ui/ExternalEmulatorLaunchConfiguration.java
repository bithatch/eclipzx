package uk.co.bithatch.emuzx.ui;

import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CUSTOM_WORKING_DIRECTORY;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.EMULATOR_ARGS;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.EMULATOR_EXECUTABLE;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.WORKING_DIRECTORY_LOCATION;
import static uk.co.bithatch.emuzx.IEmulatorLaunchConfigurationAttributes.PROGRAM;
import static uk.co.bithatch.emuzx.IEmulatorLaunchConfigurationAttributes.PROJECT;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

import uk.co.bithatch.bitzx.LaunchContext;
import uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes;
import uk.co.bithatch.emuzx.api.IExternallyLaunchable;
import uk.co.bithatch.emuzx.api.IPreparationTarget;
import uk.co.bithatch.emuzx.api.IWritablePreparationContext;

public class ExternalEmulatorLaunchConfiguration extends AbstractPreparedLaunchConfigurationDelegate<IExternallyLaunchable> {
	private final static ILog LOG = ILog.of(ExternalEmulatorLaunchConfiguration.class);

	public ExternalEmulatorLaunchConfiguration() {
		super(PROJECT, PROGRAM, IExternallyLaunchable.class);
	}

	@Override
	protected void preparedLaunch(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor,
			Optional<IPreparationTarget> preparationTarget, String mode, IFile file, 
			IWritablePreparationContext prepCtx, 
			IExternallyLaunchable launchable,
			LaunchContext launchCtx) throws CoreException {
		/* Emulator */
		var strmgr = VariablesPlugin.getDefault().getStringVariableManager();
		var emulator = strmgr.performStringSubstitution(
				configuration.getAttribute(EMULATOR_EXECUTABLE, ""));

		/* Build up command line */
		var cmd = new ArrayList<String>();

		/* C# emulators on Posix, i.e. CSpect on Linux can't be run directly */
		/* TODO better exposed configuration of this */
		if (emulator.toLowerCase().endsWith(".exe")
				&& (Boolean.getBoolean("eclipzx.useFrontEndForExternalEmulators")
						|| (!Platform.getOS().equals("win32")
								&& !Boolean.getBoolean("eclipzx.dontUseFrontEndForExternalEmulators")))) {
			cmd.add(System.getProperty("eclipzx.externalEmulatorFrontEnd", "mono"));
		}
		cmd.add(emulator);

		/* Emulator arguments */
		cmd.addAll(configuration.getAttribute(EMULATOR_ARGS, Collections.emptyList()).stream().map(s -> {
			try {
				return strmgr.performStringSubstitution(s);
			} catch (CoreException e) {
				throw new IllegalStateException(e);
			}
		}).toList());

		/* Debug args */
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			cmd.addAll(configuration
					.getAttribute(DebugLaunchConfigurationAttributes.DEBUGGER_EMULATOR_ARGS, Collections.emptyList())
					.stream().map(s -> {
						try {
							return strmgr.performStringSubstitution(s);
						} catch (CoreException e) {
							throw new IllegalStateException(e);
						}
					}).toList());
		}

		/* Build the process */
		var pb = new ProcessBuilder(cmd);

		/* Environment */
		var map = configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES,
				new HashMap<String, String>());
		if (!configuration.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true)) {
			pb.environment().clear();
			;
		}
		pb.environment().putAll(map);

		/* Working directory */
		var customWorkingDir = configuration.getAttribute(CUSTOM_WORKING_DIRECTORY, false);
		File workingDir;
		if (customWorkingDir) {
			workingDir = new File(strmgr.performStringSubstitution(
					configuration.getAttribute(WORKING_DIRECTORY_LOCATION, System.getProperty("user.dir"))));
		} else {
			workingDir = file.getProject().getLocation().toFile();
		}
		pb.directory(workingDir);

		/* Start the process */

		LOG.info(String.format("Emulator working directory: %s", workingDir));
		LOG.info(String.format("Emulator Launch Command: %s", String.join(" ", cmd)));

		Process process = null;
		try {
			process = pb.start();
		} catch (IOException e) {
			closeContexts(launchCtx, prepCtx);
			throw new CoreException(
					new Status(IStatus.ERROR, "uk.co.bithatch.zxbasic", "Failed to start emulator", e));
		}

		/* Wrap it as an IProcess so Eclipse can manage it */
		var eclipseProcess = DebugPlugin.newProcess(launch, process, "External Emulator");

		try {
			/* Listen for process termination to clean up preparation context */
			DebugPlugin.getDefault().addDebugEventListener(new IDebugEventSetListener() {
				@Override
				public void handleDebugEvents(DebugEvent[] events) {
					for (var event : events) {
						if (event.getKind() == DebugEvent.TERMINATE && event.getSource() == eclipseProcess) {
							DebugPlugin.getDefault().removeDebugEventListener(this);
							closeContexts(launchCtx, prepCtx);
						}
					}
				}
			});

			/* Register a debug target */
			if (mode.equals(ILaunchManager.DEBUG_MODE)) {
				/* TODO configurable timeout */
				Throwable lastException = null;
				for (int i = 0; i < 60; i++) {
					try {
						launch.addDebugTarget(launchable.createRemoteDebugTarget(configuration, launch, prepCtx, eclipseProcess));
						break;
					} catch (UncheckedIOException ce) {
						lastException = ce;
						if (ce.getCause() instanceof ConnectException) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								throw new IllegalStateException(e);
							}
						} else {
							throw ce;
						}
					}
				}
				if (launch.getDebugTargets().length == 0)
					throw new CoreException(Status.error("Failed to launch debugger.", lastException));
			} else {
				launch.addDebugTarget(launchable.createDefaultDebugTarget(launch, prepCtx, eclipseProcess));
			}
		}
		catch(RuntimeException | CoreException re) {
			eclipseProcess.terminate();
			throw re;
		}
	}

	@Override
	protected void closeContexts(LaunchContext launchCtx, IWritablePreparationContext prepCtx) {
		try {
			prepCtx.close();
		} catch (Exception e) {
			LOG.error("Failed to close preparation context", e);
		} finally {
			launchCtx.close();
		}
	}

}