package uk.co.bithatch.eclipzx.ui.glue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;

import de.waldheinz.fs.fat.FatType;
import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.fatexplorer.preferences.FATPreferencesAccess;
import uk.co.bithatch.fatexplorer.variables.FATImageContext;
import uk.co.bithatch.zxbasic.ui.api.IPreparationContext;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;
import uk.co.bithatch.zxbasic.ui.util.FileSet;
import uk.co.bithatch.zyxy.lib.MemoryUnit;
import uk.co.bithatch.zyxy.mmc.SDCard;

public class AutomaticFATPreparationTarget extends AbstractFATPreparationTarget {
	private final static ILog LOG = ILog.of(AutomaticFATPreparationTarget.class);

	private URI uri;
	
	@Override
	public IStatus prepare(IProgressMonitor monitor, List<FileSet> files) throws CoreException {
		var efs = getEFSDir(monitor, uri);
		copy(false, monitor, files, uri, efs);
		return Status.OK_STATUS;
	}

	@Override
	public String init(IPreparationContext prepCtx) throws CoreException {
		
		var strmgr = VariablesPlugin.getDefault().getStringVariableManager();
		var configuration = prepCtx.launchConfiguration();
		var pprj = resolveProject(configuration);
		var out = ZXBasicPreferencesAccess.get().getOutputFolder(pprj);
		var outf = out.getFile(Integer.toUnsignedLong(configuration.getName().hashCode()) + ".img");
		var imgfile = outf.getLocation().toPath();
		var imgFullPath = imgfile.toString();
		var apath = outf.getFullPath().toString().substring(1);
		var sizeMb = 64;
		var type = FatType.FAT16;
			
		if(!Files.exists(imgfile)) {
			LOG.info(String.format("Formatting disk image  at %s for type %s with a size of %d MiB", imgfile, type, sizeMb));
			try {
				new SDCard.Builder().
					withCreate().
					withFile(imgfile.toFile()).
					withMBR(true).
					withReadOnly(false).
					withSize(MemoryUnit.MEBIBYTE, 64).
					withFormatter(new SDCard.Formatter.Builder().
						withType(type).
						withOEMName("EclipZX").
						withLabel("V" + Integer.toUnsignedLong(imgfile.hashCode())).
						build()).
					build().close();
			} catch (IOException e) {
				throw new CoreException(Status.error("Failed to create disk image.", e));
			};
		}
		else {
			LOG.info(String.format("Disk image  at %s already exists, using t hat", imgfile));
		}
		
		outf.getParent().refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
		FATPreferencesAccess.addImagePath(apath);
		
		FATImageContext.set(FATImageContext.DEFAULT, "/" + pprj.getName());
		FATImageContext.set(FATImageContext.IMAGE, imgFullPath);
		FATImageContext.set(FATImageContext.FOLDER, strmgr.performStringSubstitution(
				configuration.getAttribute(ConfiguredFATPreparationTargetUI.FAT_FOLDER, "${fat_default}")));
		
		uri = FATPreferencesAccess.encodeToURI(apath);
		
		return super.init(prepCtx);
		
	}
	
	protected IProject resolveProject(ILaunchConfiguration configuration) throws CoreException {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(configuration.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PROJECT, ""));
	}
			
}
