package uk.co.bithatch.zxbasic.ui.preprocessing;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

public class ZXBasicResourceUtil {
    public static Optional<IFile> getFile(Resource resource) {
        URI uri = resource.getURI();
        if (uri.isPlatformResource()) {
            return Optional.of(ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(uri.toPlatformString(true))));
        }
        return Optional.empty();
    }
    
    public static Optional<IProject> getProject(Resource resource) {
    	return getFile(resource).map(IFile::getProject);
    }
}
