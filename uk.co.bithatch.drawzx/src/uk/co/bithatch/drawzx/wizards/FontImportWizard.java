package uk.co.bithatch.drawzx.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

public class FontImportWizard extends Wizard implements IWorkbenchWizard {

	private FontImportWizardPage selectionPage;
	private FontImportTargetWizardPage targetPage;
	private IContainer destination;

	public FontImportWizard() {
		setWindowTitle("Import Font");
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if (selection != null && selection.getFirstElement() instanceof IFile file) {
			destination = file.getParent();
		} else if (selection != null && selection.getFirstElement() instanceof IContainer container) {
			destination = container;
		}
	}

	@Override
	public void addPages() {
		selectionPage = new FontImportWizardPage("Font Selection");
		targetPage = new FontImportTargetWizardPage(selectionPage, "Imported Font Destination", destination);
		addPage(selectionPage);
		addPage(targetPage);
	}

	@Override
	public boolean performFinish() {
		var destination = targetPage.getTargetPath();
		var source = selectionPage.getSource();

		try (var in = source.getFontFile().openStream()) {
			destination.create(in, true, null);
			destination.refreshLocal(IResource.DEPTH_ZERO, null);
			return true;
		} catch (Exception e) {
			selectionPage.setErrorMessage("Import failed: " + e.getMessage());
			return false;
		}
	}
}
