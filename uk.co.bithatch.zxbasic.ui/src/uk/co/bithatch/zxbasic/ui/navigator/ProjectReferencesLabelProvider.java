package uk.co.bithatch.zxbasic.ui.navigator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;

public class ProjectReferencesLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof ProjectReferencesNode) {
            return "Project References";
        } else if (element instanceof IFolder folder) {
            return folder.getName();
        } else if (element instanceof IProject zxl) {
            return zxl.getName();
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof ProjectReferencesNode) {
            return ZXBasicUiActivator.getInstance().getImageRegistry().get(ZXBasicUiActivator.REF_PATH);
        } else if (element instanceof IProject) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
        } else if (element instanceof IFolder) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
        }
        return super.getImage(element);
    }
}
