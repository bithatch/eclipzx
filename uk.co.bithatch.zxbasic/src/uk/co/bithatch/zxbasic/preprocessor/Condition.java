package uk.co.bithatch.zxbasic.preprocessor;

final class Condition {
	private boolean isTrue;
	public boolean inElse;
	
	Condition(boolean isTrue) {
		super();
		this.isTrue = isTrue;
	}

	public boolean matches() {
		if(inElse)
			return !isTrue;
		else
			return isTrue;
	}
	
	
}