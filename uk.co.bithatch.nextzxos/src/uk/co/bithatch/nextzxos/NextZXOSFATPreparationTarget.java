package uk.co.bithatch.nextzxos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;

import de.waldheinz.fs.fat.FatType;
import uk.co.bithatch.bitzx.FileSet;
import uk.co.bithatch.eclipzx.ui.glue.AutomaticFATPreparationTarget;

public class NextZXOSFATPreparationTarget extends AutomaticFATPreparationTarget {
	
	private NextZXOSHelper helper;

	@Override
	protected void beforePrepare(IProgressMonitor monitor, List<FileSet> files) throws CoreException {
		if(helper != null) {
			helper.getImage(monitor);
		}
	}

	@Override
	protected void checkForImage(ILaunchConfiguration configuration, Path imgfile, int sizeMb, FatType type)
			throws CoreException {
		var resetImageState = configuration
				.getAttribute(NextZXOSLaunchConfigurationAttributes.PREPARATION_RESET_IMAGE_STATE, false);
		var baseOnNextZXOS = configuration
				.getAttribute(NextZXOSLaunchConfigurationAttributes.PREPARATION_BASE_ON_NEXT_ZXOS, true);
			
		if(!Files.exists(imgfile) || resetImageState) {
			if(baseOnNextZXOS) {
				helper =  new NextZXOSHelper(configuration, imgfile, baseOnNextZXOS);
			}
			else {
				super.checkForImage(configuration, imgfile, sizeMb, type);
			}
		}
		else {
			super.checkForImage(configuration, imgfile, sizeMb, type);
		}
		
	}
	
			
}
