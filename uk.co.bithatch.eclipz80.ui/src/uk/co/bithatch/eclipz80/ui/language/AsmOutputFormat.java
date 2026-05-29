package uk.co.bithatch.eclipz80.ui.language;

import uk.co.bithatch.bitzx.IOutputFormat;

public enum AsmOutputFormat implements IOutputFormat {
	BIN;

	public boolean snapshot() {
		return false;
	}


	@Override
	public String fullDescription() {
		return  description() + " (*." + name().toLowerCase() + ")" ;
	}

	@Override
	public String description() {
		switch(this) {
		case BIN:
			return "Z80 Assembler Binary";
		default:
			return name() + " Format";
		}
	}

}
