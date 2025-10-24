package uk.co.bithatch.zxbasic.ui.preparation;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import uk.co.bithatch.zxbasic.ui.api.IPreparationContext;
import uk.co.bithatch.zxbasic.ui.api.IPreparationSource;
import uk.co.bithatch.zxbasic.ui.builder.ResourceProperties;
import uk.co.bithatch.zxbasic.ui.util.FileItem;
import uk.co.bithatch.zxbasic.ui.util.FileSet;
import uk.co.bithatch.zxbasic.ui.util.FileSet.Purpose;

public class AssociatedResourcesPreparationSource implements IPreparationSource {

	@Override
	public void contribute(IPreparationContext ctx, List<FileSet> fileSets, IProgressMonitor monitor) throws CoreException {
		
		var file = ctx.programFile();
		file.getProject().accept(vis -> {
			
			if(ResourceProperties.getProperty(vis, ResourceProperties.DISK_IMAGE_INCLUDE_IN_PREPARATION, false)) {
				/* File is included, is it appropriate for the program we are launching? */
				
				boolean include = false;
				
				if(file.getFullPath().toString().equals(vis.getFullPath().toString()) &&
				   ResourceProperties.getProperty(vis, ResourceProperties.DISK_IMAGE_TRIGGER_PROGRAMS_IN_THIS_FOLDER, true)) {
					
					/* Included because the file is in the same folder as the program we are launching */
					include = true;
				}
				
				for(var other : ResourceProperties.getProperty(file, ResourceProperties.DISK_IMAGE_OTHER_TRIGGER_PROGRAMS, Collections.emptySet())) {
					var res = file.getProject().findMember(other);
					if(res.getFullPath().toString().equals(other)) {
						include = true;
					}
				}
				
				var folder = ResourceProperties.getProperty(vis, ResourceProperties.DISK_IMAGE_PREPARATION_FOLDER, "");
				var flatten = ResourceProperties.getProperty(vis, ResourceProperties.DISK_IMAGE_FLATTEN_PREPARATION, false);
				
				if(include) {
					fileSets.add(new FileSet(Purpose.ANCILLARY, folder, flatten, new FileItem(file.getRawLocation().toFile())));
				}
			}
			
			return true;
		}, IResource.DEPTH_INFINITE, false);
		
	}

}
