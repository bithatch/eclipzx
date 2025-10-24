package uk.co.bithatch.jspeccy.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import uk.co.bithatch.jspeccy.Activator;

public class EmulatorFilesLabelProvider extends LabelProvider {
    private final WorkbenchLabelProvider delegate = new WorkbenchLabelProvider();

    @Override
    public Image getImage(Object element) {
        if (element instanceof IFile ifile && ifile.getFileExtension() != null) {
        	if(ifile.getFileExtension().equalsIgnoreCase("rom")) {
                return Activator.getDefault().getImageRegistry().get(Activator.ROM_PATH);
        	}
        	else if(ifile.getFileExtension().equalsIgnoreCase("tap") || 
        			ifile.getFileExtension().equalsIgnoreCase("tzx") ||
        			ifile.getFileExtension().equalsIgnoreCase("csw")) {
                return Activator.getDefault().getImageRegistry().get(Activator.TAPE_PATH);
        	}
        	if(ifile.getFileExtension().equalsIgnoreCase("sna") ||
        			ifile.getFileExtension().equalsIgnoreCase("z80") ||
        			ifile.getFileExtension().equalsIgnoreCase("szx") ||
        			ifile.getFileExtension().equalsIgnoreCase("sp")) {
                return Activator.getDefault().getImageRegistry().get(Activator.SNAPSHOT_PATH);
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
