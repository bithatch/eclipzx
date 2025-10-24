package uk.co.bithatch.zxbasic.ui.api;

import org.eclipse.core.resources.IProject;

public interface ILaunchPreparationUI {

	void updateLaunchConfigurationDialog();

	IProject resolveProject();

}
