package uk.co.bithatch.drawzx.editor;

public class SCREditor extends AbstractScreenEditor {

	
	@Override
	public int paletteCellSize() {
		return 64;
	}

	@Override
	public int defaultPaletteIndex() {
		return 0;
	}
	
	@Override
	public int defaultSecondaryPaletteIndex() {
		return 7;
	}
}
