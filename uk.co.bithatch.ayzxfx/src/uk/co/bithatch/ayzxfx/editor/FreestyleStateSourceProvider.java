package uk.co.bithatch.ayzxfx.editor;

import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.services.ISourceProviderService;

public class FreestyleStateSourceProvider extends AbstractSourceProvider {

	public static final String FREESTYLE_VAR = "uk.co.bithatch.ayzxfx.editor.freestyleActive";

	private boolean freestyleActive = false;

	@Override
	public void dispose() {
	}

	@Override
	public Map<?, ?> getCurrentState() {
		return Map.of(FREESTYLE_VAR, freestyleActive);
	}

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] { FREESTYLE_VAR };
	}

	public void setFreestyleActive(boolean active) {
		this.freestyleActive = active;
		fireSourceChanged(ISources.ACTIVE_WORKBENCH_WINDOW, FREESTYLE_VAR, active);

		var commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
		commandService.refreshElements(AutoAddHandler.ID, null);

	}

	public boolean isFreestyleActive() {
		return freestyleActive;
	}

	
	public static boolean isFreestyleModeEnabled() {
		/* TODO this is still a bit crap. Needs to be per-editor, and change states
		 * when active editor changes.
		 */
		var sourceProviderService = PlatformUI.getWorkbench().getService(ISourceProviderService.class);
		var provider = (FreestyleStateSourceProvider) sourceProviderService
				.getSourceProvider(FreestyleStateSourceProvider.FREESTYLE_VAR);
		return provider.isFreestyleActive();
	}
}
