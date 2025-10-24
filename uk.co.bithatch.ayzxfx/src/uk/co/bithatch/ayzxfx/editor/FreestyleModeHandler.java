package uk.co.bithatch.ayzxfx.editor;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.services.ISourceProviderService;

public class FreestyleModeHandler extends AbstractHandler implements IElementUpdater {

	private boolean enabled = false;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		enabled = !enabled;

		var part = HandlerUtil.getActiveEditor(event);
		if (part instanceof AFXEditor ase) {
			ase.setFreestyleMode(enabled);
			var sourceProviderService = PlatformUI.getWorkbench().getService(ISourceProviderService.class);
			var provider = (FreestyleStateSourceProvider) sourceProviderService
					.getSourceProvider(FreestyleStateSourceProvider.FREESTYLE_VAR);
			provider.setFreestyleActive(enabled);
		}

		return null;
	}

	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
		element.setChecked(enabled);
	}
}
