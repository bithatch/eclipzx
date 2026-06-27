package uk.co.bithatch.eclipz88dk.toolchain;

import java.util.Set;

/**
 * Unified parser for z88dk object, archive, and linked output files.
 */
public class Z88DKBinaryParser extends AbstractZ88DKBinaryParser {

	private static final Set<String> EXTENSIONS = Set.of("o", "obj", "lib", "bin");
	
	@Override
	protected Set<String> supportedExtensions() {
		return EXTENSIONS;
	}

	@Override
	public String getFormat() {
		return "Z88DK (z88dk-z80nm)";
	}
}
