package uk.co.bithatch.eclipzx.ui.intro;

import java.util.Properties;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

public class AbstractSetupAction implements IIntroAction {
	
	private final String prefId;
	
	protected AbstractSetupAction(String prefId) {
		this.prefId = prefId;
	}
	
	@Override
	public final void run(IIntroSite site, Properties params) {
		var workbench = PlatformUI.getWorkbench();
		var ww = workbench.getActiveWorkbenchWindow();

		var dialog = PreferencesUtil.createPreferenceDialogOn(ww.getShell(),
				prefId, null, null);
		
		if (dialog != null) {
			dialog.open();
			IIntroManager introManager = workbench.getIntroManager();
			IIntroPart introPart = introManager.getIntro();
			if (introPart != null) {
				introManager.closeIntro(introPart);
				introManager.showIntro(ww, false);
			}
		}

	}

}
