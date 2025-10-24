package uk.co.bithatch.drawzx.widgets;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import uk.co.bithatch.drawzx.Colors;
import uk.co.bithatch.zyxy.graphics.Palette;

public class ColorCache {

	private Map<Palette.Entry, Color> cache = new HashMap<>();
	private Display display;
	
	public ColorCache(Display display) {
		this.display = display;
	}
	
	public Color color(Palette.Entry entry) {
		return cache.computeIfAbsent(entry, k -> Colors.toColor(k, display));
	}
	
	public void dispose() {
		cache.forEach((k,v) -> v.dispose());
	}
}
