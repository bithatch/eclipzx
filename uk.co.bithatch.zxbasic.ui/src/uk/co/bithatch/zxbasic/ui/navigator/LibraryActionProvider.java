package uk.co.bithatch.zxbasic.ui.navigator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

import uk.co.bithatch.zxbasic.ui.util.FileStorageEditorInput;

public class LibraryActionProvider extends CommonActionProvider {

    private static final String STORAGE_EDITOR = "uk.co.bithatch.zxbasic.ui.storageEditor";
	private IAction openAction;

    @Override
    public void init(ICommonActionExtensionSite site) {
        openAction = new Action("Open") {
            @Override
            public void run() {
                var selection = (IStructuredSelection) site.getStructuredViewer().getSelection();
                var element = selection.getFirstElement();
                if (element instanceof LibraryFileNode libNode) {
                	var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                	try {
						IDE.openEditor(page, new FileStorageEditorInput(libNode), STORAGE_EDITOR);
					} catch (PartInitException e) {
					}

                }
            }
        };
        openAction.setId("open");
        
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        menu.add(openAction);
    }
}
