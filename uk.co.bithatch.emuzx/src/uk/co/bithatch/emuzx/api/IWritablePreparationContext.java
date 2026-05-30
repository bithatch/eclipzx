package uk.co.bithatch.emuzx.api;

import java.nio.file.Path;

import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.IProgramBuildOptions;

public interface IWritablePreparationContext extends IPreparationContext {
	
	void outputFormat(IOutputFormat fmt);
	
	void launchFile(Path file);
	
	void buildOptions(IProgramBuildOptions buildOptions);
	
	void preparedBinaryFilePath(String preparedBinaryFilePath);
}
