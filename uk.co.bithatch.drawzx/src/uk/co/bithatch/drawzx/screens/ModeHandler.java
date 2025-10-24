package uk.co.bithatch.drawzx.screens;

import static uk.co.bithatch.drawzx.editor.EditorFileProperties.EDITOR_MODE_PROPERTY;
import static uk.co.bithatch.drawzx.editor.EditorFileProperties.setProperty;

import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import uk.co.bithatch.drawzx.editor.AbstractScreenEditor;
import uk.co.bithatch.drawzx.editor.AbstractSpriteEditor;
import uk.co.bithatch.drawzx.editor.EditorFileProperties;
import uk.co.bithatch.drawzx.editor.EditorFileProperties.ScreenPaintMode;

public class ModeHandler extends AbstractScreenHandler implements IElementUpdater {

	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {

		var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			var page = window.getActivePage();
			if (page != null) {
				var editor = page.getActiveEditor();

				if (editor instanceof AbstractSpriteEditor spriteEditor) {
					var file = spriteEditor.getFile();
					if (file != null) {
						String value = (String) parameters.get(PARAMETER);
						element.setChecked(value != null && value.equals(EditorFileProperties
								.getProperty(file, EDITOR_MODE_PROPERTY, ScreenPaintMode.SELECT.name()).toUpperCase()));
					}
				}
			}
		}
	}

	@Override
	protected Object onHandle(ExecutionEvent event, AbstractScreenEditor ase) {
		String mode = event.getParameter(PARAMETER);
		/* TODO getting called twice, 2nd value is correct */
		if (mode != null) {
			setProperty(ase.getFile(), EDITOR_MODE_PROPERTY, mode.toUpperCase());
		}
		return null;

	}
}
