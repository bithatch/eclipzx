package uk.co.bithatch.emuzx.api;

import java.io.Closeable;
import java.io.File;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchConfiguration;

import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.IProgramBuildOptions;

public interface IPreparationContext extends Closeable {
	@Override
	void close();
	
	void addCleanUpTask(Runnable task);
	
	ILaunchConfiguration launchConfiguration();
	
	IOutputFormat outputFormat();
	
	IProgramBuildOptions buildOptions();
	
	IFile programFile();
	
	File binaryFile();
	
	Optional<String> preparedBinaryFilePath();
}
