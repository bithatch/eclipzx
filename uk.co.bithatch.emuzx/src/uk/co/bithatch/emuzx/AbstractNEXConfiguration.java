package uk.co.bithatch.emuzx;

import java.nio.file.Path;
import java.util.Optional;

public abstract class AbstractNEXConfiguration implements INEXConfiguration {

	@Override
	public final void pcsp(int pc) {
		pcsp(pc, Optional.empty());
	}

	@Override
	public final void pcsp(int pc, Optional<Integer> sp) {
		pcsp(pc, sp, Optional.empty());
	}

	@Override
	public final void core(String core) {
		var a = core.split("\\.");
		var v = new int[a.length];
		for (var i = 0; i < v.length; i++) {
			v[i] = Integer.parseInt(a[i]);
		}
		core(v);
	}

	@Override
	public final void addFile(Path file, Optional<Integer> bank, Optional<Integer> address) {
		addFile(file, bank, address, new int[256]);
	}

	@Override
	public final void mmu(Path file, int bank) {
		mmu(file, Optional.of(bank), Optional.empty());
	}

	@Override
	public final void mmu(Path file, int bank, int address) {
		mmu(file, Optional.of(bank), Optional.of(address));
	}
}
