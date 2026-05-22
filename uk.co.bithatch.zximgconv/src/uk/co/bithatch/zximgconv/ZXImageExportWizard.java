package uk.co.bithatch.zximgconv;

import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Export wizard that converts selected image files to ZX Spectrum screen
 * formats. Configuration mirrors the property page but is specified per-export
 * rather than persisted on the file.
 */
public class ZXImageExportWizard extends Wizard implements IExportWizard {

	private static final ILog LOG = Platform.getLog(ZXImageExportWizard.class);

	static final Set<String> IMAGE_EXTENSIONS = Set.of(
			"png", "gif", "jpg", "jpeg", "bmp", "tiff", "tif", "webp");

	private IStructuredSelection selection;
	private ZXImageExportWizardPage configPage;

	public ZXImageExportWizard() {
		setWindowTitle("Export ZX Image");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	@Override
	public void addPages() {
		configPage = new ZXImageExportWizardPage(selection);
		addPage(configPage);
	}

	@Override
	public boolean performFinish() {
		List<IFile> files = configPage.getSelectedFiles();
		if (files.isEmpty()) return true;

		int formatIndex = configPage.getFormatIndex();
		String outputFolder = configPage.getOutputFolder();
		ZXImageConverter.DitherMode ditherMode = configPage.getDitherMode();
		int l2Resolution = configPage.getL2Resolution();
		String paletteFile = configPage.getPaletteFile();
		boolean generatePalette = configPage.isGeneratePalette();
		boolean embedPalette = configPage.isEmbedPalette();
		boolean transparency = configPage.isTransparency();
		int transparencyIndex = configPage.getTransparencyIndex();
		int alphaThreshold = configPage.getAlphaThreshold();

		try {
			getContainer().run(true, true, monitor -> {
				SubMonitor sub = SubMonitor.convert(monitor, "Exporting ZX images", files.size() * 100);
				for (IFile file : files) {
					if (sub.isCanceled()) break;
					ZXImageConverter.convert(file, formatIndex, outputFolder,
							ditherMode, l2Resolution, null, paletteFile,
							generatePalette, embedPalette, transparency,
							transparencyIndex, 
							alphaThreshold, sub.split(100));
				}
			});
		} catch (Exception e) {
			configPage.setErrorMessage("Export failed: " + e.getMessage());
			return false;
		}
		return true;
	}
}
