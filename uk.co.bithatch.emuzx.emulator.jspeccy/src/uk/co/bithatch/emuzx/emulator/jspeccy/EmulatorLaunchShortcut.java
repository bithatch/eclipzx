package uk.co.bithatch.emuzx.emulator.jspeccy;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;

import uk.co.bithatch.emuzx.ui.AbstractEmulatorLaunchShortcut;

public class EmulatorLaunchShortcut extends AbstractEmulatorLaunchShortcut {

	@Override
	protected void doLaunch(IFile programFile, String mode) throws CoreException {

		var project = programFile.getProject(); // extract from selection

	        ILaunchConfiguration config = findExistingConfiguration(project, programFile);
	        if (config == null) {
	            config = createConfiguration(project, programFile);
	        }
	        DebugUITools.launch(config, mode);


	}
	
//	protected void doLaunch(IFile file, String mode) throws CoreException {
//
//		var manager = DebugPlugin.getDefault().getLaunchManager();
//		var type = manager.getLaunchConfigurationType(EmulatorLaunchConfigurationAttributes.ID);
//
//		var name = file.getFullPath().toString();
//		var cfg = manager.generateLaunchConfigurationName(name);
//		
//		ILaunchConfiguration lcfg = manager.getLaunchConfiguration(cfg);
//		if(lcfg == null) {
//			var workingCopy = type.newInstance(PreferencesAccess.getOutputFolder(file.getProject()), cfg);
//			workingCopy.setAttribute(EmulatorLaunchConfigurationAttributes.PROGRAM, file.getName());
//			workingCopy.setAttribute(EmulatorLaunchConfigurationAttributes.PROJECT, file.getProject().getName());
////			workingCopy.setAttribute(EmulatorLaunchConfigurationAttributes.PROJECT, file.getProject().getName());
//			lcfg = workingCopy.doSave();
//		}
//		DebugUITools.launch(lcfg, mode);
//
//
//	}

	@Override
	protected String[] getSupportedExtensions() {
		return new String[] { "bas", "sna", "z80", "szx", "sp", "tap", "tzx", "csw", "rom" };
	}
	
	private ILaunchConfiguration findExistingConfiguration(IProject project, IFile programFile) throws CoreException {
	    var manager = DebugPlugin.getDefault().getLaunchManager();
	    var type = manager.getLaunchConfigurationType(EmulatorLaunchConfigurationAttributes.ID);
	    for (var config : manager.getLaunchConfigurations(type)) {
	        var projectName = config.getAttribute(EmulatorLaunchConfigurationAttributes.PROJECT, "");
	        var programPath = config.getAttribute(EmulatorLaunchConfigurationAttributes.PROGRAM, "");
	        if (projectName.equals(project.getName()) && programPath.equals(programFile.getProjectRelativePath().toString())) {
	            return config;
	        }
	    }
	    return null;
	}
	
	private ILaunchConfiguration createConfiguration(IProject project, IFile programFile) throws CoreException {
	    var manager = DebugPlugin.getDefault().getLaunchManager();
	    var type = manager.getLaunchConfigurationType(EmulatorLaunchConfigurationAttributes.ID);

	    var name = manager.generateLaunchConfigurationName(project.getName() + " - " + programFile.getName());
	    var wc = type.newInstance(null, name);

	    wc.setAttribute(EmulatorLaunchConfigurationAttributes.PROJECT, project.getName());
	    wc.setAttribute(EmulatorLaunchConfigurationAttributes.PROGRAM, programFile.getProjectRelativePath().toString());

	    return wc.doSave();
	}

}