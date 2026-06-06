package uk.co.bithatch.emuzx.ui;

import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.EMULATOR_EXECUTABLE;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.ID;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PROGRAM;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PROJECT;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.emuzx.AbstractEmulatorLaunchShortcut;
import uk.co.bithatch.emuzx.LaunchableRegistry;
import uk.co.bithatch.emuzx.api.IExternallyLaunchable;

public final class ExternalEmulatorLaunchShortcut extends AbstractEmulatorLaunchShortcut {

	@Override
	protected final void doLaunch(IFile file, String mode) throws CoreException {

		var manager = DebugPlugin.getDefault().getLaunchManager();
		var type = manager.getLaunchConfigurationType(ID);
		var name = file.getProject().getName() + " - " + file.getName();
		var projectName = file.getProject().getName();
		var programPath = file.getProjectRelativePath().toString();

		// Look for an existing config of the right type matching the project and program
		ILaunchConfiguration config = null;
		for (var cfg : manager.getLaunchConfigurations(type)) {
			if (cfg.getAttribute(PROJECT, "").equals(projectName)
					&& cfg.getAttribute(PROGRAM, "").equals(programPath)) {
				config = cfg;
				break;
			}
		}

		// If no existing config, create one
		if (config == null) {
			var workingCopy = type.newInstance(null, manager.generateLaunchConfigurationName(name));
			workingCopy.setAttribute(EMULATOR_EXECUTABLE, "");
			workingCopy.setAttribute(PROGRAM, programPath);
			workingCopy.setAttribute(PROJECT, projectName);
			config = workingCopy.doSave();
		}

		// If no emulator configured yet, open the dialog so the user can set one up
		if (config.getAttribute(EMULATOR_EXECUTABLE, "").isEmpty()) {
			var workbench = PlatformUI.getWorkbench();
			var shell = workbench.getActiveWorkbenchWindow().getShell();
			var groupId = mode.equals(ILaunchManager.DEBUG_MODE)
					? "org.eclipse.debug.ui.launchGroup.debug"
					: "org.eclipse.debug.ui.launchGroup.run";

			if (DebugUITools.openLaunchConfigurationPropertiesDialog(shell, config, groupId) != Window.OK) {
				return;
			}
			// Re-read the config after the dialog may have saved changes
			for (var cfg : manager.getLaunchConfigurations(type)) {
				if (cfg.getAttribute(PROJECT, "").equals(projectName)
						&& cfg.getAttribute(PROGRAM, "").equals(programPath)) {
					config = cfg;
					break;
				}
			}
		}

		DebugUITools.launch(config, mode);
	}

	@Override
	protected String[] getSupportedExtensions() {
		return LaunchableRegistry.descriptors(IExternallyLaunchable.class).stream().flatMap(d -> d.extensions().stream()).distinct().toArray(String[]::new);
	}

}
