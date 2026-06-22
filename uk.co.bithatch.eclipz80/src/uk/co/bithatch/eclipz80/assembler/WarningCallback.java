package uk.co.bithatch.eclipz80.assembler;

/**
 * Callback for non-fatal warnings emitted during assembly.
 */
@FunctionalInterface
public interface WarningCallback {
	void warn(String filename, int line, String warning);
}