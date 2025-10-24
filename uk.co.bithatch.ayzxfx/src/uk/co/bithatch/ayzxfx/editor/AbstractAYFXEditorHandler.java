package uk.co.bithatch.ayzxfx.editor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public abstract class AbstractAYFXEditorHandler extends AbstractHandler {
    @Override
    public final Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart part = HandlerUtil.getActiveEditor(event);
        if (part instanceof AFXEditor ase) {
        	onHandle(ase);
        }
        return null;
    }

	protected abstract void onHandle(AFXEditor ase);
}
