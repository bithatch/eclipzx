package uk.co.bithatch.drawzx.editor;

import java.io.IOException;
import java.io.InputStream;

import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.zyxy.graphics.Palette;

public class UDGEditor extends AbstractSpriteEditor {

	@Override
	protected SpriteSheet loadSheet(InputStream in) throws IOException {
		return SpriteSheet.load(in, 1, false);
	}

	@Override
	public int defaultPaletteIndex() {
		return 1;
	}

	@Override
	public int paletteCellSize() {
		return 64;
	}

	@Override
	public void setDefaultPalette() {
		setSpriteSheet(spriteSheet.withPalette(Palette.mono()));
		markDirty();
	}

	@Override
	public void setDefaultTransPalette() {
		setDefaultPalette();;
	}
}
