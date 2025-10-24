package uk.co.bithatch.eclipzx.ui.glue;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;

import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.fatexplorer.preferences.FATPreferencesAccess;
import uk.co.bithatch.fatexplorer.variables.FATImageContext;
import uk.co.bithatch.zxbasic.ui.api.IPreparationContext;
import uk.co.bithatch.zxbasic.ui.util.FileSet;

public class ConfiguredFATPreparationTarget extends AbstractFATPreparationTarget {

	private boolean cleanBeforeUse;

	@Override
	public String init(IPreparationContext prepCtx) throws CoreException {
		
		var strmgr = VariablesPlugin.getDefault().getStringVariableManager();
		var configuration = prepCtx.launchConfiguration();
		
		FATImageContext.set(FATImageContext.DEFAULT,
				"/" + configuration.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PROJECT, "eclipzx"));
		FATImageContext.set(FATImageContext.IMAGE,
				configuration.getAttribute(ConfiguredFATPreparationTargetUI.FAT_IMAGE_PATH, ""));
		
		var folder = strmgr.performStringSubstitution(
				configuration.getAttribute(ConfiguredFATPreparationTargetUI.FAT_FOLDER, "${fat_default}"));
		FATImageContext.set(FATImageContext.FOLDER, folder);
		
		cleanBeforeUse = configuration
				.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_CLEAR_BEFORE_USE, false);
		
		return folder;
	}

	@Override
	public IStatus prepare(IProgressMonitor monitor, List<FileSet> files) throws CoreException {

		var path = FATImageContext.get(FATImageContext.IMAGE, "");
		var uri = FATPreferencesAccess.encodeToURI(path);
		var folder = FATImageContext.get(FATImageContext.FOLDER, "");
		if (!folder.equals("") && !folder.equals("\\") && !folder.equals("/")) {
			uri = uri.resolve(folder.replace("\\", "/"));
		}
		var efs = getEFSDir(monitor, uri);

		copy(cleanBeforeUse, monitor, files, uri, efs);

		return Status.OK_STATUS;
	}
}
