package uk.co.bithatch.zxbasic.ui.library;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.ecore.resource.Resource;

import uk.co.bithatch.zxbasic.IIncludeSource;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class ZXBasicLibraryIncludeSource implements IIncludeSource {

	@Override
	public Path find(Resource resource, String importURI) {
		var resUri = resource.getURI().toString();
		if(resUri.startsWith("platform:/resource/")) {
			var file = ResourcesPlugin.getWorkspace().getRoot().findMember(resUri.substring(19));
			if(file != null) {
				var libDirs = ZXBasicPreferencesAccess.get().getAllLibs(file.getProject());
				for(var libDir : libDirs) {
					var resFile = new File(libDir, importURI);
					if(resFile.exists()) {
						return resFile.toPath();
					}
				}
			}
		}
		return null;
	}

	
}
