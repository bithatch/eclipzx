package uk.co.bithatch.drawzx.palettes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import uk.co.bithatch.zyxy.graphics.Palette;

public class JASCPalette implements Palette {
	
	private Entry[] entries;

	public JASCPalette(Path file) throws IOException {
		var l = new ArrayList<Entry>();
		var started = false;
		for(var line : Files.readAllLines(file)) {
			line = line.trim();
			if(line.equals("")) 
				continue;
			
			var parts = line.split("\\s+");
			if(parts.length == 1 && !started) {
				continue;
			}
			else if(parts.length > 1 && !started) {
				started = true;
			}
			
			if(parts.length > 2) {
				l.add(new Entry(Integer.parseInt(parts[0]), Integer.parseInt(parts[0]), Integer.parseInt(parts[0])));
			}
			else
				throw new IOException("Unexpected content in JASC Palette file.");
		}
		entries = l.toArray(new Entry[0]);
	}

	@Override
	public Entry[] colors() {
		return entries;
	}

	@Override
	public int bytes() {
		throw new UnsupportedOperationException();
	}

}
