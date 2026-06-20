package uk.co.bithatch.emuzx.ui;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class ExternalEmulatorLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    	var preparationTab = new DiskImagePreparationTab();
    	var launchTab = new ExternalEmulatorLaunchConfigurationTab();
    	var configFileTab = new EmulatorConfigurationFileTab(); 
    	var lcContext = new LaunchConfigurationContext() {
			
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
				configFileTab.initializeFrom(cfg);
				launchTab.reselectOutputFormat(cfg);
			}

			@Override
			public boolean isValid() {
				return launchTab.resolveProject() != null && launchTab.resolveProgram() != null;
			}
		};
		configFileTab.setLaunchContext(lcContext);
		
		var emulatorTab = new ExternalEmulatorConfigurationTab(mode, lcContext);

    	// TODO ew. fix it
    	launchTab.setEmulatorTab(emulatorTab);
    	preparationTab.setLaunchTab(launchTab::resolveProject);
    	
    	var tabs = new ArrayList<ILaunchConfigurationTab>(Arrays.asList( 
    		launchTab, 
    		preparationTab,
    		emulatorTab,
    		configFileTab,
    		new EnvironmentTab(),
    		new CommonTab()
    	));
    	
    	ExternalEmulatorLaumchTabFactoryRegistry.descriptors().forEach(desc -> {
    		try {
				desc.createTabFactory().fillTabs(mode, dialog, tabs);
			} catch (CoreException e) {
				e.printStackTrace();
			}
    	});
    	
    	setTabs(tabs.toArray(new ILaunchConfigurationTab[0]));
    }

}