package uk.co.bithatch.drawzx.screens;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.drawzx.editor.AbstractScreenEditor;

public class ZoomInHandler extends AbstractScreenHandler {
	
	public ZoomInHandler() {
	}

	@Override
	protected Object onHandle(ExecutionEvent event, AbstractScreenEditor ase) {
    	ase.zoomIn();
    	return null;
	}
}
