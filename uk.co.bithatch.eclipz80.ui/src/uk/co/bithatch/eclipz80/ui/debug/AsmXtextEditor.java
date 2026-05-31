package uk.co.bithatch.eclipz80.ui.debug;

import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.debug.ui.actions.ToggleBreakpointAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.xtext.ui.editor.XtextEditor;

/**
 * Custom Xtext editor that installs breakpoint toggle on ruler double-click.
 */
public class AsmXtextEditor extends XtextEditor {

	@Override
	protected void createActions() {
		super.createActions();

		// Replace the default ruler double-click action (bookmark) with breakpoint toggle
		IAction action = new ToggleBreakpointAction(this, null, getVerticalRuler());
		setAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK, action);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IToggleBreakpointsTarget.class) {
			return (T) new AsmToggleBreakpointsTarget();
		}
		return super.getAdapter(adapter);
	}
}
