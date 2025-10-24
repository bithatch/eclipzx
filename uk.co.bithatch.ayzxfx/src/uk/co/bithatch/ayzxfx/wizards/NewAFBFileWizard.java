package uk.co.bithatch.ayzxfx.wizards;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import uk.co.bithatch.ayzxfx.ay.AFB;

public class NewAFBFileWizard extends Wizard implements INewWizard {

	private IWorkbench workbench;
	private IStructuredSelection selection;
	private NewAFBFileWizardPage page;
	private AFBConfigurationWizardPage config;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

	@Override
	public void addPages() {
		page = new NewAFBFileWizardPage("New AFB File", selection);
		page.setFileExtension("afb");
		addPage(page);
		config = new AFBConfigurationWizardPage();
		addPage(config);
	}

	@Override
	public boolean performFinish() {
		var file = page.createNewFile();
		if (file == null)
			return false;

		try {

			var afb = AFB.create(config.getNumberOfEffects(), config.getNumberOfFrames());
			try (var out = Files.newByteChannel(file.getLocation().toPath(), StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
				afb.save(out);
			}

			var activePage = workbench.getActiveWorkbenchWindow().getActivePage();
			IDE.openEditor(activePage, file);
		} catch (IOException | PartInitException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
