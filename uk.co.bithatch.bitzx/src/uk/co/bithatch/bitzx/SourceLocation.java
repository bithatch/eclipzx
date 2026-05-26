package uk.co.bithatch.bitzx;

public record SourceLocation(String fileName, int line) {
	@Override
	public String toString() {
		return fileName + ":" + line;
	}
}