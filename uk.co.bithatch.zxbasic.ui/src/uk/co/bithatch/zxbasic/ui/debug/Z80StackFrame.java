package uk.co.bithatch.zxbasic.ui.debug;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

import uk.co.bithatch.zyxy.dezog.Command.Register;
import uk.co.bithatch.zyxy.dezog.Command.Registers;

public final class Z80StackFrame extends DelegatingDebugElement implements IStackFrame {
	
	private Z80RegisterGroup z80registers;
	
	Z80StackFrame(IThread delegate, Registers registerValue) {
		super(delegate);
		z80registers = new Z80RegisterGroup(this);
		

//		public record GetRegistersResult(int pc, int sp, int af, int bc, int de, int hl, int ix, int iy, int af2, int bc2, int de2, int hl2, int r, int i, int im, int reserved, int[] slots) implements Result  {
		
		
		z80registers.addRegister("PC", "Program Counter").setValue(registerValue.values().get(Register.PC));
		z80registers.addRegister("SP", "Stack Pointer").setValue(registerValue.values().get(Register.SP));
		z80registers.addRegister("AF", "Pair").setValue(registerValue.values().get(Register.AF));
		z80registers.addRegister("BC", "Pair").setValue(registerValue.values().get(Register.BC));
		z80registers.addRegister("DE", "Pair").setValue(registerValue.values().get(Register.DE));
		z80registers.addRegister("HL", "Pair").setValue(registerValue.values().get(Register.HL));
		z80registers.addRegister("IX", "Pair").setValue(registerValue.values().get(Register.IX));
		z80registers.addRegister("IY", "Pair").setValue(registerValue.values().get(Register.IY));
	}
	
	@Override
	protected IThread delegate() {
		return (IThread) super.delegate();
	}

	@Override
	public void terminate() throws DebugException {
		delegate().terminate();
	}

	@Override
	public boolean isTerminated() {
		return delegate().isTerminated();
	}

	@Override
	public boolean canTerminate() {
		return delegate().canTerminate();
	}

	@Override
	public void suspend() throws DebugException {
		delegate().suspend();
	}

	@Override
	public void resume() throws DebugException {
		delegate().resume();
	}

	@Override
	public boolean isSuspended() {
		return delegate().isSuspended();
	}

	@Override
	public boolean canSuspend() {
		return delegate().canSuspend();
	}

	@Override
	public boolean canResume() {
		return delegate().canResume();
	}

	@Override
	public void stepReturn() throws DebugException {
		delegate().stepReturn();
	}

	@Override
	public void stepOver() throws DebugException {
		delegate().stepOver();				
	}

	@Override
	public void stepInto() throws DebugException {
		delegate().stepInto();				
	}

	@Override
	public boolean isStepping() {
		return delegate().isStepping();				
	}

	@Override
	public boolean canStepReturn() {
		return delegate().canStepReturn();
	}

	@Override
	public boolean canStepOver() {
		return delegate().canStepOver();
	}

	@Override
	public boolean canStepInto() {
		return delegate().canStepInto();
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return false;
	}

	@Override
	public boolean hasRegisterGroups() throws DebugException {
		return true;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		// TODO Auto-generated method stub
		return new IVariable[0];
	}

	@Override
	public IThread getThread() {
		return delegate();
	}

	@Override
	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return new IRegisterGroup[] {
			z80registers
		};
	}

	@Override
	public String getName() throws DebugException {
		return delegate().getName();
	}

	@Override
	public int getLineNumber() throws DebugException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCharStart() throws DebugException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCharEnd() throws DebugException {
		// TODO Auto-generated method stub
		return 0;
	}
}