package uk.co.bithatch.eclipzx.ui.glue;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import uk.co.bithatch.fatexplorer.variables.FATImageContext;
import uk.co.bithatch.fatexplorer.vfs.FATImageFileStore;
import uk.co.bithatch.fatexplorer.vfs.FileOverwritePolicy;
import uk.co.bithatch.fatexplorer.vfs.FileStoreCopyUtil;
import uk.co.bithatch.zxbasic.ui.api.IPreparationContext;
import uk.co.bithatch.zxbasic.ui.preparation.AbstractPreparationTarget;
import uk.co.bithatch.zxbasic.ui.util.FileSet;

public abstract class AbstractFATPreparationTarget extends AbstractPreparationTarget {
	
	private final static ILog LOG = ILog.of(AbstractFATPreparationTarget.class);

	@Override
	public final void cleanUp() {
		FATImageContext.clear();
	}
	
	@Override
	public String init(IPreparationContext prepCtx) throws CoreException {
		return "/";
	}

	protected void copy(boolean cleanBeforeUse, IProgressMonitor monitor, List<FileSet> files, URI uri, FATImageFileStore efs)
			throws CoreException {
		if(cleanBeforeUse) {
			for(var c : efs.childStores(EFS.NONE, monitor)) {
				LOG.info(String.format("Clening %s on %s in preparation for copying to disk image", c.getName(), uri));
				c.delete(EFS.NONE, monitor);
			}
		}

        var remaining = files.size();
		var subMonitor = SubMonitor.convert(monitor, remaining);
		for(var fileSet : files) {
			subMonitor.setWorkRemaining(remaining--);
			
			var dest = efs;
			
			var subfolder = fileSet.destination();
			var filesetUri = uri;

			if(!subfolder.equals("") && !subfolder.equals("\\") && !subfolder.equals("/")) {
				filesetUri = filesetUri.resolve(subfolder.replace("\\", "/"));
				dest = getEFSDir(monitor, filesetUri);
			}
			
			var filesRemain = fileSet.files().length;
			var fileSetSubMonitor = subMonitor.split(filesRemain);
			
			for(var file : flatten(fileSet)) {
				fileSetSubMonitor.setWorkRemaining(filesRemain--);
				try {
					LOG.info(String.format("Copying %s to %s on disk image %s", file.file(), dest, uri));
					FileStoreCopyUtil.copyFileToStore(file.file(), file.targetName(), dest, true, FileOverwritePolicy.always(), fileSetSubMonitor.split(1));
				} catch (IOException e) {
					throw new CoreException(Status.error("Failed to copy file to store.", e));
				}	
			}
		}
	}

	protected FATImageFileStore getEFSDir(IProgressMonitor monitor, URI uri) throws CoreException {
		var efs = (FATImageFileStore) EFS.getStore(uri);
		var info = efs.fetchInfo();
		if(!info.exists()) {
			efs = (FATImageFileStore) efs.mkdir(EFS.NONE, monitor);
		}
		else if(!info.isDirectory()) {
			throw new CoreException(Status.error("Preparation location " + uri + " is not a folder."));
		}
		return efs;
	}
}
