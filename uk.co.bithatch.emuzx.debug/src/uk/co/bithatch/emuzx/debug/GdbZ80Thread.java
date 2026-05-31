package uk.co.bithatch.emuzx.debug;

import java.io.IOException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

import uk.co.bithatch.bitzx.ISourceAdressMap;

/**
 * Z80 thread implementation for GDB-based debugging.
 * Unlike {@link Z80Thread} which depends on {@link DezogDebugTarget},
 * this delegates to the generic {@link IDebugTarget} interface.
 */
public final class GdbZ80Thread extends DelegatingDebugElement implements IThread {

	private static final ILog LOG = ILog.of(GdbZ80Thread.class);

	private final GdbRspClient rsp;
	private final ISourceAdressMap debugInfo;
	private volatile boolean stepping = false;
	
	/* 
	 * Eclipse's Debug View uses object identity to maintain tree selection and
	 * expansion state across suspend/resume cycles.  We keep ONE frame instance
	 * for the entire session and update it in-place when the target suspends.
	 * A dirty flag tells getTopStackFrame() to re-read registers/source.
	 */
	private final GdbZ80StackFrame currentFrame;
	private volatile boolean frameDirty = true;

	GdbZ80Thread(IDebugTarget delegate, GdbRspClient rsp, ISourceAdressMap debugInfo) {
		super(delegate);
		this.rsp = rsp;
		this.debugInfo = debugInfo;
		this.currentFrame = new GdbZ80StackFrame(this, rsp, debugInfo);
	}

	private GdbDebugTarget target() {
		return (GdbDebugTarget) super.delegate();
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
				LOG.info("Stepping into...");
				stepping = true;
				frameDirty = true;

				/* Transition to "not suspended" so the event is consistent
				 * with what isSuspended() returns when Eclipse processes it. */
				target().setSuspended(false);
				DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {
					new DebugEvent(this, DebugEvent.RESUME, DebugEvent.STEP_INTO)
				});
				
				/* Send step and read stop reply synchronously.
				 * The step command completes very quickly (single instruction).
				 * The waitForStopLoop is guarded by the 'stepping' flag so it
				 * will not compete for the socket. */
				rsp.sendCommand("s");

				/* Back to suspended */
				target().setSuspended(true);
				stepping = false;

				/* Update the frame in-place BEFORE firing events so Eclipse
				 * sees the new PC / line number when it queries the model. */
				currentFrame.update();
				frameDirty = false;

				DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {
					new DebugEvent(this, DebugEvent.SUSPEND, DebugEvent.STEP_END)
				});
				/* Tell Eclipse the frame content changed so it refreshes
				 * source highlighting even though the frame object is the same. */
				DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {
					new DebugEvent(currentFrame, DebugEvent.CHANGE, DebugEvent.CONTENT)
				});
				
			} catch (IOException e) {
				stepping = false;
				target().setSuspended(true);
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

	/**
	 * Mark the cached stack frame as stale so it will be refreshed on the
	 * next call to {@link #getTopStackFrame()}.
	 * <p>
	 * Unlike nulling the frame, this preserves the same object instance so
	 * Eclipse's Debug View keeps its tree selection and expansion state
	 * (e.g., the Registers viewer stays expanded).
	 */
	void invalidateFrame() {
		frameDirty = true;
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
			if (frameDirty) {
				currentFrame.update();
				frameDirty = false;
			}
			return currentFrame;
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
