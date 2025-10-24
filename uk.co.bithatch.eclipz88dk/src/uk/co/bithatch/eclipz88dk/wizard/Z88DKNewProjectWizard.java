package uk.co.bithatch.eclipz88dk.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

public class Z88DKNewProjectWizard extends AbstractZ88DKProjectWizard<Z88DKNewProjectWizardPage> {

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle("New Z88DK Project");
	}

	@Override
	protected Z88DKNewProjectWizardPage createMainPage() {
		return new Z88DKNewProjectWizardPage();
	}

	@Override
	protected CreateTask doProjectCreation(String projectName) {
		// TODO Auto-generated method stub
		return null;
	}

}
