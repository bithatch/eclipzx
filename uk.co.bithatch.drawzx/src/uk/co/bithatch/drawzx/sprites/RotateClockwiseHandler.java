package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.drawzx.editor.AbstractSpriteEditor;

public class RotateClockwiseHandler extends AbstractSpriteHandler {
	
	public RotateClockwiseHandler() {
	}

	@Override
	protected Object onHandle(ExecutionEvent event, AbstractSpriteEditor ase) {
    	ase.rotate(90);
    	return null;
	}
}
