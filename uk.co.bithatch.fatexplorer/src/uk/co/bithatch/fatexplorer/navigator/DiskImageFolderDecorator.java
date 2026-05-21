package uk.co.bithatch.fatexplorer.navigator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import uk.co.bithatch.fatexplorer.Activator;
import uk.co.bithatch.fatexplorer.FATDiskImageManager;
import uk.co.bithatch.fatexplorer.preferences.FATLock;

/**
 * Decorates FAT disk image linked folders with a lock overlay icon
 * when the underlying image is locked for exclusive access.
 */
public class DiskImageFolderDecorator implements ILightweightLabelDecorator {

	public static final String ID = "uk.co.bithatch.fatexplorer.diskImageFolderDecorator";

	@Override
	public void decorate(Object element, IDecoration decoration) {
		if (!(element instanceof IFolder folder)) return;

		var parent = folder.getParent();
		if (parent == null || !FATDiskImageManager.MOUNTS_FOLDER.equals(parent.getName())) return;

		var project = folder.getProject();
		if (project == null || !project.isOpen()) return;

		// Find the mount config for this linked folder
		var folderName = folder.getName();
		var mounts = FATDiskImageManager.getMounts(project);
		for (var mount : mounts) {
			if (mount.getName().equals(folderName)) {
				var uri = mount.toURI(project);
				if (FATLock.isImageLocked(uri)) {
					decoration.addOverlay(
						Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, Activator.NO_ENTRY_PATH),
						IDecoration.BOTTOM_LEFT);
				}
				break;
			}
		}
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void dispose() {
	}
}
