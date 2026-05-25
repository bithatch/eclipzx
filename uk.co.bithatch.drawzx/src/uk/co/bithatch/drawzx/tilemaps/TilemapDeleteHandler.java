package uk.co.bithatch.drawzx.tilemaps;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.drawzx.editor.TilemapEditor;

public class TilemapDeleteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var part = HandlerUtil.getActiveEditor(event);
		if (part instanceof TilemapEditor te) {
			te.deleteSelection();
		}
		return null;
	}
}
