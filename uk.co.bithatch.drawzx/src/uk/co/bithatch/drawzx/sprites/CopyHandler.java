package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.drawzx.editor.AbstractSpriteEditor;

public class CopyHandler extends AbstractSpriteHandler {
	
	@Override
    public boolean isEnabled() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IEditorPart part = page.getActiveEditor();
                if (part instanceof AbstractSpriteEditor editor) {
                    return editor.hasSelection(); // your method
                }
            }
        }
        return false;
    }

	@Override
	protected Object onHandle(ExecutionEvent event, AbstractSpriteEditor ase) {
    	ase.copySelectionToClipboard();
    	return null;
	}
}
