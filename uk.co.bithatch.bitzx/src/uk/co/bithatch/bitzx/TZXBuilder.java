package uk.co.bithatch.bitzx;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for the ZX Spectrum <strong>.TZX</strong> tape format (version 1.20).
 * <p>
 * A TZX file begins with a 10-byte file header, followed by a sequence of
 * typed blocks. This builder currently emits <strong>ID 0x10 — Standard Speed
 * Data Blocks</strong>, which are functionally equivalent to TAP blocks
 * (flag + payload + checksum) wrapped in a TZX container with a configurable
 * inter-block pause.
 * <p>
 * By default the builder enforces that the <em>first</em> entry is a BASIC
 * Program block — this mirrors how real tapes work (the ROM loader expects a
 * BASIC program first). Call {@link #allowNonBasicFirstBlock(boolean)
 * allowNonBasicFirstBlock(true)} to override this check.
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * try (var out = Files.newOutputStream(path)) {
 *     new TZXBuilder()
 *         .addBasicLoader("loader", 32767, 32768)
 *         .addCode("game", binData, 32768)
 *         .writeTo(out);
 * }
 * }</pre>
 *
 * @see <a href="https://worldofspectrum.net/TZXformat.html">TZX Format
 *      Specification v1.20</a>
 */
public final class TZXBuilder implements IFormatBuilder {

	/* ---- TZX file header constants ---- */

	/** 7-byte ASCII signature: {@code ZXTape!} */
	private static final byte[] TZX_SIGNATURE = {
		'Z', 'X', 'T', 'a', 'p', 'e', '!'
	};

	/** End-of-text marker byte that follows the signature. */
	private static final byte TZX_EOF_MARKER = 0x1A;

	/** TZX format major version (1). */
	private static final byte TZX_VERSION_MAJOR = 1;

	/** TZX format minor version (20 → v1.20). */
	private static final byte TZX_VERSION_MINOR = 20;

	/* ---- TZX block-type IDs ---- */

	/** ID 0x10 — Standard Speed Data Block. */
	private static final int BLOCK_ID_STANDARD_SPEED = 0x10;

	/** ID 0x11 — Turbo Speed Data Block (not yet implemented). */
	@SuppressWarnings("unused")
	private static final int BLOCK_ID_TURBO_SPEED = 0x11;

	/** ID 0x12 — Pure Tone (not yet implemented). */
	@SuppressWarnings("unused")
	private static final int BLOCK_ID_PURE_TONE = 0x12;

	/** ID 0x15 — Direct Recording (not yet implemented). */
	@SuppressWarnings("unused")
	private static final int BLOCK_ID_DIRECT_RECORDING = 0x15;

	/* ---- Default pause between blocks ---- */

	/** Default inter-block pause in milliseconds (1000 ms = 1 second). */
	public static final int DEFAULT_PAUSE_MS = 1000;

	/* ---- Builder state ---- */

	private final List<TapEncoding.TapEntry> entries = new ArrayList<>();
	private boolean allowNonBasicFirstBlock;
	private int pauseMs = DEFAULT_PAUSE_MS;

	// ------------------------------------------------------------------ builder

	/**
	 * If set to {@code true}, the builder will not throw when the first entry
	 * is not a BASIC Program block. Default is {@code false}.
	 *
	 * @param allow whether to allow a non-BASIC first block
	 * @return this builder for chaining
	 */
	public TZXBuilder allowNonBasicFirstBlock(boolean allow) {
		this.allowNonBasicFirstBlock = allow;
		return this;
	}

	/**
	 * Set the pause duration (in milliseconds) that is written after each
	 * standard-speed data block. Default is {@value #DEFAULT_PAUSE_MS} ms.
	 * <p>
	 * The pause tells the emulator how long to wait before processing the next
	 * block — it corresponds to the silent gap between tape sections.
	 *
	 * @param pauseMs pause in milliseconds (0–65535)
	 * @return this builder for chaining
	 * @throws IllegalArgumentException if {@code pauseMs} is negative or
	 *                                  exceeds 65535
	 */
	public TZXBuilder pauseMs(int pauseMs) {
		if (pauseMs < 0 || pauseMs > 0xFFFF) {
			throw new IllegalArgumentException(
					"Pause must be 0–65535 ms, got: " + pauseMs);
		}
		this.pauseMs = pauseMs;
		return this;
	}

	/**
	 * Returns the current inter-block pause in milliseconds.
	 *
	 * @return pause duration
	 */
	public int pauseMs() {
		return pauseMs;
	}

	// ------------------------------------------------------- IFormatBuilder API

