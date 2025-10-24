package uk.co.bithatch.fatexplorer.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import uk.co.bithatch.fatexplorer.Activator;

public class DiskImageFilesLabelProvider extends LabelProvider {
    private final WorkbenchLabelProvider delegate = new WorkbenchLabelProvider();

    @Override
    public Image getImage(Object element) {
        if (element instanceof IFile ifile && ifile.getFileExtension() != null) {
        	if(ifile.getFileExtension().equalsIgnoreCase("img")) {
                return Activator.getDefault().getImageRegistry().get(Activator.DISK_IMAGE_PATH);
        	}
        }
        return super.getImage(element);
    }

    @Override
    public String getText(Object element) {
        return delegate.getText(element);
    }

    @Override
    public void dispose() {
        delegate.dispose();
        super.dispose();
    }
}
