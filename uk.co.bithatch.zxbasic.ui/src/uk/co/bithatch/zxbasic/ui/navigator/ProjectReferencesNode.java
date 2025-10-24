package uk.co.bithatch.zxbasic.ui.navigator;

import java.util.Objects;

import org.eclipse.core.resources.IProject;

public class ProjectReferencesNode {
    private final IProject project;

    public ProjectReferencesNode(IProject project) {
        this.project = project;
    }

    public IProject getProject() {
        return project;
    }

    @Override
    public String toString() {
        return "Project Referencess";
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
		ProjectReferencesNode other = (ProjectReferencesNode) obj;
		return Objects.equals(project, other.project);
	}
}
