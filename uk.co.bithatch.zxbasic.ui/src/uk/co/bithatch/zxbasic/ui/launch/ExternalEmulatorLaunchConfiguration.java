package uk.co.bithatch.zxbasic.ui.launch;

import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CUSTOM_WORKING_DIRECTORY;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.EMULATOR_ARGS;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.OUTPUT_FORMAT;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PROGRAM;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PROJECT;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.WORKING_DIRECTORY_LOCATION;
import static uk.co.bithatch.zxbasic.ui.builder.ZXBasicBuilder.compileForLaunch;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.bitzx.LaunchContext;
import uk.co.bithatch.emuzx.AbstractConfigurationDelegate;
import uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes;
import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicBuilder;
import uk.co.bithatch.zxbasic.ui.debug.DezogDebugTarget;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;
import uk.co.bithatch.zxbasic.ui.preparation.DefaultPreparationContext;
import uk.co.bithatch.zxbasic.ui.preparation.PreparationSourceRegistry;
import uk.co.bithatch.zxbasic.ui.preparation.PreparationTargetRegistry;
import uk.co.bithatch.zxbasic.ui.util.FileSet;

public class ExternalEmulatorLaunchConfiguration extends AbstractConfigurationDelegate {
	private final static ILog LOG = ILog.of(ExternalEmulatorLaunchConfiguration.class);

	public ExternalEmulatorLaunchConfiguration() {
		super(PROJECT, PROGRAM);
	}

	@Override
	public void launch(IFile file, ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {
		var strmgr = VariablesPlugin.getDefault().getStringVariableManager();
		var prepCtx = new DefaultPreparationContext(configuration, file);

		/*
		 * Initialise preparation target if there is one. Don't do preparation just yet,
		 * just set up context for dynamic variables etc
		 */
		var preparationTarget = PreparationTargetRegistry.targetFor(configuration);
		if (preparationTarget.isPresent()) {
			prepCtx.preparedBinaryFilePath(preparationTarget.get().init(prepCtx));
		}
		var launchCtx = LaunchContext.set(configuration);
		try {

			/* Pick the best format to use */
			var launchFmt = configuration.getAttribute(OUTPUT_FORMAT, "");
			var prj = file.getProject();
			var prjFmt = ZXBasicPreferencesAccess.get().getOutputFormat(prj);

			prepCtx.outputFormat(launchFmt.equals("") ? prjFmt : LanguageSystem.outputFormatOrDefault(prj, launchFmt));

			/* Compile to the chosen format for the launch. */
			compileForLaunch(prepCtx, mode, ZXBasicBuilder.DEFAULT_REPORTER);
			launchCtx.attr(LaunchContext.BINARY_FILE, prepCtx.binaryFile());

			/* If there is preparation to do, do it now */
			if (preparationTarget.isPresent()) {

				var externalFiles = new ArrayList<FileSet>();

				/* Plugins that can find more resources to contribute */
				for (var desc : PreparationSourceRegistry.descriptors()) {
					desc.createSource().contribute(prepCtx, externalFiles, monitor);
				}

				/* Do the prep */
				var status = preparationTarget.get().prepare(monitor, externalFiles);
				if (!status.isOK()) {
					throw new CoreException(status);
				}
			}

			/* Emulator */
			var emulator = strmgr.performStringSubstitution(
					configuration.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.EMULATOR_EXECUTABLE, ""));

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
				workingDir = prj.getLocation().toFile();
			}
			pb.directory(workingDir);

			/* Start the process */

			LOG.info(String.format("Emulator working directory: %s", workingDir));
			LOG.info(String.format("Emulator Launch Command: %s", String.join(" ", cmd)));

			Process process = null;
			try {
				process = pb.start();
			} catch (IOException e) {
				throw new CoreException(
						new Status(IStatus.ERROR, "uk.co.bithatch.zxbasic", "Failed to start emulator", e));
			}

			/* Wrap it as an IProcess so Eclipse can manage it */
			var eclipseProcess = DebugPlugin.newProcess(launch, process, "External Emulator");

			/* Register a debug target */
			if (mode.equals(ILaunchManager.DEBUG_MODE)) {
				/* TODO configurable timeout */
				for (int i = 0; i < 60; i++) {
					try {
						launch.addDebugTarget(new DezogDebugTarget(launch, configuration, eclipseProcess));
					} catch (UncheckedIOException ce) {
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
					throw new CoreException(Status.error("Failed to launch debugger."));
			} else {
				launch.addDebugTarget(new ExternalEmulatorDebugTarget(launch, eclipseProcess));
			}
		} catch (IllegalStateException ise) {
			if (ise.getCause() instanceof CoreException ce) {
				throw ce;
			} else {
				throw ise;
			}
		} finally {
			/*
			 * Clean up preparation if there is any (remove any context from dynamic
			 * variables that are based on launch attributes)
			 */
			try {
				if (preparationTarget.isPresent()) {
					preparationTarget.get().cleanUp();
				}
			} finally {
				LaunchContext.clear();
			}
		}
	}


}