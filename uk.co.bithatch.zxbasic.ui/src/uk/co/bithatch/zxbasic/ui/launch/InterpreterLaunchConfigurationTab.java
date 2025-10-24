package uk.co.bithatch.zxbasic.ui.launch;

import org.eclipse.swt.widgets.Composite;

public class InterpreterLaunchConfigurationTab extends AbstractZXBasicLaunchConfigurationTab {
    public InterpreterLaunchConfigurationTab() {
		super(InterpreterLaunchConfigurationAttributes.PROJECT, InterpreterLaunchConfigurationAttributes.PROGRAM);
	}

	@Override
    protected void createAdditionalControls(Composite parent) {
        // No additional controls for Interpreter
    }
}