package uk.co.bithatch.zxbasic.ui.debug;

import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.PORT;
import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.START_SUSPENDED;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.stream.IntStream;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
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

import uk.co.bithatch.zxbasic.ui.launch.ExternalEmulatorDebugTarget;
import uk.co.bithatch.zyxy.dezog.Command.PauseNotification;
import uk.co.bithatch.zyxy.dezog.DezogClient;

public class DezogDebugTarget extends ExternalEmulatorDebugTarget {
	
	private final DezogClient dezog;
	private final IThread z80thread;

	public DezogDebugTarget(ILaunch launch, ILaunchConfiguration configuration, IProcess process) throws CoreException {
		super(launch, process);

		dezog = new DezogClient.Builder()
				.withPort(configuration.getAttribute(PORT, DezogClient.DEFAULT_PORT))
				.withStartSuspended(configuration.getAttribute(START_SUSPENDED, false))
				.onPause(pause -> {
					remotePause(pause);
				})
				.build();

		System.out.println(String.format("Connected to Debugger %s (%s) which is a %s", dezog.name(),
				String.join(".", IntStream.of(dezog.version()).mapToObj(String::valueOf).toList()),
				dezog.machineType()));
		
		z80thread = new Z80Thread(this);
		
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);

	}

	@Override
	public IThread[] getThreads() throws DebugException {
		return isTerminated() ? new IThread[0] : new IThread[] { z80thread };
	}

	@Override
	public boolean hasThreads() {
		return true;
	}

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
		return dezog.suspended();
	}

	@Override
	public void resume() throws DebugException {
		if(isSuspended()) {
			dezog.resume();
			System.out.println("UI -  Resumed");
		}
	}

	@Override
	public void suspend() throws DebugException {
		if(!isSuspended()) {
			dezog.pause();
			System.out.println("UI -  Suspended");
		}
	}

	@Override
	public void terminate() throws DebugException {
		try {
			dezog.close();
		} catch (IOException e) {
			throw new DebugException(Status.error("Failed to close debugger.", e));
		} finally {
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
		System.out.println("BRKPOINT ADDED " + breakpoint);
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		System.out.println("BRKPOINT REMOVED " + breakpoint + " : " +delta);
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		System.out.println("BRKPOINT CHANGED " + breakpoint + " : " +delta);
	}
	
	@Override
	public boolean supportsStorageRetrieval() {
		return true;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
		var data = dezog.readMemory((int)startAddress, (int)length);
		var bytes = new byte[data.remaining()];
		data.get(bytes);
		return new IMemoryBlock() {
			
			@Override
			public <T> T getAdapter(Class<T> adapter) {
				return null;
			}
			
			@Override
			public String getModelIdentifier() {
				return DezogDebugTarget.this.getModelIdentifier();
			}
			
			@Override
			public ILaunch getLaunch() {
				return DezogDebugTarget.this.getLaunch();
			}
			
			@Override
			public IDebugTarget getDebugTarget() {
				return DezogDebugTarget.this;
			}
			
			@Override
			public boolean supportsValueModification() {
				return true;
			}
			
			@Override
			public void setValue(long offset, byte[] bytes) throws DebugException {
				dezog.writeMemory((int)offset, ByteBuffer.wrap(bytes));
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
				return bytes;
			}
		};
	}

	DezogClient dezog() {
		return dezog;
	}

	private void remotePause(PauseNotification pause) {
		System.out.println("remotePause " + pause);
		switch(pause.reason()) {
		case BREAKPOINT:
			fireSuspendEvent(DebugEvent.BREAKPOINT);
			break;
		case MANUAL:
			fireSuspendEvent(DebugEvent.SUSPEND);
			break;
		case WATCHPOINT_WRITE:
		case WATCHPOINT_READ:
			fireSuspendEvent(DebugEvent.EVALUATION);
			break;
		case OTHER:
			fireChangeEvent(DebugEvent.STATE);
			break;
		case NONE:
			// TODO for stepping over, check event type
			fireSuspendEvent(DebugEvent.CLIENT_REQUEST);
			break;
		default:
			System.out.println("Unhandled pause reason " + pause.reason());
			break;
		}
	}
}
