package uk.co.bithatch.zxbasic.ui.launch;

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

public class ExternalEmulatorLaunchShortcut extends AbstractEmulatorLaunchShortcut {

	@Override
	protected void doLaunch(IFile file, String mode) throws CoreException {

		var workbench = PlatformUI.getWorkbench();
		var shell = workbench.getActiveWorkbenchWindow().getShell();

		while (true) {

			var manager = DebugPlugin.getDefault().getLaunchManager();
			var type = manager.getLaunchConfigurationType(ID);
			var name = file.getProject().getName() + " - " + file.getName();

			ILaunchConfiguration config = null;

			for (var cfg : manager.getLaunchConfigurations()) {
				if (cfg.getName().equals(name)) {
					if (cfg.getAttribute(EMULATOR_EXECUTABLE, "").equals("")) {
						config = cfg;
						break;
					} else {
						DebugUITools.launch(cfg, mode);
					}
					return;
				}
			}

			if (config == null) {
				var workingCopy = type.newInstance(null, manager.generateLaunchConfigurationName(name));

				// Configure your emulator
				workingCopy.setAttribute(EMULATOR_EXECUTABLE, "");
				workingCopy.setAttribute(PROGRAM, file.getProjectRelativePath().toString());
				workingCopy.setAttribute(PROJECT, file.getProject().getName());

				config = workingCopy.doSave();
			}

			if (DebugUITools.openLaunchConfigurationPropertiesDialog(shell, config,
					mode.equals(ILaunchManager.DEBUG_MODE) ? "org.eclipse.debug.ui.launchGroup.debug"
							: "org.eclipse.debug.ui.launchGroup.run") != Window.OK) {
				return;
			}
		}

	}

	@Override
	protected String[] getSupportedExtensions() {
		return new String[0];
	}

}
