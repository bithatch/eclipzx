package uk.co.bithatch.eclipz80.assembler;

/**
 * Thrown when the assembler encounters a fatal error (e.g. an
 * unimplemented instruction or directive).
 */
public class AssemblyException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final String filename;
	private final int line;

	public AssemblyException(String filename, int line, String message) {
		super(filename + ":" + line + ": " + message);
		this.filename = filename;
		this.line = line;
	}

	public String getFilename() { return filename; }
	public int getLine() { return line; }
}