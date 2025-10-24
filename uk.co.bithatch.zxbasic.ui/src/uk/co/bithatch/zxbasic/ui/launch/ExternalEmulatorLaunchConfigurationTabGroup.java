package uk.co.bithatch.zxbasic.ui.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import uk.co.bithatch.emuzx.ui.ExternalEmulatorConfigurationTab;
import uk.co.bithatch.emuzx.ui.ExternalEmulatorConfigurationTab.LaunchConfigurationContext;

public class ExternalEmulatorLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

	public final static String GROUP_ID = "uk.co.bithatch.zxbasic.ui.launch.externalEmulatorLaunch";
	
    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    	var launchTab = new ExternalEmulatorLaunchConfigurationTab();
    	var preparationTab = new DiskImagePreparationTab();
    	var emulatorTab = new ExternalEmulatorConfigurationTab(mode, new LaunchConfigurationContext() {
			
			@Override
			public IPath resolveProjectPath() {
				return launchTab.resolveProjectPath();
			}
			
			@Override
			public IFile resolveProgram() {
				return launchTab.resolveProgram();
			}
			
			@Override
			public void initializeFrom(ILaunchConfigurationWorkingCopy cfg) {
				preparationTab.initializeFrom(cfg);				
			}
		}); 
    	
    	// TODO ew. fix it
    	launchTab.setEmulatorTab(emulatorTab);
    	preparationTab.setLaunchTab(launchTab);
    	
    	if(mode.equals(ILaunchManager.DEBUG_MODE)) {
    		
    		
	        setTabs(new ILaunchConfigurationTab[] { 
	        		launchTab, 
	        		preparationTab,
	        		emulatorTab,
	        		new DebugConfigurationTab(),
	        		new EnvironmentTab(),
	        		new CommonTab()});
    	}
    	else {
	        setTabs(new ILaunchConfigurationTab[] { 
	        		launchTab, 
	        		preparationTab,
	        		emulatorTab,
	        		new EnvironmentTab(),
	        		new CommonTab()});
    	}
    }

}