package uk.co.bithatch.drawzx.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class NewTilemapWizardPage extends WizardNewFileCreationPage {

	public NewTilemapWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle("New Tilemap");
		setDescription("Create a new ZX Next tilemap file (.map).");
		setFileName("tilemap.map");
	}
}
