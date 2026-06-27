package uk.co.bithatch.zxbasic.ui.editor;

import org.eclipse.core.resources.IFile;

import uk.co.bithatch.eclipzpp.ui.AbstractOpenGeneratedFileHandler;
import uk.co.bithatch.zxbasic.ui.builder.ZXDebugBuild;

public class OpenGeneratedFileHandler extends AbstractOpenGeneratedFileHandler {

	@Override
	public IFile sourceToGeneratedResource(IFile sourceFile) {
        return ZXDebugBuild.generateAsm(sourceFile);
	}
}
