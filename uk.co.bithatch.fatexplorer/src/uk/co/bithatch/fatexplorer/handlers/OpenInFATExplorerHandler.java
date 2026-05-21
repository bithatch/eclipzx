package uk.co.bithatch.fatexplorer.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.fatexplorer.FATDiskImageManager;
import uk.co.bithatch.fatexplorer.FATDiskImageMount;

public class OpenInFATExplorerHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection sel) {
			var element = sel.getFirstElement();
			if (element instanceof IFile file) {
				var project = file.getProject();
				// Use project-relative path
				var path = file.getProjectRelativePath().toPortableString();

				// Derive a name from the filename
				var fname = file.getName();
				var dot = fname.lastIndexOf('.');
				var name = dot > 0 ? fname.substring(0, dot) : fname;

				// Check if already mounted
				var mounts = FATDiskImageManager.getMounts(project);
				var existing = mounts.stream().filter(m -> m.getImagePath().equals(path)).findFirst();
				if (existing.isEmpty()) {
					var mount = new FATDiskImageMount(name, path, true);
					mounts.add(mount);
					FATDiskImageManager.saveMounts(project, mounts);
					var job = Job.create("Mounting disk image: " + name, monitor -> {
						try {
							FATDiskImageManager.mount(project, mount);
						} catch (Exception ex) {
							return Status.error("Failed to mount '" + name + "'", ex);
						}
						return Status.OK_STATUS;
					});
					job.setUser(true);
					job.schedule();
				}
			}
		}
		return null;
	}
}