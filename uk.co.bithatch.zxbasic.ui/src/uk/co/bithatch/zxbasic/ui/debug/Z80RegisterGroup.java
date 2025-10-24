package uk.co.bithatch.zxbasic.ui.debug;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegister;
import org.eclipse.debug.core.model.IRegisterGroup;

public final class Z80RegisterGroup extends DelegatingDebugElement implements IRegisterGroup {

	private List<Z80Register> registers = new ArrayList<>();

	Z80RegisterGroup(Z80StackFrame delegate) {
		super(delegate);
	}
	
	Z80Register addRegister(String name, String type) {
		var reg = new Z80Register(name, type, this);
		registers.add(reg);
		return reg;
	}

	@Override
	public String getName() throws DebugException {
		return "Z80 Registers";
	}

	@Override
	public IRegister[] getRegisters() throws DebugException {
		return registers.toArray(new IRegister[0]);
	}

	@Override
	public boolean hasRegisters() throws DebugException {
		return true;
	}
	
}