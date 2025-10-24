package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.drawzx.editor.AbstractSpriteEditor;

public class MirrorVHandler extends AbstractSpriteHandler {
	
	public MirrorVHandler() {
	}

	@Override
	protected Object onHandle(ExecutionEvent event, AbstractSpriteEditor ase) {
    	ase.mirrorSpriteV();
    	return null;
	}
}
