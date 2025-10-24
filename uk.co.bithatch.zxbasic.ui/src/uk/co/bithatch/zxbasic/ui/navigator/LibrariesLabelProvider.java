package uk.co.bithatch.zxbasic.ui.navigator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;

public class LibrariesLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof LibrariesNode) {
            return "Libraries";
        } else if (element instanceof IFolder folder) {
            return folder.getName();
        } else if (element instanceof LibraryNode zxl) {
            return zxl.getLibrary().name();
        } else if (element instanceof LibraryFileNode zxl) {
            return zxl.getFile().getName();
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof LibrariesNode) {
            return ZXBasicUiActivator.getInstance().getImageRegistry().get(ZXBasicUiActivator.LIBRARY_PATH);
        } else if (element instanceof LibraryNode pxl) {
        	var zxl = pxl.getLibrary();
       		return ZXBasicUiActivator.getInstance().getImageRegistry().get(zxl.icon() == null || zxl.icon().equals("") ? ZXBasicUiActivator.LIBRARY_PATH : zxl.icon());
        } else if (element instanceof LibraryFileNode lfn && lfn.getFile().isDirectory()) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
        } else if (element instanceof LibraryFileNode) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
        }
        return super.getImage(element);
    }
}
