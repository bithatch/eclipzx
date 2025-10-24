package uk.co.bithatch.eclipz88dk.wizard;

import java.io.File;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.eclipz88dk.wizard.CdtProjectCreator.CdtType;
import uk.co.bithatch.widgetzx.util.FileCopyUtil;

public class Z88DKExamplesWizard extends AbstractZ88DKProjectWizard<Z88DKExamplesWizardPage> {

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle("New Z88DK Examples Project");
	}

	@Override
	protected Z88DKExamplesWizardPage createMainPage() {
		return new Z88DKExamplesWizardPage();
	}


	@Override
	protected CreateTask doProjectCreation(String projectName) {
		var sdk = Z88DKPreferencesAccess.get().getAllSDKs().get(page.sdk.getSelectionIndex());
		return (mon) -> {
			var project = CdtProjectCreator.createManagedCProject(CdtType.EXECUTABLE, projectName, mon);
			FileCopyUtil.copyDirectoryToProject(new File(sdk.location(), "examples"), project, mon);
		};
	}

}
