package uk.co.bithatch.emuzx.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * Tab group for "Bundle Application" launch configurations.
 * Contains only the Main tab (project/program/output format) and the
 * Preparation tab (disk image / directory options). No emulator tabs.
 */
public class BundleLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		var launchTab = new BundleLaunchConfigurationTab();
		var preparationTab = new DiskImagePreparationTab();

		preparationTab.setLaunchTab(launchTab::resolveProject);
		preparationTab.setPreparationRequired(true);

		setTabs(new ILaunchConfigurationTab[] {
			launchTab,
			preparationTab,
			new CommonTab()
		});
	}
}
