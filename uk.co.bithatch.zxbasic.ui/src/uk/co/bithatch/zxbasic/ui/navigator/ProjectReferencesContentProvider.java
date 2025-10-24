package uk.co.bithatch.zxbasic.ui.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;

public class ProjectReferencesContentProvider implements ITreeContentProvider {

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IProject project && project.isOpen()) {
            return new Object[] { new ProjectReferencesNode(project) };
        }
        else if (parentElement instanceof ProjectReferencesNode node) {
            return resolveLibraryFolders(node.getProject());
        }
        return new Object[0];
    }

    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof IProject prj && prj.isOpen()) || (element instanceof ProjectReferencesNode);
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    private Object[] resolveLibraryFolders(IProject project) {
        try {
			return project.getDescription().getReferencedProjects();
		} catch (CoreException e) {
			return new Object[0];
		}
    }

	@Override
	public Object getParent(Object element) {
		if(element instanceof ProjectReferencesNode prn) {
			return prn.getProject();
		}
		return null;
	}
}
