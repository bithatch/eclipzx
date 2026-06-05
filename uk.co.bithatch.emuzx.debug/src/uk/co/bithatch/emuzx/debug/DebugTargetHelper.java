package uk.co.bithatch.emuzx.debug;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;

import uk.co.bithatch.bitzx.ISourceAdressMap;

public class DebugTargetHelper implements IBreakpointListener {
	private static final ILog LOG = ILog.of(DebugTargetHelper.class);

	/** Tracks breakpoints by address so we can remove them */
	private final Map<IBreakpoint, Integer> breakpointAddresses = new HashMap<>();
	private final Map<Integer, IBreakpoint> addressBreakpoints = new HashMap<>();

	private final IThread thread;
	private final IDebugTarget target;
	private final ISourceAdressMap debugInfo;

	public DebugTargetHelper(ILaunch launch, IDebugTarget target, IThread thread, ISourceAdressMap debugInfo) {
		this.thread = thread;
		this.target = target;
		this.debugInfo = debugInfo;

		/* Set up source locator so Eclipse can find and highlight source files */
		if (launch.getSourceLocator() == null) {
			launch.setSourceLocator(new DebugTargetSourceLocator());
		}

		/* Install any existing breakpoints from the workspace */
		var bpManager = DebugPlugin.getDefault().getBreakpointManager();
		bpManager.addBreakpointListener(this);
		for (var bp : bpManager.getBreakpoints()) {
			if (target.supportsBreakpoint(bp)) {
				breakpointAdded(bp);
			}
		}
	}

	@Override
	public final void breakpointAdded(IBreakpoint breakpoint) {
		try {
			var marker = breakpoint.getMarker();
			LOG.info("Potentially adding breakpoint: " + breakpoint + ", marker attrs: " + marker.getAttributes());
			
			/* Check for address-based breakpoint (e.g. from memory view) */
			var addrStr = marker.getAttribute("org.eclipse.cdt.debug.core.address", (String) null);
			if (addrStr != null) {
				int address = Integer.decode(addrStr);
				LOG.info("Setting address breakpoint at 0x" + Integer.toHexString(address));
				onAddBreakpoint(breakpoint, address);
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
					onAddBreakpoint(breakpoint, address);
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
	
	protected void onAddBreakpoint(IBreakpoint breakpoint, int address) throws IOException {
		put(breakpoint, address);
	}

	@Override
	public final void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		LOG.info("Breakpoint removed: " + breakpoint);
		var address = breakpointAddresses.remove(breakpoint);
		if (address != null) {
			addressBreakpoints.remove(address);
			if(onBreakpointRemoved(breakpoint, delta, address)) {
				((DebugElement)target).fireEvent(new DebugEvent(thread, DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST));	
			}
		}
		
	}
	

	protected boolean onBreakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta, int address) {
		return true;
	}



	@Override
	public final void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
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

	public final void terminate() {
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
	}

	public final void put(IBreakpoint breakpoint, int address) {

		LOG.info("Adding breakpoint: " + breakpoint + ", add: " + String.format("0x%04x", address));
		
		breakpointAddresses.put(breakpoint, address);
		addressBreakpoints.put(address, breakpoint);
	}

	public boolean hasBreakpoint(int address) {
		return addressBreakpoints.containsKey(address);
	}

	public IBreakpoint get(int address) {
		return addressBreakpoints.get(address);
	}
}
