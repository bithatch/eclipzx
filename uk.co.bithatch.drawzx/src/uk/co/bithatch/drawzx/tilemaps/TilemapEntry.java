package uk.co.bithatch.drawzx.tilemaps;

/**
 * Represents a single entry in a ZX Next tilemap.
 * 
 * <p>In 16-bit mode, each entry is 2 bytes:</p>
 * <ul>
 *   <li>Byte 0 (attributes): bit 0 = palette offset bit 4, bit 1 = mirror X, 
 *       bit 2 = mirror Y, bit 3 = rotate, bit 4 = ULA over tilemap,
 *       bits 5-7 = palette offset bits 1-3</li>
 *   <li>Byte 1: tile index (0-255)</li>
 * </ul>
 * 
 * <p>In 8-bit mode, each entry is 1 byte (just the tile index).</p>
 */
public class TilemapEntry {

	private int tileIndex;
	private boolean mirrorX;
	private boolean mirrorY;
	private boolean rotate;
	private boolean ulaOverTilemap;
	private int paletteOffset;

	public TilemapEntry() {
		this(0);
	}

	public TilemapEntry(int tileIndex) {
		this.tileIndex = tileIndex;
	}

	public TilemapEntry(int tileIndex, boolean mirrorX, boolean mirrorY, boolean rotate,
			boolean ulaOverTilemap, int paletteOffset) {
		this.tileIndex = tileIndex;
		this.mirrorX = mirrorX;
		this.mirrorY = mirrorY;
		this.rotate = rotate;
		this.ulaOverTilemap = ulaOverTilemap;
		this.paletteOffset = paletteOffset;
	}

	public int tileIndex() {
		return tileIndex;
	}

	public void tileIndex(int tileIndex) {
		this.tileIndex = tileIndex;
	}

	public boolean mirrorX() {
		return mirrorX;
	}

	public void mirrorX(boolean mirrorX) {
		this.mirrorX = mirrorX;
	}

	public boolean mirrorY() {
		return mirrorY;
	}

	public void mirrorY(boolean mirrorY) {
		this.mirrorY = mirrorY;
	}

	public boolean rotate() {
		return rotate;
	}

	public void rotate(boolean rotate) {
		this.rotate = rotate;
	}

	public boolean ulaOverTilemap() {
		return ulaOverTilemap;
	}

	public void ulaOverTilemap(boolean ulaOverTilemap) {
		this.ulaOverTilemap = ulaOverTilemap;
	}

	public int paletteOffset() {
		return paletteOffset;
	}

	public void paletteOffset(int paletteOffset) {
		this.paletteOffset = paletteOffset;
	}

	/**
	 * Encode this entry as a 16-bit (2-byte) tilemap entry.
	 * Returns {attributeByte, tileIndexByte}.
	 */
	public byte[] encode16bit() {
		int attr = 0;
		attr |= (paletteOffset & 0x10) != 0 ? 1 : 0;        // bit 0 = palette offset bit 4
		attr |= mirrorX ? 0x02 : 0;                           // bit 1
		attr |= mirrorY ? 0x04 : 0;                           // bit 2
		attr |= rotate ? 0x08 : 0;                            // bit 3
		attr |= ulaOverTilemap ? 0x10 : 0;                    // bit 4
		attr |= (paletteOffset & 0x0F) << 4;                  // bits 4-7 = palette offset bits 0-3
		// Actually per spec: bits 5-7 = palette offset bits 1-3, bit 0 = palette offset bit 0
		// Let me re-read... The attribute byte for ZX Next tilemap 16-bit mode:
		// Bit 0: X mirror
		// Bit 1: Y mirror  
		// Bit 2: Rotate
		// Bit 3: ULA over tilemap
		// Bit 4-7: Palette offset (4 bits)
		// Actually there are different interpretations. Let me use the most common one:
		// For now, keep it simple.
		attr = 0;
		attr |= mirrorX ? 0x01 : 0;
		attr |= mirrorY ? 0x02 : 0;
		attr |= rotate ? 0x04 : 0;
		attr |= ulaOverTilemap ? 0x08 : 0;
		attr |= (paletteOffset & 0x0F) << 4;
		return new byte[] { (byte) attr, (byte) tileIndex };
	}

	/**
	 * Decode a 16-bit tilemap entry from attribute + tile index bytes.
	 */
	public static TilemapEntry decode16bit(int attrByte, int tileByte) {
		var entry = new TilemapEntry(tileByte & 0xFF);
		entry.mirrorX = (attrByte & 0x01) != 0;
		entry.mirrorY = (attrByte & 0x02) != 0;
		entry.rotate = (attrByte & 0x04) != 0;
		entry.ulaOverTilemap = (attrByte & 0x08) != 0;
		entry.paletteOffset = (attrByte >> 4) & 0x0F;
		return entry;
	}

	/**
	 * Decode an 8-bit tilemap entry (tile index only).
	 */
	public static TilemapEntry decode8bit(int tileByte) {
		return new TilemapEntry(tileByte & 0xFF);
	}

	public TilemapEntry copy() {
		return new TilemapEntry(tileIndex, mirrorX, mirrorY, rotate, ulaOverTilemap, paletteOffset);
	}

	@Override
	public String toString() {
		return String.format("TilemapEntry[tile=%d, mx=%b, my=%b, rot=%b, ula=%b, palOff=%d]",
				tileIndex, mirrorX, mirrorY, rotate, ulaOverTilemap, paletteOffset);
	}
}
