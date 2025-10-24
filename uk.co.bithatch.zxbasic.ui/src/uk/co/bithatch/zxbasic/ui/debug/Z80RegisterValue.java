package uk.co.bithatch.zxbasic.ui.debug;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public final class Z80RegisterValue extends DelegatingDebugElement implements IValue {
	
	private String valStr;

	Z80RegisterValue(Z80Register register, String valStr) {
		super(register);
		this.valStr = valStr;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return "Val";
	}

	@Override
	public String getValueString() throws DebugException {
		return valStr;
	}

	@Override
	public boolean isAllocated() throws DebugException {
		return true;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		return new IVariable[0];
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return false;
	}
	
}