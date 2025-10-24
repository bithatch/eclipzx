package uk.co.bithatch.zxbasic.ui.debug;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.LineBreakpoint;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

public class ToggleBreakpointHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var part = HandlerUtil.getActiveEditor(event);
		if (!(part instanceof ITextEditor editor))
			return null;

		var ruler = editor.getAdapter(IVerticalRulerInfo.class);
		if (ruler == null)
			return null;

		int lineNumber = ruler.getLineOfLastMouseButtonActivity();
		if (lineNumber < 0)
			return null;

		var file = editor.getEditorInput().getAdapter(IFile.class);
		if (file == null)
			return null;

		try {
			var mgr = DebugPlugin.getDefault().getBreakpointManager();
			var breakpoints = mgr.getBreakpoints();

			// Toggle logic: remove if exists
			for (var bp : breakpoints) {
				if (bp instanceof LineBreakpoint && file.equals(bp.getMarker().getResource())
						&& ((LineBreakpoint) bp).getLineNumber() == lineNumber + 1) {
					bp.delete();
					return null;
				}
			}

			// Add new
			var marker = file.createMarker(ZXBasicLineBreakpoint.MARKER_ID);
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber + 1);
			marker.setAttribute(IMarker.MESSAGE, "Breakpoint at line " + (lineNumber + 1));
//            marker.setAttribute(IMarker.PERSISTENT, true);

			mgr.addBreakpoint(new ZXBasicLineBreakpoint(marker));

		} catch (CoreException e) {
			throw new ExecutionException("Could not toggle breakpoint", e);
		}

		return null;
	}
}
