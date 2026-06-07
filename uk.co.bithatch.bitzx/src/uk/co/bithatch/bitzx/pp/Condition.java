package uk.co.bithatch.bitzx.pp;

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