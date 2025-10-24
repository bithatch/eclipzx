package uk.co.bithatch.fatexplorer.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.fatexplorer.preferences.FATPreferencesAccess;
import uk.co.bithatch.fatexplorer.views.FATExplorerView;

public class OpenInFATExplorerHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		 var selection = HandlerUtil.getCurrentSelection(event);
	        if (selection instanceof IStructuredSelection sel) {
	            var element = sel.getFirstElement();
	            if (element instanceof IFile file) {
	                var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	                try {
						FATPreferencesAccess.addImagePath(file.getFullPath().toPortableString().substring(1));
	                    page.showView(FATExplorerView.ID);
//	                    var view = (FATExplorerView) page.showView(FATExplorerView.ID);
//						view.setInput(FATPreferencesAccess.getConfiguredImageURIs());
						
	                    // TODO
//	                    File nativeFile = file.getLocation().toFile();
//						view.load(nativeFile);
	                } catch (Exception e) {
	                    throw new ExecutionException("Failed to open view", e);
	                }
	            }
	        }
	        return null;
	}

}
