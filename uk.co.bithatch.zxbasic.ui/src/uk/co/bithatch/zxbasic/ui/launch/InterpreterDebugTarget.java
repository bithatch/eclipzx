package uk.co.bithatch.zxbasic.ui.launch;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import uk.co.bithatch.zxbasic.interpreter.ZXBasicInterpreter;
import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;

public class InterpreterDebugTarget extends DebugElement implements IDebugTarget {

    private final ZXBasicInterpreter process;
    private final ILaunch launch;
	private boolean terminated;

    public InterpreterDebugTarget(ILaunch launch, ZXBasicInterpreter process) {
        super(null);
        this.process = process;
        this.launch = launch;
    }

    @Override
    public String getName() {
        return "Interpreter Debug Target";
    }

    @Override
    public IProcess getProcess() {
        return null;
    }

    @Override
    public ILaunch getLaunch() {
        return launch;
    }

    @Override
    public boolean canTerminate() {
        return true;
    }

    @Override
    public boolean isTerminated() {
   		return terminated;
    }

    @Override
    public void terminate() throws DebugException {
    	if(terminated)
    		return;
		terminated = true;
        process.terminate();
    }

    // --- Unused for now ---
    @Override public boolean canResume() { return false; }
    @Override public boolean canSuspend() { return false; }
    @Override public boolean isSuspended() { return false; }
    @Override public void resume() throws DebugException {}
    @Override public void suspend() throws DebugException {}
    @Override public IDebugTarget getDebugTarget() { return this; }
    @Override public boolean hasThreads() { return false; }
    @Override public IThread[] getThreads() throws DebugException { return new IThread[0]; }
    @Override public boolean supportsBreakpoint(IBreakpoint breakpoint) { return false; }

	@Override
	public String getModelIdentifier() {
		return ZXBasicUiActivator.PLUGIN_ID;
	}

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	@Override
	public boolean canDisconnect() {
		return false;
	}

	@Override
	public void disconnect() throws DebugException {
	}

	@Override
	public boolean isDisconnected() {
		return false;
	}

	@Override
	public boolean supportsStorageRetrieval() {
		return false;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
		return null;
	}
}
