package uk.co.bithatch.drawzx.editor;

import java.io.IOException;
import java.io.InputStream;

import uk.co.bithatch.drawzx.sprites.SpriteSheet;

/**
 * Editor for ZX Next tile definition files (.TIL).
 * 
 * <p>Tile definitions are always 4-bit (16 colour), 8×8 pixels, with each tile
 * occupying 32 bytes. This is the same format as 4-bit sprites but with a fixed
 * 8×8 cell size.</p>
 * 
 * @see <a href="https://wiki.specnext.dev/Tilemap">ZX Next Tilemap - Tile Definitions</a>
 */
public class TileDefEditor extends SP4Editor {

	@Override
	protected SpriteSheet loadSheet(InputStream in) throws IOException {
		// Tile definitions are always 4-bit, 8x8
		return SpriteSheet.load(in, 4, true);
	}

	@Override
	public int[] cellSizes() {
		// Tile definitions are always 8x8, no other sizes
		return new int[] { 8 };
	}

	@Override
	protected int swatchColumnsFor(SpriteSheet sheet) {
		return sheet.size() > 256 ? 16 : 8;
	}

	@Override
	protected int swatchCellSizeFor(SpriteSheet sheet) {
		return sheet.size() > 256 ? 16 : 20;
	}

	@Override
	public int defaultPaletteIndex() {
		return 15;
	}
}