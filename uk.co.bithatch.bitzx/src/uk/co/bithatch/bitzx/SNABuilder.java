package uk.co.bithatch.bitzx;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Placeholder builder for the ZX Spectrum <strong>.SNA</strong> snapshot
 * format.
 * <p>
 * This stub establishes the extension pattern for future format support (SNA,
 * TZX, Z80 snapshot, etc.). All mutating methods currently throw
 * {@link UnsupportedOperationException}.
 */
public final class SNABuilder implements IFormatBuilder {

	@Override
	public WellKnownOutputFormat format() {
		return WellKnownOutputFormat.SNA;
	}

	@Override
	public SNABuilder addCode(String name, byte[] data, int loadAddress) {
		throw new UnsupportedOperationException("SNA format not yet implemented");
	}

	@Override
	public SNABuilder addBasicLoader(String name, int clearAddress, int startAddress) {
		throw new UnsupportedOperationException("SNA format not yet implemented");
	}

	@Override
	public SNABuilder addBasicProgram(String name, byte[] basicData, int autoStartLine) {
		throw new UnsupportedOperationException("SNA format not yet implemented");
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		throw new UnsupportedOperationException("SNA format not yet implemented");
	}
}
