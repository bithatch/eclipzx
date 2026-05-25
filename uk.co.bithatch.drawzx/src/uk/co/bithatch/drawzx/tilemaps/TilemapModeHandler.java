package uk.co.bithatch.drawzx.tilemaps;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import uk.co.bithatch.drawzx.editor.TilemapEditor;
import uk.co.bithatch.drawzx.widgets.TilemapEditorGrid.TilemapPaintMode;

public class TilemapModeHandler extends AbstractHandler implements IElementUpdater {

	private static final String PARAMETER = "uk.co.bithatch.drawzx.tilemaps.mode";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var part = HandlerUtil.getActiveEditor(event);
		if (part instanceof TilemapEditor te) {
			var mode = event.getParameter(PARAMETER);
			if (mode != null) {
				te.setMode(TilemapPaintMode.valueOf(mode.toUpperCase()));
			}
		}
		return null;
	}

	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
		var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			var page = window.getActivePage();
			if (page != null) {
				var editor = page.getActiveEditor();
				if (editor instanceof TilemapEditor te) {
					var value = (String) parameters.get(PARAMETER);
					if (value != null) {
						element.setChecked(value.equalsIgnoreCase(te.getMode().name()));
					}
				}
			}
		}
	}
}
