package uk.co.bithatch.drawzx.screens;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.drawzx.editor.AbstractScreenEditor;

public abstract class AbstractScreenHandler extends AbstractHandler {
	
	protected static final String PARAMETER = "uk.co.bithatch.drawzx.screens.mode";
	
    @Override
    public final Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart part = HandlerUtil.getActiveEditor(event);
        if (part instanceof AbstractScreenEditor ase) {
        	return onHandle(event, ase);
        }
        return null;
    }

	protected abstract Object onHandle(ExecutionEvent event, AbstractScreenEditor ase);
}
