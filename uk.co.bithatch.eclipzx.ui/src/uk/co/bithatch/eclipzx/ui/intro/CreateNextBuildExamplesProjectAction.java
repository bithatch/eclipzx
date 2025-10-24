package uk.co.bithatch.eclipzx.ui.intro;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.nextbuild.NextBuildExamplesWizard;

public class CreateNextBuildExamplesProjectAction extends AbstractIntroAction {


	@Override
	public IWizard createWizard(IWorkbench workbench) {
        var wizard = new NextBuildExamplesWizard();
        wizard.init(workbench, null);
        return wizard;
	}

	
}
