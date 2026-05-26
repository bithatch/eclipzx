package uk.co.bithatch.emuzx;

import java.nio.file.Path;
import java.util.Optional;

public interface INEXConfiguration {

	void extraFile(Path file);

	void scr(Path file);

	void bmp(Path file, boolean use8bitPalette, boolean dontSavePalette);
	
	void loading(int border, int bar1, int bar2, int delay1,
			int delay2);

	void slr(Path file);

	void sl2(Path file);

	void nxi(Path file);

	void shr(Path file);

	void shc(Path file);

	void mmu(Path file, int bank);

	void mmu(Path file, int bank, int address);

	void mmu(Path file, Optional<Integer> bank8k, Optional<Integer> address8k);

	void addFile(Path file, Optional<Integer> bank, Optional<Integer> address);

	void addFile(Path file, Optional<Integer> bank, Optional<Integer> address, int[] SNA_Bank);

	void entryBank(int bank);

	void pcsp(int pc);

	void pcsp(int pc, Optional<Integer> sp);

	void pcsp(int pc, Optional<Integer> sp, Optional<Integer> bank);

	void core(String core);

	void core(int... core);

	boolean hasContent();

}