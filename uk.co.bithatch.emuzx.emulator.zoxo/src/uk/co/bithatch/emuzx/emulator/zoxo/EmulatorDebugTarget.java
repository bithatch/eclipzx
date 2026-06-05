package uk.co.bithatch.emuzx.emulator.zoxo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.ILog;
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

import uk.co.bithatch.bitzx.ISourceAdressMap;
import uk.co.bithatch.eclipzoxo.views.EmulatorInstance;
import uk.co.bithatch.emuzx.debug.DebugTargetHelper;
import uk.co.bithatch.zoxo.system.CPUListener;


public class EmulatorDebugTarget extends DebugElement implements IDebugTarget {
	private static final ILog LOG = ILog.of(EmulatorDebugTarget.class);

	private final EmulatorInstance emulator;
	private final ILaunch launch;
	private boolean terminated;
	private final IProcess emprocess;
	private final boolean debugMode;
	private final EmulatorZ80Thread z80Thread;
	private final DebugTargetHelper helper;

	public EmulatorDebugTarget(ILaunch launch, EmulatorInstance emulator) {
		this(launch, emulator, null);
	}

	public EmulatorDebugTarget(ILaunch launch, EmulatorInstance emulator, ISourceAdressMap debugInfo) {
		super(null);
		this.emulator = emulator;
		this.launch = launch;
		
		debugMode = debugInfo != null;

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
				return "Zoxo";
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
		

		if(debugMode) {

			z80Thread = new EmulatorZ80Thread(this, emulator, debugInfo);
			emulator.machine().addCPUListener(new CPUListener() {

				@Override
				public Optional<Integer> onBreakpoint(int address, int opcode) {
					
					if(helper.hasBreakpoint(address)) {
						/* Stop PC from advancing */
						emulator.machine().cpu().setHalted(true);
						
						hitBreakpoint(address, opcode, helper.get(address));
						return Optional.of(0);
					}
					return Optional.empty();
				}
				
			});
			helper = new DebugTargetHelper(launch, this, z80Thread, debugInfo) {

				@Override
				protected void onAddBreakpoint(IBreakpoint breakpoint, int address) throws IOException {
					emulator.machine().cpu().setBreakpoint(address, true);
					super.onAddBreakpoint(breakpoint, address);					
					
				}

				@Override
				protected boolean onBreakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta, int address) {
					emulator.machine().cpu().setBreakpoint(address, false);
//					try {
//						boolean wasRunning = !suspended;
//						if (wasRunning) {
//							rsp.interrupt();
//							long deadline = System.currentTimeMillis() + 3000;
//							while (!suspended && System.currentTimeMillis() < deadline) {
//								try {
//									Thread.sleep(50);
//								} catch (InterruptedException e) {
//									Thread.currentThread().interrupt();
//									return false;
//								}
//							}
//						}
//						rsp.removeBreakpoint(address);
//						LOG.info("Removed breakpoint at 0x" + Integer.toHexString(address));
//						if (wasRunning && suspended) {
//							z80thread.invalidateFrame();
//							rsp.continueExecution();
//							suspended = false;
//							return true;
//						}
//					} catch (IOException e) {
//						LOG.error("Failed to remove breakpoint at 0x" + Integer.toHexString(address), e);
//					}
//					return false;
					return true;
				}
				
			};
		}
		else {
			z80Thread = null;
			helper = null;
		}
		
	}

	public EmulatorInstance getEmulator() {
		return emulator;
	}

	@Override
	public String getName() {
		return "Zoxo Run Target";
	}

	@Override
	public final IProcess getProcess() {
		return emprocess;
	}

	@Override
	public final ILaunch getLaunch() {
		return launch;
	}

	@Override
	public final boolean canTerminate() {
		return true;
	}

	@Override
	public final boolean isTerminated() {
		return terminated;
	}

	@Override
	public final void terminate() throws DebugException {
		if (terminated)
			return;
		else {
//			emulator.stopEmulation();
			try {
				emulator.close();
			}
			finally {
				if(helper != null) {
					helper.terminate();
				}
			}
		}
	}

	@Override
	public final boolean canResume() {
		return emulator.isPaused();
	}

	@Override
	public final boolean canSuspend() {
		return !emulator.isPaused();
	}

	@Override
	public final boolean isSuspended() {
		return emulator.isPaused();
	}

	@Override
	public final void resume() throws DebugException {
		emulator.unpause();
		fireResumeEvent(DebugEvent.UNSPECIFIED);
	}

	@Override
	public final void suspend() throws DebugException {
		emulator.pause();
		fireSuspendEvent(DebugEvent.UNSPECIFIED);
	}

	@Override
	public final IDebugTarget getDebugTarget() {
		return this;
	}

	@Override
	public boolean hasThreads() {
		return debugMode;
	}

	@Override
	public IThread[] getThreads() throws DebugException {
		return debugMode ? new IThread[] { z80Thread}  :  new IThread[0];
	}

	@Override
	public final boolean supportsBreakpoint(IBreakpoint breakpoint) {
		return debugMode;
	}

	@Override
	public final String getModelIdentifier() {
		return Activator.PLUGIN_ID;
	}

	@Override
	public final void breakpointAdded(IBreakpoint breakpoint) {
		helper.breakpointAdded(breakpoint);;
	}

	@Override
	public final void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		helper.breakpointRemoved(breakpoint, delta);
	}

	@Override
	public final void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		helper.breakpointChanged(breakpoint, delta);
	}

	@Override
	public final boolean canDisconnect() {
		return false;
	}

	@Override
	public final void disconnect() throws DebugException {
	}

	@Override
	public final boolean isDisconnected() {
		return false;
	}

	@Override
	public final boolean supportsStorageRetrieval() {
		return true;
	}

	@Override
	public final IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
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

	private void hitBreakpoint(int address, int opcode, IBreakpoint iBreakpoint) {
		LOG.info("Hit breakpoint " + String.format("0x%04x", address));
		
		/* Pause emulation first so isSuspended() returns true */
		emulator.pause();
		
		/* Mark the frame as dirty so it re-reads registers/source on next access,
		 * then force an update so Eclipse sees the correct PC and source location
		 * when it processes the suspend event. */
		z80Thread.invalidateFrame();
		
		fireEvent(new DebugEvent(z80Thread, DebugEvent.SUSPEND, DebugEvent.BREAKPOINT));
	}
}
