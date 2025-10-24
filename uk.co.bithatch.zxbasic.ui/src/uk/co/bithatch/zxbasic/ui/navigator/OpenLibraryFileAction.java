package uk.co.bithatch.zxbasic.ui.navigator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import uk.co.bithatch.zxbasic.ui.util.EditorUtil;

public class OpenLibraryFileAction implements IObjectActionDelegate {
    private LibraryFileNode libFile;

    @Override
    public void run(IAction action) {
        if (libFile != null) {
            EditorUtil.openFileInEditor(libFile);
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object first = ((IStructuredSelection) selection).getFirstElement();
            if (first instanceof LibraryFileNode libFile) {
                this.libFile = libFile;
            }
        }
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) { }
}
