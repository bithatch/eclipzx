package uk.co.bithatch.bitzx;

public interface IDescribed  {
	
	/**
	 * Name. MUST be all upper case like a constants
	 * 
	 * @return name
	 */
	String name();
	
	default String description() {
		return name();
	}
	
	default String fullDescription() {
		return description();
	}
}
