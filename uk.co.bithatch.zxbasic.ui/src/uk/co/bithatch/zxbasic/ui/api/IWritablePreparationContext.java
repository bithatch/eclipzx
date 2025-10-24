package uk.co.bithatch.zxbasic.ui.api;

import java.io.File;

import uk.co.bithatch.bitzx.IOutputFormat;

public interface IWritablePreparationContext extends IPreparationContext {
	
	void outputFormat(IOutputFormat fmt);
	
	void binaryFile(File file);
	
	void buildOptions(IProgramBuildOptions buildOptions);
	
	void preparedBinaryFilePath(String preparedBinaryFilePath);
}
