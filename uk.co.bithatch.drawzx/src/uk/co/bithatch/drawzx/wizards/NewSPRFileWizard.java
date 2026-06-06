package uk.co.bithatch.drawzx.wizards;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import uk.co.bithatch.drawzx.sprites.SpriteSheet;

public class NewSPRFileWizard extends Wizard implements INewWizard {

	private IWorkbench workbench;
	private IStructuredSelection selection;
	private NewSPRFileWizardPage page;
	private SPRConfigurationWizardPage config;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

	@Override
	public void addPages() {
		page = new NewSPRFileWizardPage("New Spritesheet File", selection);
		page.setFileExtension("spr");
		addPage(page);
		config = new SPRConfigurationWizardPage(page::getFileName, page::getContainerFullPath);
		addPage(config);
	}

	@Override
	public boolean performFinish() {
		var file = page.createNewFile();
		if (file == null)
			return false;

		try {

			var palette = config.getPalette();
			var spr = new SpriteSheet(
				palette, 
				config.getNumberOfSprites(), 
				config.getCellSize(), 
				config.getBPP()
			);

			if (config.isFillTransparency()) {
				var transIdx = palette.transparency().orElse(-1);
				if (transIdx >= 0) {
					for (var cell : spr.cells()) {
						var sz = cell.size();
						for (var y = 0; y < sz; y++) {
							for (var x = 0; x < sz; x++) {
								cell.index(x, y, transIdx);
							}
						}
					}
				}
			}

			var path = file.getLocation().toPath();
			try (var out = Files.newByteChannel(path, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
				spr.save(out);
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