	@Override
	public WellKnownOutputFormat format() {
		return WellKnownOutputFormat.TZX;
	}

	@Override
	public TZXBuilder addCode(String name, byte[] data, int loadAddress) {
		byte[] header = TapEncoding.buildHeader(TapEncoding.TYPE_CODE, name, data.length, loadAddress, 32768);
		entries.add(new TapEncoding.TapEntry(header, data, TapEncoding.TYPE_CODE));
		return this;
	}

	@Override
	public TZXBuilder addBasicLoader(String name, int clearAddress, int startAddress) {
		try {
			byte[] basicProg = TapEncoding.generateLoader(clearAddress, startAddress);
			return addBasicProgram(name, basicProg, 10);
		} catch (IOException e) {
			throw new java.io.UncheckedIOException("Failed to generate BASIC loader", e);
		}
	}

	@Override
	public TZXBuilder addBasicProgram(String name, byte[] basicData, int autoStartLine) {
		byte[] header = TapEncoding.buildHeader(TapEncoding.TYPE_PROGRAM, name, basicData.length, autoStartLine, basicData.length);
		entries.add(new TapEncoding.TapEntry(header, basicData, TapEncoding.TYPE_PROGRAM));
		return this;
	}

	// ------------------------------------------------- future block-type stubs

	/**
	 * Add a Turbo Speed Data Block (TZX ID 0x11).
	 *
	 * @param pilotPulseLen  length of the pilot pulse in T-states
	 * @param syncPulse1     length of the first sync pulse in T-states
	 * @param syncPulse2     length of the second sync pulse in T-states
	 * @param zeroBitPulse   length of a zero-bit pulse in T-states
	 * @param oneBitPulse    length of a one-bit pulse in T-states
	 * @param pilotToneLen   number of pilot pulses
	 * @param usedBitsLast   number of used bits in the last byte (1-8)
	 * @param pauseAfter     pause after this block in ms
	 * @param data           the raw data bytes
	 * @return this builder for chaining
	 * @throws UnsupportedOperationException always — not yet implemented
	 */
	// TODO: Implement Turbo Speed Data Block (ID 0x11)
	public TZXBuilder addTurboSpeedData(int pilotPulseLen, int syncPulse1, int syncPulse2,
			int zeroBitPulse, int oneBitPulse, int pilotToneLen, int usedBitsLast,
			int pauseAfter, byte[] data) {
		throw new UnsupportedOperationException("Turbo Speed Data Block (ID 0x11) not yet implemented");
	}

	/**
	 * Add a Pure Tone block (TZX ID 0x12).
	 *
	 * @param pulseLength length of one pulse in T-states
	 * @param pulseCount  number of pulses
	 * @return this builder for chaining
	 * @throws UnsupportedOperationException always — not yet implemented
	 */
	// TODO: Implement Pure Tone block (ID 0x12)
	public TZXBuilder addPureTone(int pulseLength, int pulseCount) {
		throw new UnsupportedOperationException("Pure Tone block (ID 0x12) not yet implemented");
	}

	/**
	 * Add a Sequence of Pulses block (TZX ID 0x13).
	 *
	 * @param pulseLengths array of pulse lengths in T-states
	 * @return this builder for chaining
	 * @throws UnsupportedOperationException always — not yet implemented
	 */
	// TODO: Implement Pulse Sequence block (ID 0x13)
	public TZXBuilder addPulseSequence(int[] pulseLengths) {
		throw new UnsupportedOperationException("Pulse Sequence block (ID 0x13) not yet implemented");
	}

	/**
	 * Add a Pure Data Block (TZX ID 0x14).
	 *
	 * @param zeroBitPulse length of a zero-bit pulse in T-states
	 * @param oneBitPulse  length of a one-bit pulse in T-states
	 * @param usedBitsLast number of used bits in the last byte (1-8)
	 * @param pauseAfter   pause after this block in ms
	 * @param data         the raw data bytes
	 * @return this builder for chaining
	 * @throws UnsupportedOperationException always — not yet implemented
	 */
	// TODO: Implement Pure Data Block (ID 0x14)
	public TZXBuilder addPureData(int zeroBitPulse, int oneBitPulse, int usedBitsLast,
			int pauseAfter, byte[] data) {
		throw new UnsupportedOperationException("Pure Data Block (ID 0x14) not yet implemented");
	}

