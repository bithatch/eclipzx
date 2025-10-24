package uk.co.bithatch.emuzx.api;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

public interface IEmulator {
	
	void configure(EmulatorDescriptor descriptor, ILaunchConfigurationWorkingCopy configuration, IFile programFile, File home, String mode);
}
