package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.drawzx.editor.AbstractSpriteEditor;

public class MirrorHHandler extends AbstractSpriteHandler {
	
	public MirrorHHandler() {
	}

	@Override
	protected Object onHandle(ExecutionEvent event, AbstractSpriteEditor ase) {
    	ase.mirrorSpriteH();
    	return null;
	}
}
