package uk.co.bithatch.bitzx;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder for the ZX Spectrum <strong>.TAP</strong> tape format.
 * <p>
 * A TAP file is a sequence of data blocks. Each logical "file" on the tape
 * consists of a <em>header block</em> (flag 0x00, 17 payload bytes) followed by
 * a <em>data block</em> (flag 0xFF, arbitrary payload). The builder accumulates
 * these pairs and writes them all when {@link #writeTo(OutputStream)} is
 * called.
 * <p>
 * By default the builder enforces that the <em>first</em> entry is a BASIC
 * Program block — this mirrors how real tapes work (the ROM loader expects a
 * BASIC program first). Call {@link #allowNonBasicFirstBlock(boolean)
 * allowNonBasicFirstBlock(true)} to override this check.
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * try (var out = Files.newOutputStream(path)) {
 *     new TAPBuilder()
 *         .addBasicLoader("loader", 32767, 32768)
 *         .addCode("game", binData, 32768)
 *         .writeTo(out);
 * }
 * }</pre>
 */
public final class TAPBuilder implements IFormatBuilder {

	/* TAP header file-type constants */
	private static final int TYPE_PROGRAM = 0;
	private static final int TYPE_NUMBER_ARRAY = 1;
	private static final int TYPE_CHAR_ARRAY = 2;
	private static final int TYPE_CODE = 3;

	/** Maximum filename length in a TAP header. */
	private static final int FILENAME_LENGTH = 10;

	/**
	 * A single TAP entry: a matched header block + data block pair.
	 */
	private record TapEntry(byte[] headerBlock, byte[] dataBlock, int fileType) {
	}

	private final List<TapEntry> entries = new ArrayList<>();
	private boolean allowNonBasicFirstBlock;

	// ------------------------------------------------------------------ builder

	/**
	 * If set to {@code true}, the builder will not throw when the first entry
	 * is not a BASIC Program block. Default is {@code false}.
	 *
	 * @param allow whether to allow a non-BASIC first block
	 * @return this builder for chaining
	 */
	public TAPBuilder allowNonBasicFirstBlock(boolean allow) {
		this.allowNonBasicFirstBlock = allow;
		return this;
	}

	// ------------------------------------------------------- IFormatBuilder API

	@Override
	public WellKnownOutputFormat format() {
		return WellKnownOutputFormat.TAP;
	}

	@Override
	public TAPBuilder addCode(String name, byte[] data, int loadAddress) {
		byte[] header = buildHeader(TYPE_CODE, name, data.length, loadAddress, 32768);
		entries.add(new TapEntry(header, data, TYPE_CODE));
		return this;
	}

	@Override
	public TAPBuilder addBasicLoader(String name, int clearAddress, int startAddress) {
		try {
			byte[] basicProg = generateLoader(clearAddress, startAddress);
			return addBasicProgram(name, basicProg, 10);
		} catch (IOException e) {
			throw new java.io.UncheckedIOException("Failed to generate BASIC loader", e);
		}
	}

	@Override
	public TAPBuilder addBasicProgram(String name, byte[] basicData, int autoStartLine) {
		byte[] header = buildHeader(TYPE_PROGRAM, name, basicData.length, autoStartLine, basicData.length);
		entries.add(new TapEntry(header, basicData, TYPE_PROGRAM));
		return this;
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		if (entries.isEmpty()) {
			throw new IllegalStateException("No entries have been added to the TAP builder.");
		}

		if (!allowNonBasicFirstBlock && entries.get(0).fileType() != TYPE_PROGRAM) {
			throw new IllegalStateException(
					"The first block in a TAP file must be a BASIC Program. "
					+ "Call allowNonBasicFirstBlock(true) to override this check.");
		}

		for (var entry : entries) {
			writeTapBlock(out, (byte) 0x00, entry.headerBlock());
			writeTapBlock(out, (byte) 0xFF, entry.dataBlock());
		}
	}

	// --------------------------------------------------------- TAP wire format

	/**
	 * Build a 17-byte TAP header payload (without flag or checksum — those are
	 * added by {@link #writeTapBlock}).
	 *
	 * <pre>
	 * Offset  Length  Description
	 * 0       1       File type (0-3)
	 * 1       10      Filename (space-padded)
	 * 11      2       Data length (LE)
	 * 13      2       Param 1 (LE) — autostart line / load address
	 * 15      2       Param 2 (LE) — program length / 32768
	 * </pre>
	 */
	private static byte[] buildHeader(int type, String name, int dataLength, int param1, int param2) {
		byte[] header = new byte[17];

		// Byte 0: file type
		header[0] = (byte) type;

		// Bytes 1-10: filename, space-padded
		byte[] nameBytes = padFilename(name);
		System.arraycopy(nameBytes, 0, header, 1, FILENAME_LENGTH);

		// Bytes 11-12: data block length (LE)
		writeLE16(header, 11, dataLength);

		// Bytes 13-14: param 1 (LE)
		writeLE16(header, 13, param1);

		// Bytes 15-16: param 2 (LE)
		writeLE16(header, 15, param2);

		return header;
	}

	/**
	 * Write a single TAP block to the output stream.
	 * <p>
	 * Wire format:
	 * <pre>
	 *   2 bytes   block length (LE) = 1 (flag) + payload.length + 1 (checksum)
	 *   1 byte    flag (0x00 = header, 0xFF = data)
	 *   N bytes   payload
	 *   1 byte    checksum (XOR of flag and all payload bytes)
	 * </pre>
	 */
	private static void writeTapBlock(OutputStream out, byte flag, byte[] payload) throws IOException {
		int blockLen = 1 + payload.length + 1; // flag + payload + checksum

		// 2-byte LE block length
		out.write(blockLen & 0xFF);
		out.write((blockLen >> 8) & 0xFF);

		// Flag byte
		out.write(flag & 0xFF);

		// Payload
		out.write(payload);

		// Checksum: XOR of flag and every payload byte
		int checksum = flag & 0xFF;
		for (byte b : payload) {
			checksum ^= (b & 0xFF);
		}
		out.write(checksum & 0xFF);
	}

	// ----------------------------------------------------------- BASIC loader

	/**
	 * Generate a minimal BASIC loader program:
	 * <pre>
	 *   10 CLEAR &lt;clearAddress&gt;
	 *   20 LOAD "" CODE
	 *   30 RANDOMIZE USR &lt;startAddress&gt;
	 * </pre>
	 *
	 * Uses {@link SimpleZXNextBasicCompileer#encodeLine(String)} for correct
	 * Sinclair BASIC tokenisation and {@link ZxNumberEncoder} number encoding.
	 */
	private static byte[] generateLoader(int clearAddress, int startAddress) throws IOException {
		var lines = List.of(
			"10 CLEAR " + clearAddress,
			"20 LOAD \"\" CODE",
			"30 RANDOMIZE USR " + startAddress
		);
		return SimpleZXNextBasicCompileer.encodeProgram(lines);
	}

	// --------------------------------------------------------------- helpers

	/**
	 * Pad or truncate a filename to exactly {@value #FILENAME_LENGTH} bytes,
	 * space-padding on the right.
	 */
	private static byte[] padFilename(String name) {
		byte[] result = new byte[FILENAME_LENGTH];
		Arrays.fill(result, (byte) ' ');
		byte[] src = name.getBytes();
		System.arraycopy(src, 0, result, 0, Math.min(src.length, FILENAME_LENGTH));
		return result;
	}

	private static void writeLE16(byte[] buf, int offset, int value) {
		buf[offset] = (byte) (value & 0xFF);
		buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
	}
}
