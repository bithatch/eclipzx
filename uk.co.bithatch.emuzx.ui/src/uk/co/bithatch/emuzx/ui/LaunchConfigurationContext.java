package uk.co.bithatch.emuzx.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

public interface LaunchConfigurationContext {

	IPath resolveProjectPath();

	IFile resolveProgram();

	void initializeFrom(ILaunchConfigurationWorkingCopy cfg);

}