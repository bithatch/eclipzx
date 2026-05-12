package uk.co.bithatch.emuzx.ui;

import org.eclipse.core.resources.IProject;

public interface ILaunchPreparationUI {

	void updateLaunchConfigurationDialog();

	IProject resolveProject();

}
