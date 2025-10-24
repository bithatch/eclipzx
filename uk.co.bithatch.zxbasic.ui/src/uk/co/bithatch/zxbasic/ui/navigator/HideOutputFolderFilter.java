package uk.co.bithatch.zxbasic.ui.navigator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class HideOutputFolderFilter extends ViewerFilter {
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof IFolder folder) {
            try {
                IProject project = folder.getProject();
                String outputFolderName = ZXBasicPreferencesAccess.get().getOutputPath(project); // implement this
                String fname = folder.getName();
                if(outputFolderName.endsWith("/"))
                	fname += "/";
				return !fname.equals(outputFolderName);
            } catch (Exception e) {
                return true;
            }
        }
        return true;
    }

}
