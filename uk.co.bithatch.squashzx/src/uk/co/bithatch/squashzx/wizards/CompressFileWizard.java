package uk.co.bithatch.squashzx.wizards;

import org.eclipse.ui.IExportWizard;

public class CompressFileWizard extends AbstractZX0Wizard<CompressFileWizardPage> implements IExportWizard {


	public CompressFileWizard() {
		super("Compress File With ZX0", "Compressing with ZX0", "Compressing file...", "Compression failed", false);
	}

	@Override
	protected CompressFileWizardPage createPage() {
		return new CompressFileWizardPage("Compress File With ZX0");
	}

}
