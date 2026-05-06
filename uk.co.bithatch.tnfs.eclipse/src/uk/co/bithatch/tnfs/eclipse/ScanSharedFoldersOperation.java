package uk.co.bithatch.tnfs.eclipse;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class ScanSharedFoldersOperation extends WorkspaceModifyOperation {

    private final List<IContainer> sharedContainers = new ArrayList<>();

    public List<IContainer> getSharedContainers() {
        return Collections.unmodifiableList(sharedContainers);
    }

    @Override
    protected void execute(IProgressMonitor monitor)
            throws CoreException, InvocationTargetException, InterruptedException {

        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        SubMonitor sub = SubMonitor.convert(
                monitor,
                "Scanning for TNFS shared folders",
                IProgressMonitor.UNKNOWN);

        try {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IProject[] projects = root.getProjects();

            for (IProject project : projects) {
                if (sub.isCanceled()) {
                    throw new OperationCanceledException();
                }

                if (!project.isOpen()) {
                    continue;
                }

                sub.subTask("Scanning project " + project.getName());
                // Each project gets its own child monitor
                scanContainer(project, sub.split(1));
            }

            // At this point sharedContainers is filled.
            // Enable/disable your server here or outside via getSharedContainers()
            // e.g.:
             TNFSActivator activator = TNFSActivator.getDefault();
			 activator.setSharecContainers(sharedContainers);
			 activator.updateState(sub);
            
        }
        catch(Exception e) {
        	throw new CoreException(Status.error("Failed to update TNFS server state.", e));

        } finally {
            sub.done();
        }
    }

    private void scanContainer(IContainer container, IProgressMonitor monitor)
            throws CoreException {

        SubMonitor sub = SubMonitor.convert(
                monitor,
                "Scanning " + container.getFullPath().toString(),
                IProgressMonitor.UNKNOWN);

        if (sub.isCanceled()) {
            throw new OperationCanceledException();
        }

        // Check if this container is shared.
        if (isShared(container)) {
            sharedContainers.add(container);
            // Optimization: children are implicitly shared, don't descend further.
            sub.worked(1);
            return;
        }

        // Not shared => we have to check its children.
        IResource[] members = container.members();
        sub.setWorkRemaining(members.length);

        for (IResource member : members) {
            if (sub.isCanceled()) {
                throw new OperationCanceledException();
            }

            if (member instanceof IContainer) {
                // Recurse into sub-folders / nested projects
                scanContainer((IContainer) member, sub.split(1));
            } else {
                // Files: just tick the monitor
                sub.worked(1);
            }
        }
    }

    private boolean isShared(IResource resource) throws CoreException {
        return TNFSResourceProperties.getProperty(resource, TNFSResourceProperties.SHARED, false);
    }
}
