package uk.co.bithatch.zxbasic.ui.tools;

import java.io.File;

import uk.co.bithatch.bitzx.FileNames;

public abstract class AbstractTool {

	abstract File targetFile(File srcFile);

	
	public boolean isNeedsProcessing(File srcfile) {
		return FileNames.isNeedsProcessing(srcfile, targetFile(srcfile));
	}
	
	
}
