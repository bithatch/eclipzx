package uk.co.bithatch.eclipzx.ui.intro;

import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.eclipz88dk.wizard.Z88DKNewProjectWizard;

public class CreateZ88DKProjectAction extends AbstractIntroAction {

	@Override
	public Z88DKNewProjectWizard createWizard(IWorkbench workbench) {
        var wizard = new Z88DKNewProjectWizard();
        wizard.init(workbench, null);
		return wizard;
	}
	
}
