package uk.co.bithatch.bitzx;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Common interface for builders that convert raw Z80 opcode .BIN files into
 * runnable emulator formats (TAP, SNA, TZX, etc.).
 * <p>
 * Implementations use the builder pattern — configure the output via fluent
 * method calls, then serialise the result to an {@link OutputStream} with
 * {@link #writeTo(OutputStream)}.
 * <p>
 * A TAP file, for example, can contain multiple entries (a BASIC loader
 * followed by one or more CODE blocks). The builder accumulates entries in
 * order and writes them all when {@code writeTo} is called.
 */
public interface IFormatBuilder {

	/**
	 * The well-known output format this builder produces.
	 *
	 * @return the format identifier, never {@code null}
	 */
	WellKnownOutputFormat format();

	/**
	 * Add a raw machine-code (CODE) block.
	 *
	 * @param name        logical filename (will be truncated/padded to format
	 *                    limits, e.g. 10 chars for TAP)
	 * @param data        the raw binary data
	 * @param loadAddress the address at which the code should be loaded
	 * @return this builder for chaining
	 */
	IFormatBuilder addCode(String name, byte[] data, int loadAddress);

	/**
	 * Add a minimal BASIC auto-loader program that CLEARs memory, LOADs the
	 * next CODE block from tape and jumps to it via {@code RANDOMIZE USR}.
	 * <p>
	 * The generated program is equivalent to:
	 * <pre>
	 *   10 CLEAR &lt;clearAddress&gt;
	 *   20 LOAD "" CODE
	 *   30 RANDOMIZE USR &lt;startAddress&gt;
	 * </pre>
	 *
	 * @param name         logical filename for the BASIC program
	 * @param clearAddress the address passed to CLEAR (typically
	 *                     {@code startAddress - 1})
	 * @param startAddress the address passed to RANDOMIZE USR
	 * @return this builder for chaining
	 */
	IFormatBuilder addBasicLoader(String name, int clearAddress, int startAddress);

	/**
	 * Add a raw BASIC program block. The caller is responsible for providing
	 * correctly tokenised Sinclair BASIC.
	 *
	 * @param name          logical filename
	 * @param basicData     tokenised BASIC program bytes
	 * @param autoStartLine the line number to auto-run (or &gt;= 32768 for none)
	 * @return this builder for chaining
	 */
	IFormatBuilder addBasicProgram(String name, byte[] basicData, int autoStartLine);

	/**
	 * Serialise all accumulated entries to the given output stream.
	 * <p>
	 * The stream is <em>not</em> closed by this method.
	 *
	 * @param out the target stream
	 * @throws IOException           on I/O failure
	 * @throws IllegalStateException if the builder state is invalid (e.g. first
	 *                               block is not a BASIC program and
	 *                               non-BASIC-first was not explicitly allowed)
	 */
	void writeTo(OutputStream out) throws IOException;
}
