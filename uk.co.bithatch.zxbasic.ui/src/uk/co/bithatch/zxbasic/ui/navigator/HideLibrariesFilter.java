package uk.co.bithatch.zxbasic.ui.navigator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class HideLibrariesFilter extends ViewerFilter {
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof LibrariesNode) {
           return false;
        }
        return true;
    }

}
