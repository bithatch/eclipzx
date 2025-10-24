package uk.co.bithatch.ayzxfx.editor;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.services.ISourceProviderService;

public class AutoAddHandler extends AbstractHandler implements IElementUpdater {

	public static final String ID = "uk.co.bithatch.ayzxfx.editor.commands.autoAdd";
	private boolean enabled = false;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		enabled = !enabled;

		IEditorPart part = HandlerUtil.getActiveEditor(event);
		if (part instanceof AFXEditor ase) {
			ase.setAutoAdd(enabled);
		}

		return null;
	}

	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
		element.setChecked(enabled);
	}

	@Override
	public boolean isEnabled() {
		var sourceProviderService = PlatformUI.getWorkbench().getService(ISourceProviderService.class);
		var provider = (FreestyleStateSourceProvider) sourceProviderService
				.getSourceProvider(FreestyleStateSourceProvider.FREESTYLE_VAR);
		return provider.isFreestyleActive();
	}
}
