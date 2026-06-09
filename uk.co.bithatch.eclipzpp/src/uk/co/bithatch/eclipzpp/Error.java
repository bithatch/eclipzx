package uk.co.bithatch.eclipzpp;

public enum Error {
	UNKNOWN_PREPROCESS_DIRECTIVE(2000),
	SYNTAX_ERROR(2001),
	MISSING_INCLUDE(2002),
	ASSERT_FAILED(2003),
	ERROR_DIRECTIVE(0);
	
	private int code;

	Error(int code) {
		this.code = code;
	}
	
	public String description() {
		switch(this) {
		case ERROR_DIRECTIVE:
			return "Error directive";
		case ASSERT_FAILED:
			return "Assertion failed";
		default:
			return name();
		}
	}
	
	public int code() {
		return code;
	}
}