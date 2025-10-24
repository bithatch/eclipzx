package uk.co.bithatch.drawzx.views;

import uk.co.bithatch.zyxy.graphics.Palette;

public interface IColourPicker {

	void palette(Palette palette);

	int paletteOffset();

	void updatePaletteInfo();
}
