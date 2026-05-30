package uk.co.bithatch.bitzx;

public enum WellKnownOutputFormat {
	BIN, SNA, TAP, TZX, Z80, NEX;
	
	public String extension() {
		return name().toLowerCase();
	}
}
