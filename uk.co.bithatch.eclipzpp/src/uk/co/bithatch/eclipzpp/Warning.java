package uk.co.bithatch.eclipzpp;

public enum Warning {
	UNKNOWN_PREPROCESSOR_DIRECTIVE(2000),
	WARNING_DIRECTIVE(0),
	MACRO_REDEFINED(510),
	UNDEFINED_EXPRESSION_SYMBOL(511),
	CIRCULAR_EXPRESSION_SYMBOL(512);
	
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
		case UNDEFINED_EXPRESSION_SYMBOL:
			return "Undefined expression symbol";
		case CIRCULAR_EXPRESSION_SYMBOL:
			return "Circular expression symbol";
		default:
			return name();
		}
	}
	
	public int code() {
		return code;
	}
}