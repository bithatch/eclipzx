package uk.co.bithatch.squashzx;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class CompressedFilesLabelProvider extends LabelProvider {
    private final WorkbenchLabelProvider delegate = new WorkbenchLabelProvider();

    @Override
    public Image getImage(Object element) {
        if (element instanceof IFile ifile && ifile.getFileExtension() != null) {
        	if(ifile.getFileExtension().equalsIgnoreCase("zx0")) {
                return Activator.getDefault().getImageRegistry().get(Activator.SQUASH_PATH);
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
