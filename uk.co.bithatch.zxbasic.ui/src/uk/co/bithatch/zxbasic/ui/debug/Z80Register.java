package uk.co.bithatch.zxbasic.ui.debug;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegister;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IValue;

public final class Z80Register extends DelegatingDebugElement implements IRegister {
	
	private String name;
	private String typeName;
	private IValue value;

	Z80Register(String name, String typeName, Z80RegisterGroup group) {
		super(group);
		this.name = name;
		this.typeName = typeName;
		value = new Z80RegisterValue(this, "");
	}

	@Override
	public IValue getValue() throws DebugException {
		return value;
	}

	@Override
	public String getName() throws DebugException {
		return name;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return typeName;
	}

	@Override
	public boolean hasValueChanged() throws DebugException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setValue(String expression) throws DebugException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setValue(IValue value) throws DebugException {
		this.value = value;
	}

	@Override
	public boolean supportsValueModification() {
		return true;
	}

	@Override
	public boolean verifyValue(String expression) throws DebugException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean verifyValue(IValue value) throws DebugException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IRegisterGroup getRegisterGroup() throws DebugException {
		return (IRegisterGroup)delegate();
	}

	public void setValue(int value) {
		this.value = new Z80RegisterValue(this, String.valueOf(value));
	}
	
}