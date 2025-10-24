package uk.co.bithatch.zxbasic.interpreter;

public interface Z80IO {

	void out(int address, int value);
	
	int in(int address);
}
