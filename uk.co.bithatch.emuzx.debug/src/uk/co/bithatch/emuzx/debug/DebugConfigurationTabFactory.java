package uk.co.bithatch.emuzx.debug;

import java.util.List;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import uk.co.bithatch.emuzx.ui.IExternalEmulatorLaunchTabFactory;

public class DebugConfigurationTabFactory implements IExternalEmulatorLaunchTabFactory {

	@Override
	public void fillTabs(String mode, ILaunchConfigurationDialog dialog, List<ILaunchConfigurationTab> tabs) {
		if (ILaunchManager.DEBUG_MODE.equals(mode)) {
			tabs.add(tabs.size() - 2 >= 0 ? tabs.size() - 2 : 0, new DebugConfigurationTab());
		}
		
	}

}
