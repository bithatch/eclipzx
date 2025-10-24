package uk.co.bithatch.zxbasic.preprocessor;

@SuppressWarnings("serial")
public final class PreprocessorParseException extends RuntimeException {
	private final int lineNumber;
	private final String uri;

	PreprocessorParseException(String uri, int lineNumber, String message, Throwable cause) {
		super(message, cause);
		this.uri = uri;
		this.lineNumber = lineNumber;
	}

	private PreprocessorParseException(String uri, int lineNumber, String message) {
		super(message);
		this.uri = uri;
		this.lineNumber = lineNumber;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String getUri() {
		return uri;
	}
}