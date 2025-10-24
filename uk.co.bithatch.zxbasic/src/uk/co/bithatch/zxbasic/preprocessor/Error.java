package uk.co.bithatch.zxbasic.preprocessor;

public enum Error {
	UNKNOWN_PREPROCESS_DIRECTIVE(2000),
	SYNTAX_ERROR(2001),
	MISSING_INCLUDE(2002),
	ERROR_DIRECTIVE(0);
	
	private int code;

	Error(int code) {
		this.code = code;
	}
	
	public String description() {
		switch(this) {
		case ERROR_DIRECTIVE:
			return "Error directive";
		default:
			return name();
		}
	}
	
	public int code() {
		return code;
	}
}