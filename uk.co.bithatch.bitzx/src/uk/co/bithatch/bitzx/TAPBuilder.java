package uk.co.bithatch.bitzx;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
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

	private final List<TapEncoding.TapEntry> entries = new ArrayList<>();
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
		byte[] header = TapEncoding.buildHeader(TapEncoding.TYPE_CODE, name, data.length, loadAddress, 32768);
		entries.add(new TapEncoding.TapEntry(header, data, TapEncoding.TYPE_CODE));
		return this;
	}

	@Override
	public TAPBuilder addBasicLoader(String name, int clearAddress, int startAddress) {
		try {
			byte[] basicProg = TapEncoding.generateLoader(clearAddress, startAddress);
			return addBasicProgram(name, basicProg, 10);
		} catch (IOException e) {
			throw new java.io.UncheckedIOException("Failed to generate BASIC loader", e);
		}
	}

	@Override
	public TAPBuilder addBasicProgram(String name, byte[] basicData, int autoStartLine) {
		byte[] header = TapEncoding.buildHeader(TapEncoding.TYPE_PROGRAM, name, basicData.length, autoStartLine, basicData.length);
		entries.add(new TapEncoding.TapEntry(header, basicData, TapEncoding.TYPE_PROGRAM));
		return this;
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		if (entries.isEmpty()) {
			throw new IllegalStateException("No entries have been added to the TAP builder.");
		}

		if (!allowNonBasicFirstBlock && entries.get(0).fileType() != TapEncoding.TYPE_PROGRAM) {
			throw new IllegalStateException(
					"The first block in a TAP file must be a BASIC Program. "
					+ "Call allowNonBasicFirstBlock(true) to override this check.");
		}

		for (var entry : entries) {
			TapEncoding.writeTapBlock(out, (byte) 0x00, entry.headerBlock());
			TapEncoding.writeTapBlock(out, (byte) 0xFF, entry.dataBlock());
		}
	}
}