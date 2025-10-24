package uk.co.bithatch.eclipzx.ui.intro;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.zxbasic.ui.wizard.ZXBasicExamplesWizard;

public class CreateZXBasicExamplesProjectAction extends AbstractIntroAction {


	@Override
	public IWizard createWizard(IWorkbench workbench) {
        var wizard = new ZXBasicExamplesWizard();
        wizard.init(workbench, null);
        return wizard;
	}

	
}
