package uk.co.bithatch.emuzx.debug;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

import java.io.IOException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;

/**
 * Z80 thread implementation for GDB-based debugging.
 * Unlike {@link Z80Thread} which depends on {@link DezogDebugTarget},
 * this delegates to the generic {@link IDebugTarget} interface.
 */
public final class GdbZ80Thread extends DelegatingDebugElement implements IThread {

	private static final ILog LOG = ILog.of(GdbZ80Thread.class);

	private final GdbRspClient rsp;
	private final Z88dkDebugInfoParser debugInfo;
	private volatile boolean stepping = false;

	GdbZ80Thread(IDebugTarget delegate, GdbRspClient rsp, Z88dkDebugInfoParser debugInfo) {
		super(delegate);
		this.rsp = rsp;
		this.debugInfo = debugInfo;
	}

	private IDebugTarget target() {
		return (IDebugTarget) super.delegate();
	}

	@Override
	public void terminate() throws DebugException {
		target().terminate();
	}

	@Override
	public boolean isTerminated() {
		return target().isTerminated();
	}

	@Override
	public boolean canTerminate() {
		return target().canTerminate();
	}

	@Override
	public void stepReturn() throws DebugException {
		/* Not supported for Z80 — would need RET detection */
	}

	@Override
	public void stepOver() throws DebugException {
		/* For now, step-over behaves like step-into (single instruction) */
		stepInto();
	}

	@Override
	public void stepInto() throws DebugException {
		if (target().isSuspended() && !target().isTerminated()) {
			try {
				stepping = true;
				DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {
					new DebugEvent(this, DebugEvent.RESUME, DebugEvent.STEP_INTO)
				});
				/* Send step and read stop reply synchronously to avoid desync.
				 * The step command completes very quickly (single instruction). */
				var stopReply = rsp.sendCommand("s");
				stepping = false;
				DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {
					new DebugEvent(this, DebugEvent.SUSPEND, DebugEvent.STEP_END)
				});
			} catch (IOException e) {
				stepping = false;
				throw new DebugException(Status.error("Failed to step", e));
			}
		}
	}

	@Override
	public boolean isStepping() {
		return stepping;
	}

	void setStepping(boolean stepping) {
		this.stepping = stepping;
	}

	@Override
	public boolean canStepReturn() {
		return false;
	}

	@Override
	public boolean canStepOver() {
		return target().isSuspended() && !target().isTerminated();
	}

	@Override
	public boolean canStepInto() {
		return target().isSuspended() && !target().isTerminated();
	}

	@Override
	public void suspend() throws DebugException {
		target().suspend();
	}

	@Override
	public void resume() throws DebugException {
		target().resume();
	}

	@Override
	public boolean isSuspended() {
		return target().isSuspended();
	}

	@Override
	public boolean canSuspend() {
		return target().canSuspend();
	}

	@Override
	public boolean canResume() {
		return target().canResume();
	}

	@Override
	public boolean hasStackFrames() throws DebugException {
		return isSuspended();
	}

	@Override
	public IStackFrame getTopStackFrame() throws DebugException {
		if (isSuspended()) {
			try {
				return new GdbZ80StackFrame(this, rsp, debugInfo);
			} catch (Exception e) {
				LOG.error("Failed to create stack frame", e);
			}
		}
		return null;
	}

	@Override
	public IStackFrame[] getStackFrames() throws DebugException {
		var top = getTopStackFrame();
		return top != null ? new IStackFrame[] { top } : new IStackFrame[0];
	}

	@Override
	public int getPriority() throws DebugException {
		return 0;
	}

	@Override
	public String getName() throws DebugException {
		return "Z80 (GDB)";
	}

	@Override
	public IBreakpoint[] getBreakpoints() {
		return new IBreakpoint[0];
	}
}
