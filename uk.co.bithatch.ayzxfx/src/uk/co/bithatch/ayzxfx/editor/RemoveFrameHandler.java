package uk.co.bithatch.ayzxfx.editor;

public class RemoveFrameHandler extends AbstractAYFXEditorHandler {

	@Override
	protected void onHandle(AFXEditor ase) {
    	ase.removeSelection(); 
	}
}
