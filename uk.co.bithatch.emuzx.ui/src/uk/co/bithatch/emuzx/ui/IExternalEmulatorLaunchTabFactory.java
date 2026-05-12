package uk.co.bithatch.emuzx.ui;

import java.util.List;

import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public interface IExternalEmulatorLaunchTabFactory {

	void fillTabs(String mode, ILaunchConfigurationDialog dialog, List<ILaunchConfigurationTab> tabs);
}
