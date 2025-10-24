package uk.co.bithatch.ayzxfx.editor;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

public class SnapToNoteHandler extends AbstractHandler implements IElementUpdater {

	private boolean enabled = false;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		enabled = !enabled;

		var part = HandlerUtil.getActiveEditor(event);
		if (part instanceof AFXEditor ase) {
			ase.setSnapToNote(!ase.isSnapToNote());
		}

		return null;
	}

	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
		element.setChecked(enabled);
	}
}
