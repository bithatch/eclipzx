package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;

import uk.co.bithatch.drawzx.editor.AbstractSpriteEditor;

public class PasteHandler extends AbstractSpriteHandler {

	@Override
	protected Object onHandle(ExecutionEvent event, AbstractSpriteEditor ase) {
		ase.pasteFromClipboard();
		return null;
	}

	@Override
	public boolean isEnabled() {
		var clipboard = new Clipboard(Display.getCurrent());
		try {
			var contents = (String) clipboard.getContents(TextTransfer.getInstance());
			return contents != null && !contents.isEmpty();
		} finally {
			clipboard.dispose();
		}
	}

}
