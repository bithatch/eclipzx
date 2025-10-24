package uk.co.bithatch.jspeccy.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.jspeccy.views.EmulatorView;

public abstract class AbstractEmulatorHandler extends AbstractHandler {

	@Override
	public final Object execute(ExecutionEvent event) throws ExecutionException {

		var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			var view = (EmulatorView) page.showView(EmulatorView.ID);
			if (view != null) {
				onHandle(event, view);
			}
		} catch (PartInitException e) {
			throw new ExecutionException("Failed to open view", e);
		}
		return null;
	}

	protected abstract Object onHandle(ExecutionEvent event, EmulatorView emulator);
}
