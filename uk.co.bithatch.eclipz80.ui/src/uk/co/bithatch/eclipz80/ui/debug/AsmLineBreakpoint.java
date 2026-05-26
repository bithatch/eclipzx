package uk.co.bithatch.eclipz80.ui.debug;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.LineBreakpoint;

import uk.co.bithatch.eclipz80.ui.AsmUiActivator;

public class AsmLineBreakpoint extends LineBreakpoint {

    static final String MARKER_ID = "uk.co.bithatch.eclipz80.ui.debug.lineBreakpointMarker";

    public AsmLineBreakpoint(IMarker marker) throws CoreException {
        marker.setAttribute(IBreakpoint.ENABLED, true);
        marker.setAttribute(IBreakpoint.ID, DebugPlugin.getDefault().getBundle().getSymbolicName());
        setMarker(marker);
    }

	@Override
	public String getModelIdentifier() {
		return AsmUiActivator.PLUGIN_ID;
	}
}
