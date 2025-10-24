package uk.co.bithatch.drawzx.screens;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.drawzx.editor.AbstractScreenEditor;

public class ZoomOutHandler extends AbstractScreenHandler {
	
	public ZoomOutHandler() {
	}

	@Override
	protected Object onHandle(ExecutionEvent event, AbstractScreenEditor ase) {
    	ase.zoomOut();
    	return null;
	}
}
