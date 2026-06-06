package uk.co.bithatch.drawzx.editor;

import java.io.IOException;
import java.io.InputStream;

import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.drawzx.widgets.AbstractSpriteGrid.BackgroundType;

public class SP4Editor extends SPREditor {

	@Override
	protected SpriteSheet loadSheet(InputStream in) throws IOException {
		return SpriteSheet.load(in, 4, true);
	}

	@Override
	public boolean isPalettedChangeAllowed() {
		return true;
	}

	@Override
	public int defaultPaletteIndex() {
		return 15;
	}

	@Override
	protected void configureGrid() {
		spriteGrid.backgroundType(BackgroundType.SMALL_CHEQUER);
	}

	@Override
	public void colorSelected(int index, boolean primary) {
		if( ( primary ? spriteGrid.color() : spriteGrid.secondaryColor() ) != index) {
			super.colorSelected(index, primary);
			spriteGrid.paletteOffset(picker.paletteOffset());
		}
	}

	@Override
	public boolean isPaletteOffsetUsed() {
		return true;
	}
}