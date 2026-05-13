package uk.co.bithatch.drawzx.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage; 

public class NewSPRFileWizardPage extends WizardNewFileCreationPage {

    public NewSPRFileWizardPage(String pageName, IStructuredSelection selection) {
        super(pageName, selection);
        setTitle("New Spritesheet");
        setDescription("Create a SPR File. Can contain multiple sprites of a configured sizes and depths.");
    }

    public void setFileExtension(String ext) {
        setFileName("sprites." + ext);
    }

}
