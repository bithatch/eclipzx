package uk.co.bithatch.zxbasic.ui.api;

import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Composite;

public interface IPreparationUI {
	
	void init(ILaunchPreparationUI prepUi);
	
	void setAvailable(boolean available);
	
	void createControls(Composite parent, Runnable onUpdate);

	void initializeFrom(ILaunchConfiguration config) throws CoreException;

	void performApply(ILaunchConfigurationWorkingCopy config);

	boolean isValid(ILaunchConfiguration config, Consumer<String> messageAcceptor);

	void setDefaults(ILaunchConfigurationWorkingCopy configuration);
}
