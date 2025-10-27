package uk.co.bithatch.emuzx.emulator.jspeccy;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.IThread;

import uk.co.bithatch.jspeccy.views.EmulatorInstance;

public class EmulatorDebugTarget extends DebugElement implements IDebugTarget {

	private final EmulatorInstance emulator;
	private final ILaunch launch;
	private boolean terminated;
	private final IProcess emprocess;

	public EmulatorDebugTarget(ILaunch launch, EmulatorInstance emulator) {
		super(null);
		this.emulator = emulator;
		this.launch = launch;

		emulator.addCloseListener(() -> {
			terminated = true;
			fireTerminateEvent();
		});
		
		emprocess = new IProcess() {

			private Map<String, String> attrs = new HashMap<>();

			@Override
			public void terminate() throws DebugException {
				EmulatorDebugTarget.this.terminate();
			}

			@Override
			public boolean isTerminated() {
				return EmulatorDebugTarget.this.terminated;
			}

			@Override
			public boolean canTerminate() {
				return EmulatorDebugTarget.this.canTerminate();
			}

			@Override
			public <T> T getAdapter(Class<T> adapter) {
				return null;
			}

			@Override
			public void setAttribute(String key, String value) {
				attrs.put(key, value);
			}

			@Override
			public IStreamsProxy getStreamsProxy() {
				return null;
			}

			@Override
			public ILaunch getLaunch() {
				return launch;
			}

			@Override
			public String getLabel() {
				return "JSpeccy";
			}

			@Override
			public int getExitValue() throws DebugException {
				return 0;
			}

			@Override
			public String getAttribute(String key) {
				return attrs.get(key);
			}
		};
		
	}

	@Override
	public String getName() {
		return "Internal Emulator Debug Target";
	}

	@Override
	public IProcess getProcess() {
		return emprocess;
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
		if (terminated)
			return;
		else {
//			emulator.stopEmulation();
			emulator.close();
		}
	}

	// --- Unused for now ---
	@Override
	public boolean canResume() {
		return true;
	}

	@Override
	public boolean canSuspend() {
		return true;
	}

	@Override
	public boolean isSuspended() {
		return emulator.isPaused();
	}

	@Override
	public void resume() throws DebugException {
		emulator.unpause();
		fireResumeEvent(DebugEvent.UNSPECIFIED);
	}

	@Override
	public void suspend() throws DebugException {
		emulator.pause();
		fireSuspendEvent(DebugEvent.UNSPECIFIED);
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return this;
	}

	@Override
	public boolean hasThreads() {
		return false;
	}

	@Override
	public IThread[] getThreads() throws DebugException {
		return new IThread[0];
	}

	@Override
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		return false;
	}

	@Override
	public String getModelIdentifier() {
		return Activator.PLUGIN_ID;
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
		return true;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
		return new IMemoryBlock() {

			@Override
			public <T> T getAdapter(Class<T> adapter) {
				return null;
			}

			@Override
			public String getModelIdentifier() {
				return EmulatorDebugTarget.this.getModelIdentifier();
			}

			@Override
			public ILaunch getLaunch() {
				return launch;
			}

			@Override
			public IDebugTarget getDebugTarget() {
				return EmulatorDebugTarget.this;
			}

			@Override
			public boolean supportsValueModification() {
				return true;
			}

			@Override
			public void setValue(long offset, byte[] bytes) throws DebugException {
				emulator.setMemoryBlock(offset, bytes);
			}

			@Override
			public long getStartAddress() {
				return startAddress;
			}

			@Override
			public long getLength() {
				return length;
			}

			@Override
			public byte[] getBytes() throws DebugException {
				return emulator.getMemoryBlock(startAddress, length);
			}
		};
	}
}
