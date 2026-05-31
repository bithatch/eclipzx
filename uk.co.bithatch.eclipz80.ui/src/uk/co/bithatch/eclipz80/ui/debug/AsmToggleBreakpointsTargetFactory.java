package uk.co.bithatch.eclipz80.ui.debug;

import java.util.Collections;
import java.util.Set;

import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.xtext.ui.editor.XtextEditor;

public class AsmToggleBreakpointsTargetFactory implements IToggleBreakpointsTargetFactory {

	private static final String ID = "uk.co.bithatch.eclipz80.ui.debug.toggleBreakpointsTargetFactory";

	private boolean isAsmEditor(IWorkbenchPart part) {
		IEditorPart editor = part.getAdapter(IEditorPart.class);
		if (editor == null && part instanceof IEditorPart ep)
			editor = ep;
		if (editor instanceof XtextEditor xtextEditor) {
			return "uk.co.bithatch.eclipz80.Asm".equals(xtextEditor.getLanguageName());
		}
		return false;
	}

	@Override
	public IToggleBreakpointsTarget createToggleTarget(String targetID) {
		return new AsmToggleBreakpointsTarget();
	}

	@Override
	public String getDefaultToggleTarget(IWorkbenchPart part, ISelection selection) {
		return isAsmEditor(part) ? ID : null;
	}

	@Override
	public String getToggleTargetDescription(String targetID) {
		return "Z80 Assembly Line Breakpoints";
	}

	@Override
	public String getToggleTargetName(String targetID) {
		return "Z80 Assembly Breakpoints";
	}

	@Override
	public Set<String> getToggleTargets(IWorkbenchPart part, ISelection selection) {
		if (isAsmEditor(part)) {
			return Set.of(ID);
		}
		return Collections.emptySet();
	}
}
