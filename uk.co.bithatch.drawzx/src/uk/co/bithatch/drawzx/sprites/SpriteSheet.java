package uk.co.bithatch.drawzx.sprites;

import static uk.co.bithatch.drawzx.sprites.SpriteUtil.oneBitRowToCellRow;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import uk.co.bithatch.zyxy.graphics.Palette;
import uk.co.bithatch.zyxy.lib.Lang;

public class SpriteSheet {

	public static SpriteSheet load(Path path, int bpp) {
		return load(path, bpp, true);
	}

	public static SpriteSheet load(Path path, int bpp, boolean transparency) {
		try (var in = Files.newInputStream(path)) {
			return load(in, bpp, transparency);
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}

	public static SpriteSheet load(InputStream in, int bpp, boolean transparency) {
		return load(Channels.newChannel(in), bpp, transparency);
	}
	
	public static SpriteSheet load(ReadableByteChannel in, int bpp, boolean transparency) {
			var buf = Lang.readFully(in);
			int[][] data;
			var c = 0;
			var r = 0;

			if (bpp == 8) {
				var pal = Palette.rgb333();
				data = new int[16][1024];
				if(transparency) {
					pal = pal.withTransparency(Palette.DEFAULT_TRANSPARENCY % 16);
					fill(data, Palette.DEFAULT_TRANSPARENCY);
				}
				while (buf.hasRemaining()) {
					SpriteUtil.byteRowToCellRow(buf, 16, c * 16, data[r]);
					r++;
					if (r == 16) {
						r = 0;
						c++;
					}
				}
				return new SpriteSheet(pal, 64, 16, data, bpp);
			} else if (bpp == 4) {
				var pal = Palette.rgb333();
				data = new int[16][2048];
				if(transparency) {
					pal = pal.withTransparency(Palette.DEFAULT_TRANSPARENCY % 16);
					fill(data, Palette.DEFAULT_TRANSPARENCY % 16);
				}
				while (buf.hasRemaining()) {
					SpriteUtil.nibbleRowToCellRow(buf, 16, c * 16, data[r]);
					r++;
					if (r == 16) {
						r = 0;
						c += 1;
					}
				}
				return new SpriteSheet(pal, 128, 16, data, bpp);
			} else if (bpp == 1) {
				var spriteCells = buf.remaining() / 8;
				data = new int[8][spriteCells * 8];
				for (c = 0; c < spriteCells; c++) {
					for (r = 0; r < 8; r++) {
						oneBitRowToCellRow(buf.get(), c * 8, data[r]);
					}
				}
				return new SpriteSheet(Palette.mono(), spriteCells, 8, data, 1);

			}
			throw new UnsupportedOperationException();
	}
	
	private static void fill(int[][] data, int with) {
		for(var r = 0 ; r < data.length; r++) {
			for(var c = 0 ; c < data[r].length; c++) {
				data[r][c] = with;
			}
		}
	}

	public static SpriteSheet udgSheet() {
		return new SpriteSheet(Palette.mono(), 96, 8);
	}

	private final Palette palette;
	private final int[][] data;
	private final int size;
	private final int cellSize;
	private final SpriteCell[] cells;
	private final int bpp;

	public SpriteSheet() {
		this(96, 1);
	}

	public SpriteSheet(int size, int bpp) {
		this(Palette.mono(), size, bpp);
	}

	public SpriteSheet(Palette palette, int size, int bpp) {
		this(palette, size, 8, bpp);
	}

	public SpriteSheet(Palette palette, int size, int cellSize, int bpp) {
		this(palette, size, cellSize, new int[cellSize][size * cellSize], bpp);
	}

	public SpriteSheet(Palette palette, int size, int cellSize, int[][] data, int bpp) {
		super();
		if (bpp != 8 && bpp != 4 && bpp != 1)
			throw new IllegalArgumentException("Invalid bpp. Can be 8, 4 or 1");
		this.bpp = bpp;
		this.palette = palette;
		this.data = data;
		this.cellSize = cellSize;
		this.size = size;

		cells = new SpriteCell[size];
		for (var c = 0; c < size; c++) {
			var cellXOffset = (c * cellSize);
			cells[c] = new SpriteCell(palette, cellXOffset, cellSize, data);
		}
	}

	private SpriteSheet(Palette palette, int[][] data, int cols, int cellSize, SpriteCell[] cells, int bpp) {
		super();
		if (bpp != 8 && bpp != 4 && bpp != 1)
			throw new IllegalArgumentException("Invalid bpp. Can be 8, 4 or 1");
		this.bpp = bpp;
		this.palette = palette;
		this.data = data;
		this.size = cols;
		this.cellSize = cellSize;
		this.cells = cells;
	}
	
	public int depth() {
		return 1 << bpp;
	}

	public int byteSize() {
		return size * ((cellSize * cellSize) / palette.pixelsPerByte());
	}

	public SpriteCell[] cells() {
		return cells;
	}

	public Palette palette() {
		return palette;
	}

	public int cellSize() {
		return cellSize;
	}

	public int bpp() {
		return bpp;
	}

	public int[][] data() {
		return data;
	}

	public SpriteCell cell(int index) {
		return cells[index];
	}

	public int size() {
		return size;
	}

	public int index(SpriteCell cell) {
		for (var c = 0; c < size; c++) {
			if (cells[c] == cell)
				return c;
		}
		return -1;
	}

	public SpriteSheet withCellSize(int newCellSize) {
		if (newCellSize == this.cellSize) return this;
		int oldCellSize = this.cellSize;
		int pixelsPerOldSprite = oldCellSize * oldCellSize;
		int pixelsPerNewSprite = newCellSize * newCellSize;

		if (oldCellSize % newCellSize == 0) {
			// Splitting: e.g. 16x16 -> 8x8 using sequential byte reinterpretation
			// (matches ZX Next hardware: 256 bytes split into 4 x 64-byte sub-patterns)
			int subPatternsPerOld = pixelsPerOldSprite / pixelsPerNewSprite;
			int newSize = size * subPatternsPerOld;
			var newData = new int[newCellSize][newSize * newCellSize];
			var cells = new SpriteCell[newSize];

			for (var oldIdx = 0; oldIdx < size; oldIdx++) {
				for (var sub = 0; sub < subPatternsPerOld; sub++) {
					var newIdx = oldIdx * subPatternsPerOld + sub;
					var cellXOffset = newIdx * newCellSize;
					cells[newIdx] = new SpriteCell(palette, cellXOffset, newCellSize, newData);
					for (var y = 0; y < newCellSize; y++) {
						for (var x = 0; x < newCellSize; x++) {
							var pixelInSprite = sub * pixelsPerNewSprite + y * newCellSize + x;
							var origRow = pixelInSprite / oldCellSize;
							var origCol = pixelInSprite % oldCellSize;
							newData[y][cellXOffset + x] = data[origRow][oldIdx * oldCellSize + origCol];
						}
					}
				}
			}
			return new SpriteSheet(palette, newData, newSize, newCellSize, cells, bpp);

		} else if (newCellSize % oldCellSize == 0) {
			// Merging: e.g. 8x8 -> 16x16 (reverse of the sequential reinterpretation)
			int subPatternsPerNew = pixelsPerNewSprite / pixelsPerOldSprite;
			int newSize = size / subPatternsPerNew;
			var newData = new int[newCellSize][newSize * newCellSize];
			var cells = new SpriteCell[newSize];

			for (var newIdx = 0; newIdx < newSize; newIdx++) {
				var cellXOffset = newIdx * newCellSize;
				cells[newIdx] = new SpriteCell(palette, cellXOffset, newCellSize, newData);
				for (var y = 0; y < newCellSize; y++) {
					for (var x = 0; x < newCellSize; x++) {
						var pixelInNewSprite = y * newCellSize + x;
						var sub = pixelInNewSprite / pixelsPerOldSprite;
						var pixelInSub = pixelInNewSprite % pixelsPerOldSprite;
						var oldRow = pixelInSub / oldCellSize;
						var oldCol = pixelInSub % oldCellSize;
						var oldIdx = newIdx * subPatternsPerNew + sub;
						newData[y][cellXOffset + x] = data[oldRow][oldIdx * oldCellSize + oldCol];
					}
				}
			}
			return new SpriteSheet(palette, newData, newSize, newCellSize, cells, bpp);

		}
		throw new IllegalArgumentException("Cell sizes must be evenly divisible");
	}

	public SpriteSheet withBpp(int bpp) {
		return new SpriteSheet(palette, data, size, cellSize, cells, bpp);
	}

	public SpriteSheet withPalette(Palette palette) {
		var newCells = new SpriteCell[size];
		for (var c = 0; c < size; c++) {
			newCells[c] = cells[c].withPalette(palette);
		}
		return new SpriteSheet(palette, data, size, cellSize, newCells, bpp);
	}

	
	public void save(WritableByteChannel wtr) throws IOException {
		if(bpp == 1) {
			var buf = ByteBuffer.allocate(cellSize * data.length);
			for(var c = 0 ; c < size * cellSize; c += cellSize) {
				for(var r = 0 ; r < data.length; r++) {
					var v = 0;
					for(var b = 0 ; b < cellSize ; b++) {
						if(data[r][c + b] > 0) {
							v |= 1 << (7 -b);
						}
					}
					buf.put((byte)v);
				}
				buf.flip();
				wtr.write(buf);
				buf.clear();
			}
		}
		else if(bpp == 8) {
			var buf = ByteBuffer.allocate(cellSize * data.length);
			for(var c = 0 ; c < size * cellSize; c += cellSize) {
				for(var r = 0 ; r < data.length; r++) {
					for(var b = 0 ; b < cellSize ; b++) {
						buf.put((byte)data[r][c + b]);
					}
				}
				buf.flip();
				wtr.write(buf);
				buf.clear();
			}
		}
		else if(bpp == 4) {
			var bytesPerRow = cellSize / 2;
			var buf = ByteBuffer.allocate(bytesPerRow * data.length);
			for(var c = 0 ; c < size * cellSize; c += cellSize) {
				for(var r = 0 ; r < data.length; r++) {
					for(var b = 0 ; b < cellSize ; b += 2) {
						buf.put((byte)((data[r][c + b] << 4) | (data[r][c + b + 1] & 0xf)));
					}
				}
				buf.flip();
				wtr.write(buf);
				buf.clear();
			}
		}
		else {
			throw new UnsupportedOperationException("TODO");
		}
		
	}

}
