package uk.co.bithatch.zxbasic.ui.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class InterpreterLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        setTabs(new ILaunchConfigurationTab[] { new InterpreterLaunchConfigurationTab() ,new CommonTab()});
    }

}