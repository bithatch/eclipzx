package uk.co.bithatch.zxbasic.ui.api;

import java.io.File;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchConfiguration;

import uk.co.bithatch.bitzx.IOutputFormat;

public interface IPreparationContext {
	
	ILaunchConfiguration launchConfiguration();
	
	IOutputFormat outputFormat();
	
	IProgramBuildOptions buildOptions();
	
	IFile programFile();
	
	File binaryFile();
	
	Optional<String> preparedBinaryFilePath();
}
