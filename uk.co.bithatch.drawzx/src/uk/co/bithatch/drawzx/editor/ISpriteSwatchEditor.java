package uk.co.bithatch.drawzx.editor;

import org.eclipse.ui.IWorkbenchPart;

import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.drawzx.views.ISpriteView;
import uk.co.bithatch.drawzx.widgets.AbstractSpriteGrid.BackgroundType;

/**
 * Interface that the SpriteView uses to communicate with sprite-related editors.
 * Mirrors the pattern of {@link IColouredEditor} for the ColourPickerView.
 * 
 * <p>Implemented by any editor that uses a {@link uk.co.bithatch.drawzx.widgets.SpriteSwatch}
 * to display a spritesheet and allow sprite selection, including sprite editors
 * and the tilemap editor (for tile definition selection).</p>
 */
public interface ISpriteSwatchEditor extends IWorkbenchPart {

	/**
	 * Get the current sprite sheet.
	 */
	SpriteSheet spriteSheet();

	/**
	 * Get the number of columns for the swatch display.
	 */
	int swatchColumns();

	/**
	 * Get the cell pixel size for the swatch display.
	 */
	int swatchCellSize();

	/**
	 * Get the available cell sizes for the cell size combo.
	 * Return an empty array if cell size selection is not supported.
	 */
	int[] cellSizes();

	/**
	 * Get the currently selected sprite index.
	 */
	int selectedSpriteIndex();

	/**
	 * Called by the view when the user selects a sprite in the swatch.
	 */
	void selectSprite(int index);

	/**
	 * Called by the view when the user changes the cell size in the combo.
	 */
	void cellSizeChanged(int newCellSize);

	/**
	 * Link or unlink this editor with a sprite view.
	 * Pass null to unlink.
	 */
	void spriteView(ISpriteView spriteView);

	/**
	 * Whether the sprite preview (normal/inverse) should be shown.
	 * Sprite editors return true; tilemap editors return false.
	 */
	default boolean showPreviews() {
		return true;
	}

	/**
	 * The background type for the swatch cells.
	 */
	default BackgroundType backgroundType() {
		return BackgroundType.LARGE_CHEQUER;
	}
}
