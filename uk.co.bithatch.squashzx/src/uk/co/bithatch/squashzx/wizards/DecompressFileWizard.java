package uk.co.bithatch.squashzx.wizards;

import org.eclipse.ui.IExportWizard;

public class DecompressFileWizard extends AbstractZX0Wizard<DecompressFileWizardPage> implements IExportWizard {


	public DecompressFileWizard() {
		super("Decompress File With ZX0", "Decompressing with ZX0", "Decompressing file...", "Decompression failed", true);
	}

	@Override
	protected DecompressFileWizardPage createPage() {
		return new DecompressFileWizardPage("Decompress File With ZX0");
	}

}
