package uk.co.bithatch.eclipzx.ui.intro;

import java.util.Properties;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import uk.co.bithatch.bitzx.ZXPerspectives;
import uk.co.bithatch.widgetzx.ZXPerspectivesUI;

public class OpenDebugPerspectiveAction implements IIntroAction {
    @Override
    public final void run(IIntroSite site, Properties params) {
    	ZXPerspectivesUI.switchToPerspective(ZXPerspectives.ZX_DEBUG_ID, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
    }

	
}
