package uk.co.bithatch.emuzx.debug;

import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.PORT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTarget;

/**
 * Debug target that connects directly to an emulator's GDB Remote Serial
 * Protocol stub (e.g., MAME's gdbstub) using {@link GdbRspClient}.
 * <p>
 * This bypasses z88dk-gdb entirely and speaks GDB RSP directly over a
 * TCP socket, providing reliable control of the emulator.
 */
public class GdbDebugTarget extends ExternalEmulatorDebugTarget {

	private static final ILog LOG = ILog.of(GdbDebugTarget.class);

	private final GdbRspClient rsp;
	private final GdbZ80Thread z80thread;
	private final Thread waitThread;
	private final Z88dkDebugInfoParser debugInfo;
	private volatile boolean suspended = true;
	private volatile boolean terminated = false;

	void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}

	/** Tracks breakpoints by address so we can remove them */
	private final Map<IBreakpoint, Integer> breakpointAddresses = new HashMap<>();

	public GdbDebugTarget(ILaunch launch, ILaunchConfiguration configuration,
			IProcess emulatorProcess, Path binaryPath) throws CoreException {
		super(launch, emulatorProcess);

		/* Parse debug info from .lis / .map files */
		debugInfo = new Z88dkDebugInfoParser();
		if (binaryPath != null) {
			debugInfo.parse(binaryPath);
		}

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

		/* Set up source locator so Eclipse can find and highlight source files */
		if (launch.getSourceLocator() == null) {
			launch.setSourceLocator(new GdbSourceLocator());
		}

		/* Install any existing breakpoints from the workspace */
		var bpManager = DebugPlugin.getDefault().getBreakpointManager();
		bpManager.addBreakpointListener(this);
		for (var bp : bpManager.getBreakpoints()) {
			if (supportsBreakpoint(bp)) {
				breakpointAdded(bp);
			}
		}

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
				+ (debugInfo.hasDebugInfo() ? " with " + debugInfo.getLineToAddressMap().size() + " line mappings" : " (no debug info)"));
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
			DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
			super.terminate();
		}
	}

	@Override
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		return true;
	}

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		try {
			var marker = breakpoint.getMarker();
			LOG.info("Breakpoint added: " + breakpoint + ", marker attrs: " + marker.getAttributes());
			
			/* Check for address-based breakpoint (e.g. from memory view) */
			var addrStr = marker.getAttribute("org.eclipse.cdt.debug.core.address", (String) null);
			if (addrStr != null) {
				int address = Integer.decode(addrStr);
				LOG.info("Setting address breakpoint at 0x" + Integer.toHexString(address));
				setBreakpointWithPause(breakpoint, address);
				return;
			}

			/* Source line breakpoint — resolve via debug info parser. */
			int lineNumber = marker.getAttribute("lineNumber", -1);
			var resource = marker.getResource();
			if (resource != null && lineNumber > 0 && debugInfo.hasDebugInfo()) {
				var fileName = resource.getName();
				int address = debugInfo.getAddress(fileName, lineNumber);
				if (address >= 0) {
					LOG.info("Setting source breakpoint at " + fileName + ":" + lineNumber
							+ " → 0x" + Integer.toHexString(address));
					setBreakpointWithPause(breakpoint, address);
				} else {
					LOG.warn("No address mapping found for " + fileName + ":" + lineNumber
							+ " — do you have --c-code-in-asm in compiler flags and --list in linker flags?");
				}
			} else {
				LOG.info("Source breakpoint at " + (resource != null ? resource.getName() : "?") + ":" + lineNumber 
						+ " — " + (debugInfo.hasDebugInfo() ? "no mapping found" : "no debug info available"));
			}
		} catch (Exception e) {
			LOG.error("Failed to add breakpoint", e);
		}
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
			breakpointAddresses.put(breakpoint, address);
		} else {
			LOG.warn("GDB stub rejected breakpoint at 0x" + Integer.toHexString(address));
		}
		if (wasRunning) {
			LOG.info("Resuming after breakpoint set");
			rsp.continueExecution();
			suspended = false;
			fireEvent(new DebugEvent(z80thread, DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST));
		}
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		LOG.info("Breakpoint removed: " + breakpoint);
		var address = breakpointAddresses.remove(breakpoint);
		if (address != null) {
			try {
				boolean wasRunning = !suspended;
				if (wasRunning) {
					rsp.interrupt();
					long deadline = System.currentTimeMillis() + 3000;
					while (!suspended && System.currentTimeMillis() < deadline) {
						try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
					}
				}
				rsp.removeBreakpoint(address);
				LOG.info("Removed breakpoint at 0x" + Integer.toHexString(address));
				if (wasRunning && suspended) {
					rsp.continueExecution();
					suspended = false;
					fireEvent(new DebugEvent(z80thread, DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST));
				}
			} catch (IOException e) {
				LOG.error("Failed to remove breakpoint at 0x" + Integer.toHexString(address), e);
			}
		}
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		LOG.info("Breakpoint changed: " + breakpoint);
		/* Re-add if enabled, remove if disabled */
		try {
			if (breakpoint.isEnabled()) {
				if (!breakpointAddresses.containsKey(breakpoint)) {
					breakpointAdded(breakpoint);
				}
			} else {
				breakpointRemoved(breakpoint, delta);
			}
		} catch (CoreException e) {
			LOG.error("Failed to handle breakpoint change", e);
		}
	}

	@Override
	public boolean supportsStorageRetrieval() {
		return true;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
		try {
			var hexData = rsp.readMemory((int) startAddress, (int) length);
			var bytes = hexToBytes(hexData);
			return new IMemoryBlock() {
				@Override public <T> T getAdapter(Class<T> adapter) { return null; }
				@Override public String getModelIdentifier() { return GdbDebugTarget.this.getModelIdentifier(); }
				@Override public ILaunch getLaunch() { return GdbDebugTarget.this.getLaunch(); }
				@Override public IDebugTarget getDebugTarget() { return GdbDebugTarget.this; }
				@Override public boolean supportsValueModification() { return false; }
				@Override public void setValue(long offset, byte[] bytes) throws DebugException { }
				@Override public long getStartAddress() { return startAddress; }
				@Override public long getLength() { return length; }
				@Override public byte[] getBytes() throws DebugException { return bytes; }
			};
		} catch (IOException e) {
			throw new DebugException(Status.error("Failed to read memory", e));
		}
	}

	private void waitForStopLoop() {
		while (!terminated) {
			if (!suspended && rsp.isConnected()) {
				try {
					var stopReply = rsp.waitForStop();
					if (stopReply != null && !terminated) {
						LOG.info("Target stopped: " + stopReply);
						suspended = true;
						boolean wasStepping = z80thread.isStepping();
						z80thread.setStepping(false);
						/* Fire suspend events from the THREAD, not the debug target.
						 * Eclipse requires the event source to be the thread
						 * for proper stack frame selection and source highlighting. */
						int detail;
						if (wasStepping) {
							detail = DebugEvent.STEP_END;
						} else if (stopReply.contains("T05") || stopReply.contains("T02")) {
							detail = DebugEvent.BREAKPOINT;
						} else {
							detail = DebugEvent.CLIENT_REQUEST;
						}
						fireEvent(new DebugEvent(z80thread, DebugEvent.SUSPEND, detail));
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
	public Z88dkDebugInfoParser getDebugInfo() {
		return debugInfo;
	}


	GdbRspClient getRspClient() {
		return rsp;
	}

	private static byte[] hexToBytes(String hex) {
		if (hex == null) return new byte[0];
		int len = hex.length() / 2;
		var bytes = new byte[len];
		for (int i = 0; i < len; i++) {
			bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
		}
		return bytes;
	}

}
