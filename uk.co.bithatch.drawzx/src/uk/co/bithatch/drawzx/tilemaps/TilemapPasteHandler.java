package uk.co.bithatch.drawzx.tilemaps;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.drawzx.editor.TilemapEditor;

public class TilemapPasteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var part = HandlerUtil.getActiveEditor(event);
		if (part instanceof TilemapEditor te) {
			te.pasteFromClipboard();
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		var clipboard = new Clipboard(Display.getCurrent());
		try {
			var contents = (String) clipboard.getContents(TextTransfer.getInstance());
			return contents != null && contents.startsWith("TILEMAP_CLIP:");
		} finally {
			clipboard.dispose();
		}
	}
}
