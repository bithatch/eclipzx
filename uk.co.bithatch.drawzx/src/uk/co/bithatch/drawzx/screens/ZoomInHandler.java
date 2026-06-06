package uk.co.bithatch.drawzx.screens;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.drawzx.editor.AbstractScreenEditor;
import uk.co.bithatch.drawzx.editor.AbstractSpriteEditor;
import uk.co.bithatch.drawzx.editor.TilemapEditor;

public class ZoomInHandler extends AbstractHandler {
	
	public ZoomInHandler() {
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var part = HandlerUtil.getActiveEditor(event);
		if (part instanceof AbstractScreenEditor ase) {
			ase.zoomIn();
		} else if (part instanceof TilemapEditor te) {
			te.zoomIn();
		} else if (part instanceof AbstractSpriteEditor se) {
			se.zoomIn();
		}
		return null;
	}
}
