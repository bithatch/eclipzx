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
	protected void createEditorLayout(Composite root) {
		var layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		root.setLayout(layout);

		spriteGrid = new SpriteEditorGrid(root, spriteCell, SWT.BORDER);
		spriteGrid.backgroundType(BackgroundType.SMALL_CHEQUER);
		spriteGrid.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
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