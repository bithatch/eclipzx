package uk.co.bithatch.zxbasic.ui.builder;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicOutputFormat;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class ZXDebugBuild {

	public static IFile generateAsm(IFile sourceFile) {
		var project = sourceFile.getProject();
		var root = project.getWorkspace().getRoot();
		var bldr = ZXBasicBuilder.builderForProject(project);
		
        bldr.withMemoryMap(false);
        bldr.withOutputFormat(BorielZXBasicOutputFormat.ASM);
        
        var zxbc = bldr.build();
        var nativeFile = sourceFile.getLocation().toFile();
		if(zxbc.isNeedsProcessing(nativeFile)) {
        	try {
				zxbc.compile(nativeFile);
			} catch (IOException e) {
				throw new IllegalStateException("Failed to generate ASM.");
			}
        }
		
		try {
			ZXBasicPreferencesAccess.get().getOutputFolder(project).refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
		
		var outputPath = zxbc.targetFile(nativeFile).toPath();
		var res = root.findFilesForLocationURI(outputPath.toUri());
		if(res != null && res.length > 0) {
			return res[0];
		}
		else
			return null;
	}
}
