package uk.co.bithatch.drawzx.tilemaps;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.zyxy.lib.Lang;

/**
 * Represents a ZX Next tilemap. A tilemap is a 2D grid of {@link TilemapEntry}
 * values, where each entry references a tile (pattern) index from a tile
 * definition set ({@link SpriteSheet}), along with optional attributes such as
 * mirror, rotate, and palette offset.
 * 
 * <p>Supported configurations:</p>
 * <ul>
 *   <li>40x32 tiles (standard mode, 320x256 pixels with 8x8 tiles)</li>
 *   <li>80x32 tiles (high-res mode, 640x256 pixels with 8x8 tiles)</li>
 *   <li>16-bit entries (2 bytes: attributes + tile index)</li>
 *   <li>8-bit entries (1 byte: tile index only)</li>
 * </ul>
 * 
 * @see <a href="https://wiki.specnext.dev/Tilemap">ZX Next Tilemap</a>
 */
public class Tilemap {

	public enum TilemapMode {
		/** 40 columns x 32 rows, standard resolution */
		STANDARD_40x32(40, 32),
		/** 80 columns x 32 rows, high resolution */
		HIRES_80x32(80, 32);

		private final int columns;
		private final int rows;

		TilemapMode(int columns, int rows) {
			this.columns = columns;
			this.rows = rows;
		}

		public int columns() { return columns; }
		public int rows() { return rows; }
		public int totalTiles() { return columns * rows; }
	}

	private final TilemapMode mode;
	private final boolean sixteenBit;
	private final TilemapEntry[][] entries;
	private SpriteSheet tileDefinitions;

	/**
	 * Create a new empty tilemap.
	 */
	public Tilemap(TilemapMode mode, boolean sixteenBit, SpriteSheet tileDefinitions) {
		this.mode = mode;
		this.sixteenBit = sixteenBit;
		this.tileDefinitions = tileDefinitions;
		this.entries = new TilemapEntry[mode.rows()][mode.columns()];
		for (var r = 0; r < mode.rows(); r++) {
			for (var c = 0; c < mode.columns(); c++) {
				entries[r][c] = new TilemapEntry();
			}
		}
	}

	/**
	 * Create a tilemap with existing entries.
	 */
	public Tilemap(TilemapMode mode, boolean sixteenBit, TilemapEntry[][] entries, SpriteSheet tileDefinitions) {
		this.mode = mode;
		this.sixteenBit = sixteenBit;
		this.entries = entries;
		this.tileDefinitions = tileDefinitions;
	}

	/**
	 * Load a tilemap from a file. The tile definitions must be provided separately.
	 */
	public static Tilemap load(Path path, TilemapMode mode, boolean sixteenBit, SpriteSheet tileDefinitions) {
		try (var in = Files.newInputStream(path)) {
			return load(in, mode, sixteenBit, tileDefinitions);
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}

	public static Tilemap load(InputStream in, TilemapMode mode, boolean sixteenBit, SpriteSheet tileDefinitions) {
		return load(Channels.newChannel(in), mode, sixteenBit, tileDefinitions);
	}

	public static Tilemap load(ReadableByteChannel in, TilemapMode mode, boolean sixteenBit, SpriteSheet tileDefinitions) {
		var buf = Lang.readFully(in);
		var entries = new TilemapEntry[mode.rows()][mode.columns()];

		for (var r = 0; r < mode.rows(); r++) {
			for (var c = 0; c < mode.columns(); c++) {
				if (!buf.hasRemaining()) {
					entries[r][c] = new TilemapEntry();
				} else if (sixteenBit) {
					int attr = Byte.toUnsignedInt(buf.get());
					int tile = buf.hasRemaining() ? Byte.toUnsignedInt(buf.get()) : 0;
					entries[r][c] = TilemapEntry.decode16bit(attr, tile);
				} else {
					entries[r][c] = TilemapEntry.decode8bit(Byte.toUnsignedInt(buf.get()));
				}
			}
		}

		return new Tilemap(mode, sixteenBit, entries, tileDefinitions);
	}

	public void save(WritableByteChannel wtr) throws IOException {
		int bytesPerEntry = sixteenBit ? 2 : 1;
		var buf = ByteBuffer.allocate(mode.totalTiles() * bytesPerEntry);

		for (var r = 0; r < mode.rows(); r++) {
			for (var c = 0; c < mode.columns(); c++) {
				var entry = entries[r][c];
				if (sixteenBit) {
					var encoded = entry.encode16bit();
					buf.put(encoded[0]);
					buf.put(encoded[1]);
				} else {
					buf.put((byte) entry.tileIndex());
				}
			}
		}

		buf.flip();
		wtr.write(buf);
	}

	public TilemapMode mode() {
		return mode;
	}

	public boolean isSixteenBit() {
		return sixteenBit;
	}

	public int columns() {
		return mode.columns();
	}

	public int rows() {
		return mode.rows();
	}

	public TilemapEntry entry(int col, int row) {
		return entries[row][col];
	}

	public void entry(int col, int row, TilemapEntry entry) {
		entries[row][col] = entry;
	}

	public TilemapEntry[][] entries() {
		return entries;
	}

	public SpriteSheet tileDefinitions() {
		return tileDefinitions;
	}

	public void tileDefinitions(SpriteSheet tileDefinitions) {
		this.tileDefinitions = tileDefinitions;
	}

	public int byteSize() {
		return mode.totalTiles() * (sixteenBit ? 2 : 1);
	}

	/**
	 * Create a snapshot of all entries for undo/redo purposes.
	 */
	public TilemapEntry[][] snapshot() {
		var snap = new TilemapEntry[mode.rows()][mode.columns()];
		for (var r = 0; r < mode.rows(); r++) {
			for (var c = 0; c < mode.columns(); c++) {
				snap[r][c] = entries[r][c].copy();
			}
		}
		return snap;
	}

	/**
	 * Restore entries from a snapshot.
	 */
	public void restore(TilemapEntry[][] snapshot) {
		for (var r = 0; r < mode.rows(); r++) {
			for (var c = 0; c < mode.columns(); c++) {
				entries[r][c] = snapshot[r][c];
			}
		}
	}

	/**
	 * Fill the entire tilemap with a given tile index.
	 */
	public void fill(int tileIndex) {
		for (var r = 0; r < mode.rows(); r++) {
			for (var c = 0; c < mode.columns(); c++) {
				entries[r][c] = new TilemapEntry(tileIndex);
			}
		}
	}

	/**
	 * Create a new empty tilemap with default tile definitions (256 blank 8x8 1bpp tiles).
	 */
	public static Tilemap createEmpty(TilemapMode mode, boolean sixteenBit) {
		return new Tilemap(mode, sixteenBit, new SpriteSheet(256, 1));
	}
}
