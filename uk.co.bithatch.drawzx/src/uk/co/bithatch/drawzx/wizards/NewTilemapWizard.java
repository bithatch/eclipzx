package uk.co.bithatch.drawzx.wizards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.IDE;

import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.drawzx.tilemaps.Tilemap;
import uk.co.bithatch.drawzx.tilemaps.Tilemap.TilemapMode;

/**
 * Wizard for creating a new ZX Next tilemap (.map) file.
 */
public class NewTilemapWizard extends Wizard implements INewWizard {

	/** Persistent property key for the associated .TIL file path */
	public static final QualifiedName PROP_TIL_PATH = new QualifiedName("uk.co.bithatch.drawzx", "tilPath");

	private IWorkbench workbench;
	private IStructuredSelection selection;
	private NewTilemapWizardPage filePage;
	private TilemapConfigurationWizardPage configPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
		setWindowTitle("New Tilemap");
	}

	@Override
	public void addPages() {
		filePage = new NewTilemapWizardPage("New Tilemap File", selection);
		addPage(filePage);
		configPage = new TilemapConfigurationWizardPage();
		addPage(configPage);
	}

	@Override
	public boolean performFinish() {
		var file = filePage.createNewFile();
		if (file == null)
			return false;

		try {
			var mode = configPage.isStandard40x32() ? TilemapMode.STANDARD_40x32 : TilemapMode.HIRES_80x32;
			var bpp = configPage.getBpp();

			// Always 16-bit entries for new tilemaps (supports attributes)
			var tileDefs = new SpriteSheet(256, bpp);
			var tilemap = new Tilemap(mode, true, tileDefs);

			// Write the empty tilemap data
			var baos = new ByteArrayOutputStream();
			try (var ch = Channels.newChannel(baos)) {
				tilemap.save(ch);
			}
			file.setContents(new ByteArrayInputStream(baos.toByteArray()), true, false, null);

			// Handle tile definitions
			String tilFilePath = null;

			if (configPage.isCreateNewTil()) {
				// Create a new .TIL file alongside the .map file
				var tilName = file.getName().replaceAll("\\.[^.]+$", "") + ".til";
				var tilFile = file.getParent().getFile(new org.eclipse.core.runtime.Path(tilName));

				var tilBaos = new ByteArrayOutputStream();
				try (var ch = Channels.newChannel(tilBaos)) {
					tileDefs.save(ch);
				}
				if (!tilFile.exists()) {
					tilFile.create(new ByteArrayInputStream(tilBaos.toByteArray()), true, null);
				} else {
					tilFile.setContents(new ByteArrayInputStream(tilBaos.toByteArray()), true, false, null);
				}
				tilFilePath = tilFile.getFullPath().toPortableString();

			} else if (configPage.isAttachExistingTil()) {
				tilFilePath = configPage.getTilPath();
			}

			// Store the .TIL path and bpp as persistent properties
			if (tilFilePath != null) {
				file.setPersistentProperty(PROP_TIL_PATH, tilFilePath);
			}
			file.setPersistentProperty(
					new QualifiedName("uk.co.bithatch.drawzx", "bpp"),
					String.valueOf(bpp));

			// Open the editor
			var activePage = workbench.getActiveWorkbenchWindow().getActivePage();
			IDE.openEditor(activePage, file);

		} catch (IOException | CoreException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
}