	/**
	 * Add a Direct Recording block (TZX ID 0x15).
	 *
	 * @param tStatesPerSample number of T-states per sample
	 * @param pauseAfter       pause after this block in ms
	 * @param usedBitsLast     number of used bits in the last byte (1-8)
	 * @param data             the raw sample data
	 * @return this builder for chaining
	 * @throws UnsupportedOperationException always — not yet implemented
	 */
	// TODO: Implement Direct Recording block (ID 0x15)
	public TZXBuilder addDirectRecording(int tStatesPerSample, int pauseAfter,
			int usedBitsLast, byte[] data) {
		throw new UnsupportedOperationException("Direct Recording block (ID 0x15) not yet implemented");
	}

	/**
	 * Add a Pause / Stop-the-Tape block (TZX ID 0x20).
	 *
	 * @param pauseMs pause duration in ms (0 = stop the tape)
	 * @return this builder for chaining
	 * @throws UnsupportedOperationException always — not yet implemented
	 */
	// TODO: Implement Pause / Stop-the-Tape block (ID 0x20)
	public TZXBuilder addPause(int pauseMs) {
		throw new UnsupportedOperationException("Pause / Stop-the-Tape block (ID 0x20) not yet implemented");
	}

	/**
	 * Add an Archive Info block (TZX ID 0x32) with human-readable metadata.
	 *
	 * @param title   the tape title (may be {@code null})
	 * @param author  the author name (may be {@code null})
	 * @param comment a free-form comment (may be {@code null})
	 * @return this builder for chaining
	 * @throws UnsupportedOperationException always — not yet implemented
	 */
	// TODO: Implement Archive Info block (ID 0x32)
	public TZXBuilder addArchiveInfo(String title, String author, String comment) {
		throw new UnsupportedOperationException("Archive Info block (ID 0x32) not yet implemented");
	}

	// --------------------------------------------------------- TZX serialisation

	@Override
	public void writeTo(OutputStream out) throws IOException {
		if (entries.isEmpty()) {
			throw new IllegalStateException("No entries have been added to the TZX builder.");
		}

		if (!allowNonBasicFirstBlock && entries.get(0).fileType() != TapEncoding.TYPE_PROGRAM) {
			throw new IllegalStateException(
					"The first block in a TZX file must be a BASIC Program. "
					+ "Call allowNonBasicFirstBlock(true) to override this check.");
		}

		// ---- TZX file header (10 bytes) ----
		out.write(TZX_SIGNATURE);      // 7 bytes: "ZXTape!"
		out.write(TZX_EOF_MARKER);     // 1 byte:  0x1A
		out.write(TZX_VERSION_MAJOR);  // 1 byte:  major version
		out.write(TZX_VERSION_MINOR);  // 1 byte:  minor version

		// ---- Data blocks ----
		for (int i = 0; i < entries.size(); i++) {
			var entry = entries.get(i);
			boolean lastEntry = (i == entries.size() - 1);

			// Header block (flag 0x00) — always uses the configured pause
			writeStandardSpeedBlock(out, (byte) 0x00, entry.headerBlock(), pauseMs);

			// Data block (flag 0xFF) — last block on tape must have pause = 0
			// per the TZX spec ("For the last block on the tape the pause
			// should be set to 0"), which FUSE/libspectrum and MAME enforce.
			writeStandardSpeedBlock(out, (byte) 0xFF, entry.dataBlock(),
					lastEntry ? 0 : pauseMs);
		}
	}

	/**
	 * Write a TZX ID 0x10 — Standard Speed Data Block.
	 * <p>
	 * Wire format:
	 * <pre>
	 *   1 byte    Block ID (0x10)
	 *   2 bytes   Pause after this block in ms (LE)
	 *   2 bytes   Data length (LE) = 1 (flag) + payload.length + 1 (checksum)
	 *   N bytes   Data: flag + payload + checksum
	 * </pre>
	 *
	 * @param out          target stream
	 * @param flag         TAP flag byte (0x00 = header, 0xFF = data)
	 * @param payload      the block payload (without flag or checksum)
	 * @param blockPauseMs pause after this specific block in ms
	 */
	private static void writeStandardSpeedBlock(OutputStream out, byte flag,
			byte[] payload, int blockPauseMs) throws IOException {
		byte[] blockData = TapEncoding.assembleTapBlockBytes(flag, payload);

		// Block ID
		out.write(BLOCK_ID_STANDARD_SPEED);

		// Pause after block (2 bytes LE)
		out.write(blockPauseMs & 0xFF);
		out.write((blockPauseMs >> 8) & 0xFF);

		// Data length (2 bytes LE)
		int dataLen = blockData.length;
		out.write(dataLen & 0xFF);
		out.write((dataLen >> 8) & 0xFF);

		// Block data (flag + payload + checksum)
		out.write(blockData);
	}
}
