package uk.co.bithatch.eclipzx.ui.intro;

import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.zxbasic.ui.wizard.BasicProjectWizard;

public class CreateBasicProjectAction extends AbstractIntroAction {

	@Override
	public BasicProjectWizard createWizard(IWorkbench workbench) {
        var wizard = new BasicProjectWizard();
        wizard.init(workbench, null);
		return wizard;
	}
	
}
