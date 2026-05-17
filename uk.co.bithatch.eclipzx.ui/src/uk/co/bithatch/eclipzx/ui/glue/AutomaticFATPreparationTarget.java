package uk.co.bithatch.eclipzx.ui.glue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
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
import uk.co.bithatch.bitzx.FileSet;
import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.bitzx.URIS;
import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.emuzx.api.IPreparationContext;
import uk.co.bithatch.fatexplorer.preferences.FATLock;
import uk.co.bithatch.fatexplorer.preferences.FATPreferencesAccess;
import uk.co.bithatch.fatexplorer.variables.FATImageContext;
import uk.co.bithatch.zyxy.lib.MemoryUnit;
import uk.co.bithatch.zyxy.mmc.SDCard;
import uk.co.bithatch.zyxy.mmc.SDCard.Builder;

public class AutomaticFATPreparationTarget extends AbstractFATPreparationTarget {
	private final static ILog LOG = ILog.of(AutomaticFATPreparationTarget.class);

	protected URI uri;
	protected Builder formatter;
	
	@Override
	public final IStatus prepare(IProgressMonitor monitor, List<FileSet> files) throws CoreException {
		beforePrepare(monitor, files);
		
		if(formatter != null) {
			try {
				formatter.build().close();
			} catch (IOException e) {
				throw new CoreException(Status.error("Failed to format disk image.", e));
			}
		}
		
		var efs = getEFSDir(monitor, uri);
		try {
			copy(false, monitor, files, uri, efs, FATImageContext.get(FATImageContext.FOLDER, ""));
			return Status.OK_STATUS;
		}
		finally {
			try {
				efs.close();
			} catch (IOException e) {
				throw new CoreException(Status.error("Failed to close file system for disk image.", e));
			}
		}
		
	}

	protected void beforePrepare(IProgressMonitor monitor, List<FileSet> files) throws CoreException {
		//
	}

	@Override
	public final String init(IPreparationContext prepCtx) throws CoreException {
		
		var strmgr = VariablesPlugin.getDefault().getStringVariableManager();
//		for(var var : strmgr.getVariables()) {
//			try {
//			LOG.info(String.format("Available variable: %s=%s", var.getName(), strmgr.performStringSubstitution("${" + var.getName() + "}")));
//			}
//			catch(CoreException e) {
//				LOG.error(String.format("Failed to resolve variable %s", var.getName()), e);
//			}
//		}
		var configuration = prepCtx.launchConfiguration();
		var pprj = resolveProject(configuration);
		var prefs = LanguageSystem.languageSystem(pprj).preferenceAccess();
		var out = prefs.getOutputFolder(pprj);
		FATImageContext.set(FATImageContext.PROJECT, pprj.getName());
		var outfilename = strmgr.performStringSubstitution(
				configuration.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_IMAGE_NAME, "${ProjName}.img"));
		var outf = out.getFile(outfilename);
		var imgfile = outf.getLocation().toPath();
		var imgFullPath = imgfile.toString();
		var sizeMb = 64;
		var type = FatType.FAT16;

		var apath = outf.getFullPath().toString().substring(1);
		var diskImageUri = FATPreferencesAccess.encodeToURI(apath);
		uri = FATLock.lockImage(diskImageUri);
		
		try {
			checkForImage(configuration, imgfile, sizeMb, type);
		
			outf.getParent().refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
			FATPreferencesAccess.addImagePath(apath);
			
			FATImageContext.set(FATImageContext.DEFAULT, "/" + pprj.getName());
			FATImageContext.set(FATImageContext.IMAGE, imgFullPath);
			FATImageContext.set(FATImageContext.IMAGE_NAME, outfilename);
			FATImageContext.set(FATImageContext.FOLDER, strmgr.performStringSubstitution(
					configuration.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_FAT_FOLDER, "${fat_default}")));
			
			
			
			prepCtx.addCleanUpTask(() -> {
				FATLock.unlockImage(uri);
			});
			
			return super.init(prepCtx) + URIS.stripLeadingSlash(FATImageContext.get(FATImageContext.FOLDER, ""));
		}
		catch(CoreException e) {
			FATLock.unlockImage(uri);
			throw e;
		}
		catch(RuntimeException e) {
			FATLock.unlockImage(uri);
			throw e;
		}
	}

	protected void checkForImage(ILaunchConfiguration configuration, Path imgfile, int sizeMb, FatType type)
			throws CoreException {
		if(!Files.exists(imgfile)) {
			LOG.info(String.format("Will Format disk image  at %s for type %s with a size of %d MiB", imgfile, type, sizeMb));
			formatter = new SDCard.Builder().
				withCreate().
				withFile(imgfile.toFile()).
				withMBR(true).
				withReadOnly(false).
				withSize(MemoryUnit.MEBIBYTE, 64).
				withFormatter(new SDCard.Formatter.Builder().
					withType(type).
					withOEMName("EclipZX").
					withLabel(calcImageLabel(imgfile)).
					build());
		}
		else {
			LOG.info(String.format("Disk image  at %s already exists, using that", imgfile));
		}
	}

	protected String calcImageLabel(Path imgfile) {
		var fn = imgfile.getFileName().toString();
		var idx = fn.lastIndexOf('.');
		if(idx > 0) {
			fn = fn.substring(0, idx);
		}
		if(fn.length() > 11) {
			fn = fn.substring(0, 11);
		}
		return fn;
	}
	
	protected final IProject resolveProject(ILaunchConfiguration configuration) throws CoreException {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(configuration.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PROJECT, ""));
	}
			
}
