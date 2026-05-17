package uk.co.bithatch.emuzx.ui;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import uk.co.bithatch.bitzx.FileItem;
import uk.co.bithatch.bitzx.FileSet;
import uk.co.bithatch.bitzx.FileSet.Purpose;
import uk.co.bithatch.emuzx.api.IPreparationContext;
import uk.co.bithatch.emuzx.api.IPreparationSource;

public class AssociatedResourcesPreparationSource implements IPreparationSource {

	@Override
	public void contribute(IPreparationContext ctx, List<FileSet> fileSets, IProgressMonitor monitor) throws CoreException {
		
		var file = ctx.programFile();
		var prj = file.getProject();
		file.getProject().accept(vis -> {
			
			if(ResourceProperties.getProperty(vis, ResourceProperties.DISK_IMAGE_INCLUDE_IN_PREPARATION, false)) {
				/* File is included, is it appropriate for the program we are launching? */
				
				var include = ResourceProperties.getProperty(vis, ResourceProperties.DISK_IMAGE_TRIGGER_ALWAYS, false);
				
				if(!include && file.getFullPath().toString().equals(vis.getFullPath().toString()) &&
				   ResourceProperties.getProperty(vis, ResourceProperties.DISK_IMAGE_TRIGGER_PROGRAMS_IN_THIS_FOLDER, true)) {
					
					/* Included because the file is in the same folder as the program we are launching */
					include = true;
				}
				
				if(!include) {
					for(var other : ResourceProperties.getProperty(vis, ResourceProperties.DISK_IMAGE_OTHER_TRIGGER_PROGRAMS, Collections.emptySet())) {
						var res = prj.findMember(other);
						var fullPath = res.getFullPath().toString().substring(prj.getFullPath().toString().length()).substring(1);
						if(fullPath.equals(other)) {
							include = true;
						}
					}
				}
				
				var folder = ResourceProperties.getProperty(vis, ResourceProperties.DISK_IMAGE_PREPARATION_FOLDER, "");
				var flatten = ResourceProperties.getProperty(vis, ResourceProperties.DISK_IMAGE_FLATTEN_PREPARATION, false);
				
				if(include) {
					fileSets.add(new FileSet(Purpose.ANCILLARY, folder, flatten, new FileItem(vis.getRawLocation().toFile())));
				}
			}
			
			return true;
		}, IResource.DEPTH_INFINITE, false);
		
	}

}
