package uk.co.bithatch.drawzx.views;

import uk.co.bithatch.drawzx.sprites.SpriteCell;
import uk.co.bithatch.drawzx.sprites.SpriteSheet;

/**
 * Interface that editors use to communicate with the SpriteView.
 * Mirrors the pattern of {@link IColourPicker} for the ColourPickerView.
 */
public interface ISpriteView {

	/**
	 * Update the swatch with a new sprite sheet configuration.
	 */
	void updateSpriteSheet(SpriteSheet spriteSheet, int columns, int cellSize);

	/**
	 * Update the currently selected sprite index in the swatch.
	 */
	void updateSelection(int index);

	/**
	 * Refresh the spritesheet info labels (cells, size, depth, cell size combo).
	 */
	void updateSpriteSheetInfo();

	/**
	 * Redraw the swatch (e.g. after pixel edits to a sprite cell).
	 */
	void redrawSwatch();

	/**
	 * Update the sprite preview and index label for the given cell.
	 */
	void updateSpritePreview(SpriteCell cell, int index);
}
