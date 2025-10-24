package uk.co.bithatch.zxbasic.ui.navigator;

import java.util.Objects;

import org.eclipse.core.resources.IProject;

public class LibrariesNode {
    private final IProject project;

    public LibrariesNode(IProject project) {
        this.project = project;
    }

    public IProject getProject() {
        return project;
    }

    @Override
    public String toString() {
        return "Libraries";
    }

	@Override
	public int hashCode() {
		return Objects.hash(project);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LibrariesNode other = (LibrariesNode) obj;
		return Objects.equals(project, other.project);
	}
}
