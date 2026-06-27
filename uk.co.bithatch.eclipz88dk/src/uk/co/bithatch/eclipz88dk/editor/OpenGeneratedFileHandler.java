package uk.co.bithatch.eclipz88dk.editor;

import org.eclipse.core.resources.IFile;

import uk.co.bithatch.eclipzpp.ui.AbstractOpenGeneratedFileHandler;
import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;

public class OpenGeneratedFileHandler extends AbstractOpenGeneratedFileHandler {

	@Override
	public IFile sourceToGeneratedResource(IFile sourceFile) {
		if (sourceFile == null || !sourceFile.exists()) {
			return null;
		}
		if (!"c".equals(sourceFile.getFileExtension())) {
			return null;
		}

		var outputFolder = Z88DKPreferencesAccess.get().getOutputFolder(sourceFile.getProject());
		if (outputFolder == null) {
			return null;
		}

		/* The generated ASM path mirrors source layout under the build directory.
		 * Example: src/main.c -> Debug/src/main.c.asm */
		var generatedRelativePath = sourceFile.getProjectRelativePath().addFileExtension("asm");
		return outputFolder.getFile(generatedRelativePath);
	}
}
