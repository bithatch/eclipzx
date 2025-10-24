package uk.co.bithatch.drawzx.editor;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.drawzx.widgets.AbstractSpriteGrid.BackgroundType;
import uk.co.bithatch.drawzx.widgets.SpriteEditorGrid;
import uk.co.bithatch.drawzx.widgets.SpriteSwatch;

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
	protected void layoutGridAndSwatch(Composite parent) {
		var palettes = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(2, false);
		layout.verticalSpacing = 4;
		palettes.setLayout(layout);
		palettes.setLayoutData(GridDataFactory.create(SWT.NONE).align(SWT.FILL, SWT.FILL).grab(true, true).create());

		spriteSwatch = new SpriteSwatch(palettes, spriteSheet, 32, 4, SWT.BORDER);
		spriteSwatch.setLayoutData(
				GridDataFactory.create(SWT.NONE).align(SWT.CENTER, SWT.CENTER).grab(false, true).create());

		spriteGrid = new SpriteEditorGrid(palettes, spriteCell, SWT.BORDER);
		spriteGrid.backgroundType(BackgroundType.SMALL_CHEQUER);
		spriteGrid.setLayoutData(GridDataFactory.create(SWT.NONE).align(SWT.FILL, SWT.FILL).grab(true, true).create());
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
