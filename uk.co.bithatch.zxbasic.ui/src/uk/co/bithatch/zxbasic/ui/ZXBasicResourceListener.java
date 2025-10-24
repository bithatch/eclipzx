package uk.co.bithatch.zxbasic.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.zxbasic.ui.builder.NaturePromptUtil;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicBuilder;

public class ZXBasicResourceListener implements IResourceChangeListener {

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        try {
            IResourceDelta rootDelta = event.getDelta();
            if (rootDelta != null) {
                rootDelta.accept(delta -> {
                    if (delta.getResource().getType() == IResource.PROJECT) {
                        if ((delta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
                            System.out.println("Project references changed: " + delta.getResource().getName());
                            // You can re-fetch project.getDescription().getReferencedProjects() here
                        }
                    }
                    else if(delta.getKind() == IResourceDelta.ADDED) {
	                    IResource res = delta.getResource();
	                    if (res instanceof IFile file && FileNames.hasExtensions(file.getName(), ZXBasicBuilder.EXTENSIONS)) {
	                        NaturePromptUtil.maybePromptToEnableNature(file.getProject());
	                    }
                	}
                    return false;
                });
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
}
