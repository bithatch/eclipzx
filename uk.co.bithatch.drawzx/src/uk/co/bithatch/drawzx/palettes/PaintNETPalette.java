package uk.co.bithatch.drawzx.palettes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import uk.co.bithatch.zyxy.graphics.Palette;

public class PaintNETPalette implements Palette {
	
	private Entry[] entries;

	public PaintNETPalette(Path file) throws IOException {
		var l = new ArrayList<Entry>();
		for(var line : Files.readAllLines(file)) {
			line = line.trim();
			if(line.length() > 0 && !line.startsWith(";")) {
				l.add(Entry.ofRgb((int)Long.parseLong(line, 16)));
			}
		}
		entries = l.toArray(new Entry[0]);
	}

	@Override
	public Entry[] colors() {
		return entries;
	}

	@Override
	public Palette withoutTransparency() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Palette withTransparency(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int bytes() {
		throw new UnsupportedOperationException();
	}

}
