package uk.co.bithatch.eclipzx.ui.intro;

import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.eclipz80.ui.wizard.AsmProjectWizard;

public class CreateAsmProjectAction extends AbstractIntroAction {

	@Override
	public AsmProjectWizard createWizard(IWorkbench workbench) {
        var wizard = new AsmProjectWizard();
        wizard.init(workbench, null);
		return wizard;
	}
	
}
