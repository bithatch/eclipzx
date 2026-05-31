package uk.co.bithatch.eclipz80.ui.debug;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class AsmToggleBreakpointsTarget implements IToggleBreakpointsTarget {

	@Override
	public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
		if (!(part instanceof ITextEditor editor))
			return;

		var file = editor.getEditorInput().getAdapter(IFile.class);
		if (file == null)
			return;

		int lineNumber = ((ITextSelection) selection).getStartLine() + 1;
		System.out.println("[AsmToggleBreakpointsTarget] toggleLineBreakpoints called, line=" + lineNumber + ", file=" + file.getName());

		var mgr = DebugPlugin.getDefault().getBreakpointManager();

		// Check if a breakpoint already exists at this line
		for (IBreakpoint bp : mgr.getBreakpoints(AsmLineBreakpoint.MODEL_ID)) {
			IResource bpResource = bp.getMarker().getResource();
			if (bpResource.equals(file) && bp instanceof ILineBreakpoint lbp && lbp.getLineNumber() == lineNumber) {
				bp.delete();
				return;
			}
		}

		// Create a new breakpoint
		IMarker marker = file.createMarker(AsmLineBreakpoint.MARKER_ID);
		marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		marker.setAttribute(IMarker.MESSAGE, "Breakpoint at line " + lineNumber);
		mgr.addBreakpoint(new AsmLineBreakpoint(marker));
	}

	@Override
	public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection) {
		return part instanceof ITextEditor editor && editor.getEditorInput().getAdapter(IFile.class) != null;
	}

	@Override
	public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
		// Not supported
	}

	@Override
	public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) {
		return false;
	}

	@Override
	public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
		// Not supported
	}

	@Override
	public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection) {
		return false;
	}
}
