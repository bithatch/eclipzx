package uk.co.bithatch.eclipzpp;

final class Condition {
	private boolean branchTaken;
	private boolean active;
	public boolean inElse;
	
	Condition(boolean isTrue) {
		super();
		this.active = isTrue;
		this.branchTaken = isTrue;
	}

	public boolean matches() {
		return active;
	}

	public void enterElse() {
		inElse = true;
		active = !branchTaken;
		if(active) {
			branchTaken = true;
		}
	}

	public void applyElif(boolean isTrue) {
		if(branchTaken) {
			active = false;
		}
		else {
			active = isTrue;
			if(isTrue) {
				branchTaken = true;
			}
		}
	}
	
	
}