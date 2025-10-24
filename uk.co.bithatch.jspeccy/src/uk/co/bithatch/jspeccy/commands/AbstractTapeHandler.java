package uk.co.bithatch.jspeccy.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.jspeccy.editor.TapeBrowser;

public abstract class AbstractTapeHandler extends AbstractHandler {

	@Override
	public final Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart part = HandlerUtil.getActiveEditor(event);
		if (part instanceof TapeBrowser ase) {
			return onHandle(event, ase);
		}
		return null;
	}

	protected abstract Object onHandle(ExecutionEvent event, TapeBrowser ase);
}
