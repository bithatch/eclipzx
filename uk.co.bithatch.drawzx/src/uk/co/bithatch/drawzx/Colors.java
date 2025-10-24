package uk.co.bithatch.drawzx;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.widgets.Display;

import uk.co.bithatch.zyxy.graphics.Palette.Entry;

public class Colors {
	
	public static RGB toRGBA(Entry entry) {
		return new RGB(entry.r(),entry.g(),entry.b());
	}

	public static Color toColor(Entry entry, Display display) {
		return new Color(display, toRGBA(entry));
	}

	public static Entry toEntry(RGBA rgba) {
		return toEntry(rgba.rgb);
	}
	public static Entry toEntry(RGB rgb) {
		return new Entry(rgb.red, rgb.green, rgb.blue, false);
	}
}
