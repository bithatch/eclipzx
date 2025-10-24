package uk.co.bithatch.zxbasic.ui.decorators;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class LibraryFolderDecorator implements ILightweightLabelDecorator {

    @Override
    public void decorate(Object element, IDecoration decoration) {
        if (element instanceof IFolder folder && isUserLibraryFolder(folder)) {
            ImageDescriptor desc = ZXBasicUiActivator.getInstance().getImageRegistry().getDescriptor(ZXBasicUiActivator.LIBRARY_DECORATOR_PATH);
			decoration.addOverlay(desc, IDecoration.BOTTOM_RIGHT);
        }
    }

    private boolean isUserLibraryFolder(IFolder folder) {
        IProject project = folder.getProject();
        for(var ext : ZXBasicPreferencesAccess.get().getExternalLibs(project)) {
        	if(ext.getAbsoluteFile().toString().equals(folder.getLocation().toFile().toString())) {
        		return true;
        	}
        }
        return false;
    }

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}
}
