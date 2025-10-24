package uk.co.bithatch.zxbasic.ui.debug;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

public final class Z80Thread extends DelegatingDebugElement implements IThread {
	
	
	private IStackFrame stackFrame;

	Z80Thread(IDebugTarget delegate) {
		super(delegate);
		stackFrame = updateStackFrame();
	}

	@Override
	protected DezogDebugTarget delegate() {
		return (DezogDebugTarget) super.delegate();
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
	public void stepReturn() throws DebugException {
		// TODO Auto-generated method stub
	}

	@Override
	public void stepOver() throws DebugException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stepInto() throws DebugException {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isStepping() {
		return delegate().isSuspended();
	}

	@Override
	public boolean canStepReturn() {
		// TODO Auto-generated method stub
		return isStepping();
	}

	@Override
	public boolean canStepOver() {
		// TODO Auto-generated method stub
		return isStepping();
	}

	@Override
	public boolean canStepInto() {
		// TODO Auto-generated method stub
		return isStepping();
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
		return true;
	}

	@Override
	public boolean canResume() {
		return true;
	}

	@Override
	public boolean hasStackFrames() throws DebugException {
		return true;
	}

	@Override
	public IStackFrame getTopStackFrame() throws DebugException {
		return getStackFrames()[0];
	}

	@Override
	public IStackFrame[] getStackFrames() throws DebugException {
		return new IStackFrame[] {
			stackFrame
		};
	}

	@Override
	public int getPriority() throws DebugException {
		return 0;
	}

	@Override
	public String getName() throws DebugException {
		return "Z80MainThread";
	}

	@Override
	public IBreakpoint[] getBreakpoints() {
		// TODO
		return new IBreakpoint[0];
	}
	
	private IStackFrame updateStackFrame() {
		var regs = delegate().dezog().registers();
		return new Z80StackFrame(this, regs);
	}
}