package uk.co.bithatch.drawzx.editor;

import java.io.IOException;
import java.io.InputStream;

import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.drawzx.widgets.AbstractSpriteGrid.BackgroundType;

public class SPREditor extends AbstractSpriteEditor {

	@Override
	protected SpriteSheet loadSheet(InputStream in) throws IOException {
		return SpriteSheet.load(in, 8, true);
	}

	@Override
	public boolean isPalettedChangeAllowed() {
		return true;
	}

	@Override
	public int defaultPaletteIndex() {
		return 0;
	}

	@Override
	public int defaultSecondaryPaletteIndex() {
		return spriteCell.palette().transparency().orElse(255);
	}

	@Override
	protected void configureGrid() {
		spriteGrid.backgroundType(BackgroundType.SMALL_CHEQUER);
	}

	@Override
	public int[] cellSizes() {
		return new int[] { 16, 8 };
	}

	@Override
	protected int swatchColumnsFor(SpriteSheet sheet) {
		return sheet.cellSize() <= 8 ? 8 : 4;
	}

	@Override
	protected int swatchCellSizeFor(SpriteSheet sheet) {
		return sheet.cellSize() <= 8 ? 20 : 32;
	}

	@Override
	public boolean isPaletteHistoryUsed() {
		return true;
	}

	@Override
	public int maxPaletteHistorySize() {
		return 8;
	}

	@Override
	public int paletteCellSize() {
		return 32;
	}

	@Override
	public void colorSelected(int index, boolean primary) {
		if( ( primary ? spriteGrid.color() : spriteGrid.secondaryColor() ) != index) {
			super.colorSelected(index, primary);
		}
	}

	@Override
	public boolean isPaletteResettable() {
		return true;
	}
}