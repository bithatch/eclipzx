package uk.co.bithatch.fatexplorer.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class NewDiskImageWizardPage extends WizardNewFileCreationPage {

    public NewDiskImageWizardPage(String pageName, IStructuredSelection selection) {
        super(pageName, selection);
        setTitle("New Disk Image File");
        setDescription("Create a new empty FAT16 or FAT32 disk image file.");
    }

    @Override
	public void setFileExtension(String ext) {
        setFileName("disk." + ext);
    }

	public IFile createNewFileHandle() {
		return createFileHandle(getContainerFullPath().append(getFileName()));
	}
}
