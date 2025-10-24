package uk.co.bithatch.drawzx.widgets;

public enum Brush {
	CIRCLE,
	SQUARE,
	SPRAY,
	BLOCK;
	
	String toIkon() {
		switch(this) {
		case CIRCLE:
			return "icons/circle16.png";
		case SPRAY:
			return "icons/spray16.png";
		case BLOCK:
			return "icons/block16.png";
		default:
			return "icons/square16.png";
		}
	}
	
	int smallest() {
		return 1;
	}
	
	int largest() {
		switch(this) {
		case BLOCK:
			return 1;
		case SPRAY:
			return 32;
		default:
			return 16;
		}
	}
}