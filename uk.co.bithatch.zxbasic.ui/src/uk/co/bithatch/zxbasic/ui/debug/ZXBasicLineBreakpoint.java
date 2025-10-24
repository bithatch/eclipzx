package uk.co.bithatch.zxbasic.ui.debug;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.LineBreakpoint;

import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;

public class ZXBasicLineBreakpoint extends LineBreakpoint {

    static final String MARKER_ID = "uk.co.bithatch.zxbasic.ui.debug.lineBreakpointMarker";

    public ZXBasicLineBreakpoint(IMarker marker) throws CoreException {
        marker.setAttribute(IBreakpoint.ENABLED, true);
        marker.setAttribute(IBreakpoint.ID, DebugPlugin.getDefault().getBundle().getSymbolicName());
        setMarker(marker);
    }

	@Override
	public String getModelIdentifier() {
		return ZXBasicUiActivator.PLUGIN_ID;
	}
}
