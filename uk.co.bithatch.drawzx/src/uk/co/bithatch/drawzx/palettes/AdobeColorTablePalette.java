package uk.co.bithatch.drawzx.palettes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import uk.co.bithatch.zyxy.graphics.Palette;

public class AdobeColorTablePalette implements Palette {
	
	private Entry[] entries;

	public AdobeColorTablePalette(Path path) throws IOException {
		try (var in = new BufferedInputStream(Files.newInputStream(path))) {
            byte[] buffer = in.readAllBytes();
            int colorCount = Math.min(buffer.length / 3, 256); // usually 256 colors max
            entries = new Entry[colorCount];
            for (int i = 0; i < colorCount; i++) {
                int r = buffer[i * 3] & 0xFF;
                int g = buffer[i * 3 + 1] & 0xFF;
                int b = buffer[i * 3 + 2] & 0xFF;
                entries[i] = new Entry(r, g, b);
            }
        }
	}

	@Override
	public Entry[] colors() {
		return entries;
	}

	@Override
	public int bytes() {
		return size() * 3;
	}

}
