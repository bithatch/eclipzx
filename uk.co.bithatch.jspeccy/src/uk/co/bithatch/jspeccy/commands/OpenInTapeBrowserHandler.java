package uk.co.bithatch.jspeccy.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

import uk.co.bithatch.jspeccy.editor.TapeBrowser;

public class OpenInTapeBrowserHandler extends AbstractHandler {

	@Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        var selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection sel) {
            var element = sel.getFirstElement();
            if (element instanceof IFile file) {
            	try {
					var editor = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file,
							TapeBrowser.ID, true);
					
					if(editor == null) {
						throw new IllegalStateException("Could not find editor.");
					}
					else if(!(editor instanceof TapeBrowser)) {
						throw new IllegalStateException("Another browser is already working on this file.");
					}
					
				} catch (PartInitException e) {
					throw new ExecutionException("Failed to start tape browser.", e);
				}
            }
        }
        return null;
    }
}
