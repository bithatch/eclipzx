package uk.co.bithatch.ayzxfx.editor;

public class AddEffectHandler extends AbstractAYFXEditorHandler {

	@Override
	protected void onHandle(AFXEditor ase) {
		var afbe = (AFBEditor)ase;
		afbe.removeEffects(afbe.afx());
	}
}
