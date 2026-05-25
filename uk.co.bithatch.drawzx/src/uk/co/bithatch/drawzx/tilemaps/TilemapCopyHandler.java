package uk.co.bithatch.drawzx.tilemaps;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.drawzx.editor.TilemapEditor;

public class TilemapCopyHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var part = HandlerUtil.getActiveEditor(event);
		if (part instanceof TilemapEditor te) {
			te.copySelectionToClipboard();
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		var window = org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			var page = window.getActivePage();
			if (page != null) {
				var editor = page.getActiveEditor();
				if (editor instanceof TilemapEditor te) {
					return te.hasSelection();
				}
			}
		}
		return false;
	}
}
