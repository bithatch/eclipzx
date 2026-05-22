package uk.co.bithatch.zximgconv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

/**
 * Single wizard page for the ZX Image Export wizard, providing the same
 * configuration options as the property page.
 */
public class ZXImageExportWizardPage extends WizardPage {

	private final IStructuredSelection selection;
	private List<IFile> selectedFiles;

	private Combo formatCombo;
	private Text outputFolderText;
	private Combo ditherCombo;
	private Combo l2ResolutionCombo;
	private Label l2ResolutionLabel;
	private Text paletteFileText;
	private Button generatePaletteCheckbox;
	private Label paletteLabel;
	private Composite paletteButtons;
	private Button embedPaletteCheckbox;
	private Button transparencyCheckbox;
	private Spinner transparencyIndexSpinner;
	private Label transparencyIndexLabel;
	private Spinner alphaThresholdSpinner;
	private Label alphaThresholdLabel;

	protected ZXImageExportWizardPage(IStructuredSelection selection) {
		super("zxImageExport");
		setTitle("Export ZX Image");
		setDescription("Convert image files to ZX Spectrum screen formats.");
		this.selection = selection;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		composite.setLayout(layout);

		// --- Selected files info ---
		selectedFiles = collectFiles();
		Label filesLabel = new Label(composite, SWT.WRAP);
		filesLabel.setText(selectedFiles.size() + " image file(s) selected for export.");
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		gd.widthHint = 400;
		filesLabel.setLayoutData(gd);

		// --- Output format ---
		Label formatLabel = new Label(composite, SWT.NONE);
		formatLabel.setText("Output format:");

		formatCombo = new Combo(composite, SWT.READ_ONLY | SWT.DROP_DOWN);
		formatCombo.setItems(ZXImageConverter.OUTPUT_FORMATS);
		formatCombo.select(0);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 2;
		formatCombo.setLayoutData(gd);

		// --- Output folder ---
		Label folderLabel = new Label(composite, SWT.NONE);
		folderLabel.setText("Output folder:");

		outputFolderText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		outputFolderText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite folderButtons = new Composite(composite, SWT.NONE);
		GridLayout folderBtnLayout = new GridLayout(2, true);
		folderBtnLayout.marginWidth = 0;
		folderBtnLayout.marginHeight = 0;
		folderButtons.setLayout(folderBtnLayout);

		Button browseButton = new Button(folderButtons, SWT.PUSH);
		browseButton.setText("Project...");
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IFile first = selectedFiles.isEmpty() ? null : selectedFiles.get(0);
				if (first == null) return;
				ContainerSelectionDialog dialog = new ContainerSelectionDialog(
						getShell(), first.getProject(), true, "Select output folder in project");
				if (dialog.open() == ContainerSelectionDialog.OK) {
					Object[] result = dialog.getResult();
					if (result != null && result.length > 0) {
						IPath ipath = (IPath) result[0];
						String path = ipath.removeFirstSegments(1).makeRelative().toString();
						outputFolderText.setText(path);
					}
				}
			}
		});

		Button extFolderBrowseButton = new Button(folderButtons, SWT.PUSH);
		extFolderBrowseButton.setText("External...");
		extFolderBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.swt.widgets.DirectoryDialog dialog = new org.eclipse.swt.widgets.DirectoryDialog(getShell(), SWT.OPEN);
				dialog.setText("Select external output folder");
				String result = dialog.open();
				if (result != null) {
					outputFolderText.setText(result);
				}
			}
		});

		// --- Dithering ---
		Label ditherLabel = new Label(composite, SWT.NONE);
		ditherLabel.setText("Dithering:");

		ditherCombo = new Combo(composite, SWT.READ_ONLY | SWT.DROP_DOWN);
		ditherCombo.setItems(ZXImageConverter.DitherMode.labels());
		ditherCombo.select(1);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 2;
		ditherCombo.setLayoutData(gd);

		// --- Layer 2 Resolution ---
		l2ResolutionLabel = new Label(composite, SWT.NONE);
		l2ResolutionLabel.setText("Layer 2 resolution:");

		l2ResolutionCombo = new Combo(composite, SWT.READ_ONLY | SWT.DROP_DOWN);
		l2ResolutionCombo.setItems(ZXImageConverter.L2Resolution.labels());
		l2ResolutionCombo.select(0);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 2;
		l2ResolutionCombo.setLayoutData(gd);

		// --- Embed palette ---
		embedPaletteCheckbox = new Button(composite, SWT.CHECK);
		embedPaletteCheckbox.setText("Embed palette in output file");
		embedPaletteCheckbox.setSelection(true);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		embedPaletteCheckbox.setLayoutData(gd);

		formatCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateFormatDependentControls();
			}
		});

		// --- Transparency ---
		transparencyCheckbox = new Button(composite, SWT.CHECK);
		transparencyCheckbox.setText("Enable transparency");
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		transparencyCheckbox.setLayoutData(gd);
		transparencyCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateFormatDependentControls();
			}
		});

		transparencyIndexLabel = new Label(composite, SWT.NONE);
		transparencyIndexLabel.setText("Output transparency index:");
		transparencyIndexSpinner = new Spinner(composite, SWT.BORDER);
		transparencyIndexSpinner.setMinimum(0);
		transparencyIndexSpinner.setMaximum(255);
		transparencyIndexSpinner.setSelection(uk.co.bithatch.zyxy.graphics.Palette.DEFAULT_TRANSPARENCY);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 2;
		transparencyIndexSpinner.setLayoutData(gd);

		alphaThresholdLabel = new Label(composite, SWT.NONE);
		alphaThresholdLabel.setText("Alpha threshold:");
		alphaThresholdSpinner = new Spinner(composite, SWT.BORDER);
		alphaThresholdSpinner.setMinimum(1);
		alphaThresholdSpinner.setMaximum(255);
		alphaThresholdSpinner.setSelection(128);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 2;
		alphaThresholdSpinner.setLayoutData(gd);

		// --- Generate palette ---
		generatePaletteCheckbox = new Button(composite, SWT.CHECK);
		generatePaletteCheckbox.setText("Generate palette from source image");
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		generatePaletteCheckbox.setLayoutData(gd);
		generatePaletteCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updatePaletteControls();
			}
		});

		// --- Custom palette ---
		paletteLabel = new Label(composite, SWT.NONE);
		paletteLabel.setText("Custom palette:");
		paletteFileText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		paletteFileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		paletteButtons = new Composite(composite, SWT.NONE);
		GridLayout palBtnLayout = new GridLayout(1, false);
		palBtnLayout.marginWidth = 0;
		palBtnLayout.marginHeight = 0;
		paletteButtons.setLayout(palBtnLayout);

		Button paletteBrowseButton = new Button(paletteButtons, SWT.PUSH);
		paletteBrowseButton.setText("Browse...");
		paletteBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				dialog.setText("Select Palette File");
				dialog.setFilterExtensions(new String[] { "*.pal;*.npl", "*.*" });
				dialog.setFilterNames(new String[] { "Palette Files (*.pal, *.npl)", "All Files (*.*)" });
				String result = dialog.open();
				if (result != null) {
					paletteFileText.setText(result);
				}
			}
		});

		// --- Hints ---
		Label hint = new Label(composite, SWT.WRAP);
		hint.setText("Leave output folder empty to place converted files next to the source image.\n"
				+ "Relative paths are resolved from the project root. Absolute paths are used as-is.\n\n"
				+ "Tip: You can also configure individual images to convert automatically on build\n"
				+ "via the file's Properties → ZX Image Conversion page.");
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		gd.widthHint = 400;
		hint.setLayoutData(gd);

		// Initial state
		updateFormatDependentControls();
		updatePaletteControls();

		setControl(composite);
	}

	private void updateFormatDependentControls() {
		int idx = formatCombo.getSelectionIndex();
		if (idx >= 0 && idx < ZXImageConverter.OutputFormat.values().length) {
			ZXImageConverter.OutputFormat fmt = ZXImageConverter.OutputFormat.values()[idx];
			l2ResolutionCombo.setEnabled(fmt.supportsL2Resolution());
			l2ResolutionLabel.setEnabled(fmt.supportsL2Resolution());
			embedPaletteCheckbox.setEnabled(fmt.supportsEmbeddedPalette());

			boolean transSupported = fmt.supportsTransparency();
			transparencyCheckbox.setEnabled(transSupported);
			boolean transEnabled = transSupported && transparencyCheckbox.getSelection();
			transparencyIndexLabel.setEnabled(transEnabled);
			transparencyIndexSpinner.setEnabled(transEnabled);
			alphaThresholdLabel.setEnabled(transEnabled);
			alphaThresholdSpinner.setEnabled(transEnabled);
		}
	}

	private void updatePaletteControls() {
		boolean genPal = generatePaletteCheckbox.getSelection();
		paletteLabel.setEnabled(!genPal);
		paletteFileText.setEnabled(!genPal);
		paletteButtons.setEnabled(!genPal);
		for (org.eclipse.swt.widgets.Control child : paletteButtons.getChildren()) {
			child.setEnabled(!genPal);
		}
	}

	private List<IFile> collectFiles() {
		List<IFile> files = new ArrayList<>();
		if (selection == null) return files;
		for (Iterator<?> it = selection.iterator(); it.hasNext(); ) {
			Object obj = it.next();
			if (obj instanceof IResource res) {
				if (res instanceof IFile file) {
					String ext = file.getFileExtension();
					if (ext != null && ZXImageExportWizard.IMAGE_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
						files.add(file);
					}
				}
			}
		}
		return files;
	}

	// --- Getters for wizard ---

	public List<IFile> getSelectedFiles() { return selectedFiles; }
	public int getFormatIndex() { return formatCombo.getSelectionIndex(); }
	public String getOutputFolder() { return outputFolderText.getText().trim(); }
	public ZXImageConverter.DitherMode getDitherMode() {
		int idx = ditherCombo.getSelectionIndex();
		return idx >= 0 ? ZXImageConverter.DitherMode.values()[idx] : ZXImageConverter.DitherMode.FLOYD_STEINBERG;
	}
	public int getL2Resolution() { return l2ResolutionCombo.getSelectionIndex(); }
	public String getPaletteFile() { return paletteFileText.getText().trim(); }
	public boolean isGeneratePalette() { return generatePaletteCheckbox.getSelection(); }
	public boolean isEmbedPalette() { return embedPaletteCheckbox.getSelection(); }
	public boolean isTransparency() { return transparencyCheckbox.getSelection(); }
	public int getTransparencyIndex() { return transparencyIndexSpinner.getSelection(); }
	public int getAlphaThreshold() { return alphaThresholdSpinner.getSelection(); }
}
