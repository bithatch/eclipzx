package uk.co.bithatch.bitzx;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Shared low-level encoding helpers for the ZX Spectrum TAP wire format.
 * <p>
 * Both {@link TAPBuilder} and {@link TZXBuilder} delegate to these methods
 * for constructing 17-byte TAP headers, computing checksums, writing
 * flag+payload+checksum blocks, and generating minimal BASIC loaders.
 * <p>
 * This class is package-private — it is an implementation detail, not public
 * API.
 */
final class TapEncoding {

	/* TAP header file-type constants */
	static final int TYPE_PROGRAM = 0;
	static final int TYPE_NUMBER_ARRAY = 1;
	static final int TYPE_CHAR_ARRAY = 2;
	static final int TYPE_CODE = 3;

	/** Maximum filename length in a TAP header. */
	static final int FILENAME_LENGTH = 10;

	/**
	 * A single TAP entry: a matched header block + data block pair.
	 */
	record TapEntry(byte[] headerBlock, byte[] dataBlock, int fileType) {
	}

	private TapEncoding() {
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
	static byte[] buildHeader(int type, String name, int dataLength, int param1, int param2) {
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
	static void writeTapBlock(OutputStream out, byte flag, byte[] payload) throws IOException {
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

	/**
	 * Assemble the raw bytes of a TAP block (flag + payload + checksum) without
	 * the 2-byte length prefix. Used by {@link TZXBuilder} which wraps these
	 * bytes inside a TZX ID 0x10 container instead of the TAP length header.
	 *
	 * @param flag    the flag byte (0x00 for header, 0xFF for data)
	 * @param payload the block payload
	 * @return flag + payload + checksum bytes
	 */
	static byte[] assembleTapBlockBytes(byte flag, byte[] payload) {
		byte[] result = new byte[1 + payload.length + 1]; // flag + payload + checksum
		result[0] = flag;
		System.arraycopy(payload, 0, result, 1, payload.length);

		int checksum = flag & 0xFF;
		for (byte b : payload) {
			checksum ^= (b & 0xFF);
		}
		result[result.length - 1] = (byte) (checksum & 0xFF);
		return result;
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
	static byte[] generateLoader(int clearAddress, int startAddress) throws IOException {
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
	static byte[] padFilename(String name) {
		byte[] result = new byte[FILENAME_LENGTH];
		Arrays.fill(result, (byte) ' ');
		byte[] src = name.getBytes();
		System.arraycopy(src, 0, result, 0, Math.min(src.length, FILENAME_LENGTH));
		return result;
	}

	static void writeLE16(byte[] buf, int offset, int value) {
		buf[offset] = (byte) (value & 0xFF);
		buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
	}
}
