package uk.co.bithatch.emuzx.ui;

/**
 * Main tab for "Bundle Application" launch configurations. Identical to the
 * external-emulator main tab (project, program, output format) but without
 * any coupling to an emulator configuration tab.
 */
public class BundleLaunchConfigurationTab extends ExternalEmulatorLaunchConfigurationTab {

	public BundleLaunchConfigurationTab() {
		super();
	}

	@Override
	void setEmulatorTab(ExternalEmulatorConfigurationTab emulatorTab) {
		/* no-op — bundle configs have no emulator tab */
	}

	@Override
	protected void updateLaunchConfigurationDialog() {
		/* The superclass calls emulatorTab.getWorkingDir() which would NPE.
		 * Replicate the logic without the emulator dependency. */
		scheduleUpdateJob();
		architectureChanged();
	}
}