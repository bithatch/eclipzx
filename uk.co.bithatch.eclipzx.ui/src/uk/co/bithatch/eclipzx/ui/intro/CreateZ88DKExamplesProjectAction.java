package uk.co.bithatch.eclipzx.ui.intro;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.eclipz88dk.wizard.Z88DKExamplesWizard;

public class CreateZ88DKExamplesProjectAction extends AbstractIntroAction {

	@Override
	public IWizard createWizard(IWorkbench workbench) {
        var wizard = new Z88DKExamplesWizard();
        wizard.init(workbench, null);
        return wizard;
	}

	
}
