package uk.co.bithatch.emuzx.debug;

import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.PORT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrievalExtension;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import uk.co.bithatch.bitzx.ISourceAdressMap;
import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTarget;

/**
 * Debug target that connects directly to an emulator's GDB Remote Serial
 * Protocol stub (e.g., MAME's gdbstub) using {@link GdbRspClient}.
 * <p>
 * This bypasses z88dk-gdb entirely and speaks GDB RSP directly over a
 * TCP socket, providing reliable control of the emulator.
 */
public class GdbDebugTarget extends ExternalEmulatorDebugTarget implements IMemoryBlockRetrievalExtension {

	private static final ILog LOG = ILog.of(GdbDebugTarget.class);

	private final GdbRspClient rsp;
	private final GdbZ80Thread z80thread;
	private final Thread waitThread;
	private final ISourceAdressMap debugInfo;
	private volatile boolean suspended = true;
	private volatile boolean terminated = false;
	
	private final DebugTargetHelper helper;

	void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}

	/** Tracks active memory blocks so we can fire content-change events on suspend */
	private final List<Z80MemoryBlock> memoryBlocks = new ArrayList<>();

	public GdbDebugTarget(ILaunch launch, ILaunchConfiguration configuration,
			IProcess emulatorProcess, ISourceAdressMap debugInfo) throws CoreException {
		super(launch, emulatorProcess);

		/* Parse debug info from .lis / .map files */
		this.debugInfo = debugInfo;

		int port = 23946;
		try {
			port = configuration.getAttribute(PORT, 23946);
		} catch (CoreException e) {
			LOG.warn("Could not read port from configuration, using default", e);
		}

		try {
			rsp = new GdbRspClient("127.0.0.1", port);

			/* Query halt reason — target should be stopped at startup */
			var halt = rsp.queryHaltReason();
			LOG.info("Initial halt reason: " + halt);

			/* Read registers to confirm communication */
			var regs = rsp.readRegisters();
			LOG.info("Initial registers: " + regs);

		} catch (IOException e) {
			if (e instanceof ConnectException) {
				throw new UncheckedIOException(e);
			}
			throw new CoreException(Status.error("Failed to connect GDB RSP to emulator: " + e.getMessage(), e));
		}

		z80thread = new GdbZ80Thread(this, rsp, debugInfo);
		
		helper = new DebugTargetHelper(launch, this, z80thread, debugInfo) {

			@Override
			protected void onAddBreakpoint(IBreakpoint breakpoint, int address) throws IOException {
				setBreakpointWithPause(breakpoint, address);
			}

			@Override
			protected boolean onBreakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta, int address) {
				try {
					boolean wasRunning = !suspended;
					if (wasRunning) {
						rsp.interrupt();
						long deadline = System.currentTimeMillis() + 3000;
						while (!suspended && System.currentTimeMillis() < deadline) {
							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return false;
							}
						}
					}
					rsp.removeBreakpoint(address);
					LOG.info("Removed breakpoint at 0x" + Integer.toHexString(address));
					if (wasRunning && suspended) {
						z80thread.invalidateFrame();
						rsp.continueExecution();
						suspended = false;
						return true;
					}
				} catch (IOException e) {
					LOG.error("Failed to remove breakpoint at 0x" + Integer.toHexString(address), e);
				}
				return false;
			}
			
		};

		/* Background thread to wait for stop events after continue/step */
		waitThread = new Thread(this::waitForStopLoop, "gdb-rsp-wait");
		waitThread.setDaemon(true);
		waitThread.start();

		/* Auto-resume from the initial 0x0000 (ROM) halt.
		 * We set suspended=false BEFORE firing CREATE events, so Eclipse
		 * considers the target already running and doesn't attempt to fetch
		 * the 0x0000 stack frame (which would pop up "Source not found"). */
		LOG.info("Auto-resuming execution to avoid initial 0x0000 halt");
		z80thread.setStepping(false);

		try {
			rsp.continueExecution();
			suspended = false;
		} catch (IOException e) {
			LOG.error("Failed to auto-resume from initial halt", e);
		}

		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
		fireEvent(new DebugEvent(z80thread, DebugEvent.CREATE));
		fireEvent(new DebugEvent(z80thread, DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST));


		LOG.info("GDB RSP debug target created successfully"
				+ (debugInfo.hasDebugInfo() ? " with line mappings" : " (no debug info)"));
	}

	@Override
	public IThread[] getThreads() throws DebugException {
		return isTerminated() ? new IThread[0] : new IThread[] { z80thread };
	}

	@Override
	public boolean hasThreads() {
		return !isTerminated();
	}

	@Override
	public boolean canResume() {
		return suspended && !terminated;
	}

	@Override
	public boolean canSuspend() {
		return !suspended && !terminated;
	}

	@Override
	public boolean isSuspended() {
		return suspended;
	}

	@Override
	public void resume() throws DebugException {
		if (suspended && !terminated) {
			try {
				LOG.info("Resuming execution");
				z80thread.setStepping(false);
				z80thread.invalidateFrame();
				rsp.continueExecution();
				suspended = false;
				fireEvent(new DebugEvent(z80thread, DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST));
			} catch (IOException e) {
				suspended = true;
				throw new DebugException(Status.error("Failed to resume", e));
			}
		}
	}

	@Override
	public void suspend() throws DebugException {
		if (!suspended && !terminated) {
			try {
				LOG.info("Suspending execution");
				rsp.interrupt();
				/* waitThread will pick up the stop reply and set suspended=true */
			} catch (IOException e) {
				throw new DebugException(Status.error("Failed to suspend", e));
			}
		}
	}

	@Override
	public boolean canTerminate() {
		return !terminated;
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public void terminate() throws DebugException {
		if (!terminated) {
			terminated = true;
			LOG.info("Terminating GDB RSP debug target");
			try {
				rsp.kill();
			} catch (IOException e) {
				LOG.warn("Error sending kill to GDB RSP", e);
			}
			try {
				rsp.close();
			} catch (IOException e) {
				LOG.warn("Error closing GDB RSP connection", e);
			}
			helper.terminate();
			super.terminate();
		}
	}

	@Override
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		return true;
	}

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		helper.breakpointAdded(breakpoint);
	}

	/**
	 * Set a breakpoint, pausing the target first if it is currently running.
	 * GDB RSP does not allow sending commands while the target is running
	 * (the response would be consumed by the wait-for-stop thread).
	 */
	private void setBreakpointWithPause(IBreakpoint breakpoint, int address) throws IOException {
		boolean wasRunning = !suspended;
		if (wasRunning) {
			LOG.info("Target is running — interrupting to set breakpoint");
			rsp.interrupt();
			/* Wait for the stop reply to be consumed by waitForStopLoop */
			long deadline = System.currentTimeMillis() + 3000;
			while (!suspended && System.currentTimeMillis() < deadline) {
				try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
			}
			if (!suspended) {
				LOG.warn("Target did not stop in time for breakpoint set");
				return;
			}
		}
		if (rsp.setBreakpoint(address)) {
			helper.put(breakpoint, address);
		} else {
			LOG.warn("GDB stub rejected breakpoint at 0x" + Integer.toHexString(address));
		}
		if (wasRunning) {
			LOG.info("Resuming after breakpoint set");
			z80thread.invalidateFrame();
			rsp.continueExecution();
			suspended = false;
			fireEvent(new DebugEvent(z80thread, DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST));
		}
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		helper.breakpointRemoved(breakpoint, delta);
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		helper.breakpointChanged(breakpoint, delta);		
	}

	@Override
	public boolean supportsStorageRetrieval() {
		return true;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
		/* Delegate to the extended API */
		return getExtendedMemoryBlock("0x" + Long.toHexString(startAddress),
				this);
	}

	@Override
	public IMemoryBlockExtension getExtendedMemoryBlock(String expression,
			Object context) throws DebugException {
		/* Parse the expression as a hex or decimal address */
		BigInteger address;
		try {
			var expr = expression.strip();
			if (expr.startsWith("0x") || expr.startsWith("0X")) {
				address = new BigInteger(expr.substring(2), 16);
			} else if (expr.startsWith("$")) {
				address = new BigInteger(expr.substring(1), 16);
			} else {
				address = new BigInteger(expr);
			}
		} catch (NumberFormatException e) {
			throw new DebugException(Status.error(
					"Invalid memory address expression: " + expression, e));
		}
		var block = new Z80MemoryBlock(this, rsp, expression, address);
		memoryBlocks.add(block);
		return block;
	}

	/**
	 * Notify all active memory blocks that content may have changed.
	 * Called after the target suspends so hex views auto-refresh.
	 */
	private void fireMemoryBlockContentChange() {
		for (var block : memoryBlocks) {
			block.fireContentChange();
		}
	}

	private void waitForStopLoop() {
		while (!terminated) {
			/* Only enter the blocking wait when:
			 *  - we are NOT suspended (target is running after 'c')
			 *  - we are NOT stepping (stepInto() handles its own stop reply)
			 *  - RSP socket is still open */
			if (!suspended && !z80thread.isStepping() && rsp.isConnected()) {
				try {
					var stopReply = rsp.waitForStop();
					if (stopReply != null && !terminated) {
						LOG.info("Target stopped: " + stopReply);
						suspended = true;
						z80thread.invalidateFrame();
						z80thread.setStepping(false);

						/* Fire suspend events from the THREAD, not the debug target.
						 * Eclipse requires the event source to be the thread
						 * for proper stack frame selection and source highlighting. */
						int detail;
						if (stopReply.contains("T05") || stopReply.contains("T02")) {
							detail = DebugEvent.BREAKPOINT;
						} else {
							detail = DebugEvent.CLIENT_REQUEST;
						}
						fireEvent(new DebugEvent(z80thread, DebugEvent.SUSPEND, detail));

						/* Force the frame to update in-place and tell Eclipse
						 * its content changed.  This makes Eclipse re-query
						 * getLineNumber() / getName() and move the instruction
						 * pointer annotation to the new source line. */
						try {
							var topFrame = z80thread.getTopStackFrame();
							if (topFrame != null) {
								fireEvent(new DebugEvent(topFrame, DebugEvent.CHANGE, DebugEvent.CONTENT));
							}
						} catch (DebugException de) {
							LOG.warn("Could not refresh stack frame after suspend", de);
						}

						/* Notify memory renderings that content may have changed */
						fireMemoryBlockContentChange();
					}
				} catch (IOException e) {
					if (!terminated) {
						LOG.error("Error waiting for stop", e);
						terminated = true;
						fireTerminateEvent();
					}
				}
			} else {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	/**
	 * @return the debug info parser (for use by other components)
	 */
	public ISourceAdressMap getDebugInfo() {
		return debugInfo;
	}


	GdbRspClient getRspClient() {
		return rsp;
	}

}
