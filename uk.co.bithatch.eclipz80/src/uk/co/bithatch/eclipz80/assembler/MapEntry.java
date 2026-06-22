package uk.co.bithatch.eclipz80.assembler;

/**
 * An entry in the line-to-address map.
 */
public class MapEntry {
	final String fileName;
	final int lineNumber;
	final long address;

	MapEntry(String fileName, int lineNumber, long address) {
		this.fileName = fileName;
		this.lineNumber = lineNumber;
		this.address = address;
	}

	public String getFileName() { return fileName; }
	public int getLineNumber() { return lineNumber; }
	public long getAddress() { return address; }
}