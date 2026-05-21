package uk.co.bithatch.eclipzx.ui.glue;

import static uk.co.bithatch.bitzx.URIS.stripLeadingSlash;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;

import uk.co.bithatch.bitzx.FileSet;
import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.emuzx.api.IPreparationContext;
import uk.co.bithatch.fatexplorer.FATDiskImageMount;
import uk.co.bithatch.fatexplorer.preferences.FATLock;
import uk.co.bithatch.fatexplorer.preferences.FATPreferencesAccess;
import uk.co.bithatch.fatexplorer.variables.FATImageContext;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

public class ConfiguredFATPreparationTarget extends AbstractFATPreparationTarget {

	private boolean cleanBeforeUse;
	private URI destUri;

	@Override
	public String init(IPreparationContext prepCtx) throws CoreException {

		var strmgr = VariablesPlugin.getDefault().getStringVariableManager();
		var configuration = prepCtx.launchConfiguration();

		var projectName = configuration.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PROJECT, "eclipzx");
		var project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

		FATImageContext.set(FATImageContext.DEFAULT, "/" + projectName);
		FATImageContext.set(FATImageContext.IMAGE,
				configuration.getAttribute(ConfiguredFATPreparationTargetUI.FAT_IMAGE_PATH, ""));
		FATImageContext.set(FATImageContext.IMAGE_NAME, "");

		var folder = strmgr.performStringSubstitution(configuration
				.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_FAT_FOLDER, "${fat_default}"));
		FATImageContext.set(FATImageContext.FOLDER, folder);

		cleanBeforeUse = configuration
				.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_CLEAR_BEFORE_USE, false);

		var path = FATImageContext.get(FATImageContext.IMAGE, "");
		// Build the URI using project context for project-relative paths
		var mount = new FATDiskImageMount("configured", path, false);
		var diskImageUri = mount.toURI(project);

		var lockedDiskImageUri = FATLock.lockImage(diskImageUri);
		destUri = FATPreferencesAccess.resolve(lockedDiskImageUri, FATImageContext.get(FATImageContext.FOLDER, ""));
		prepCtx.addCleanUpTask(() -> {
			FATLock.unlockImage(diskImageUri);
		});

		return "/" + stripLeadingSlash(folder);
	}

	@Override
	public IStatus prepare(IProgressMonitor monitor, List<FileSet> files) throws CoreException {

		var efs = getEFSDir(monitor, destUri);

		try {
			copy(cleanBeforeUse, monitor, files, destUri, efs, FATImageContext.get(FATImageContext.FOLDER, ""));
			return Status.OK_STATUS;
		} finally {
			try {
				efs.close();
			} catch (IOException e) {
				throw new CoreException(Status.error("Failed to close file system for disk image.", e));
			}
		}
	}

	@Override
	public void preparationDone() {
		closeImage(destUri);
	}
}
