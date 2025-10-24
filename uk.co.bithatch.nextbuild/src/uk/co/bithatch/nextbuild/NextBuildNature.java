package uk.co.bithatch.nextbuild;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class NextBuildNature implements IProjectNature {
	public static final String NATURE_ID = "uk.co.bithatch.nextbuild.NextBuildNature";

	private IProject project;
	
	public NextBuildNature() {} 

	@Override
	public void configure() throws CoreException {
	}

	@Override
	public void deconfigure() throws CoreException {
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}
}
