package uk.co.bithatch.zxbasic.preprocessor;

public enum Warning {
	UNKNOWN_PREPROCESSOR_DIRECTIVE(2000),
	WARNING_DIRECTIVE(0),
	MACRO_REDEFINED(510);
	
	private int code;

	Warning(int code) {
		this.code = code;
	}
	
	public String description() {
		switch(this) {
		case WARNING_DIRECTIVE:
			return "Warning Directive";
		case MACRO_REDEFINED:
			return "Macro redefined";
		default:
			return name();
		}
	}
	
	public int code() {
		return code;
	}
}