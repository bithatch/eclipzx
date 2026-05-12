package uk.co.bithatch.emuzx.api;

import java.io.File;

import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.IProgramBuildOptions;

public interface IWritablePreparationContext extends IPreparationContext {
	
	void outputFormat(IOutputFormat fmt);
	
	void binaryFile(File file);
	
	void buildOptions(IProgramBuildOptions buildOptions);
	
	void preparedBinaryFilePath(String preparedBinaryFilePath);
}
