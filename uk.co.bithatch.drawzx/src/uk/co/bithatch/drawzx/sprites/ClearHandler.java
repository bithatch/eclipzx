package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.drawzx.editor.AbstractSpriteEditor;

public class ClearHandler extends AbstractSpriteHandler {

	@Override
	protected Object onHandle(ExecutionEvent event, AbstractSpriteEditor ase) {
    	ase.clear();
    	return null;
	}
}
