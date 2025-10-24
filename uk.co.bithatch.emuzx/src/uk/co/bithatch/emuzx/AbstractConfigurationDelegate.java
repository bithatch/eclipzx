package uk.co.bithatch.emuzx;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

public abstract class AbstractConfigurationDelegate extends LaunchConfigurationDelegate {
	
	private final String projectKey;
	private final String programKey;
	
	protected AbstractConfigurationDelegate(String projectKey, String programKey) {
		this.programKey = programKey;
		this.projectKey = projectKey;
	}

	@Override
	public final void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		var strmgr = VariablesPlugin.getDefault().getStringVariableManager();
		
		var projectName = configuration.getAttribute(projectKey, (String) null);
	    if (projectName == null || projectName.equals("")) {
	        throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Project name not specified"));
	    }

	    var project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	    if (project == null || !project.exists()) {
	        throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Project '" + projectName + "' does not exist"));
	    }
	    var programName = configuration.getAttribute(programKey, (String) null);
	    if (programName == null || programName.equals("")) {
	        throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Program name not specified"));
	    }

	    // Now you can use the project
	    var programFile = project.getFile(strmgr.performStringSubstitution(programName));
	    if (!programFile.exists()) {
	        throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "'" + programName + "' does not exist in the project"));
	    }
		    
		    
		launch(programFile, configuration, mode, launch, monitor);
	}

	public abstract void launch(IFile file, ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException;
}
