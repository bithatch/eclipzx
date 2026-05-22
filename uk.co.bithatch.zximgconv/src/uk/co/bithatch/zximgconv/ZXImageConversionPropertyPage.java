package uk.co.bithatch.zximgconv;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * A property page shown for image files (PNG, GIF, JPEG, etc.) that lets the
 * user configure ZX Spectrum image conversion settings stored as persistent
 * properties on the {@link IFile}.
 */
public class ZXImageConversionPropertyPage extends PropertyPage {

	private Button convertCheckbox;
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

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);

		// --- Convert on build checkbox ---
		convertCheckbox = new Button(composite, SWT.CHECK);
		convertCheckbox.setText("Convert this image on build");
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		convertCheckbox.setLayoutData(gd);

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
				IFile file = getFile();
				if (file == null) return;
				ContainerSelectionDialog dialog = new ContainerSelectionDialog(
						getShell(), file.getProject(), true, "Select output folder in project");
				if (dialog.open() == ContainerSelectionDialog.OK) {
					Object[] result = dialog.getResult();
					if (result != null && result.length > 0) {
						IPath ipath = (IPath) result[0];
						// Make project-relative by removing the first segment (project name)
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
				dialog.setMessage("Choose an external folder for converted files");
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
		ditherCombo.select(1); // Default to Floyd-Steinberg
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

		// --- Embed palette checkbox ---
		embedPaletteCheckbox = new Button(composite, SWT.CHECK);
		embedPaletteCheckbox.setText("Embed palette in output file");
		embedPaletteCheckbox.setSelection(true); // default true
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		embedPaletteCheckbox.setLayoutData(gd);

		// Enable/disable L2 resolution and embed palette based on format selection
		formatCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateL2ResolutionEnabled();
				updateEmbedPaletteEnabled();
				updateTransparencyEnabled();
			}
		});

		// --- Transparency checkbox ---
		transparencyCheckbox = new Button(composite, SWT.CHECK);
		transparencyCheckbox.setText("Enable transparency");
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		transparencyCheckbox.setLayoutData(gd);
		transparencyCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateTransparencyEnabled();
			}
		});

		// --- Transparency index (output) ---
		transparencyIndexLabel = new Label(composite, SWT.NONE);
		transparencyIndexLabel.setText("Output transparency index:");

		transparencyIndexSpinner = new Spinner(composite, SWT.BORDER);
		transparencyIndexSpinner.setMinimum(0);
		transparencyIndexSpinner.setMaximum(255);
		transparencyIndexSpinner.setSelection(uk.co.bithatch.zyxy.graphics.Palette.DEFAULT_TRANSPARENCY);
		transparencyIndexSpinner.setToolTipText("Palette index in the output image that represents transparency (ZX Next default is 227 / 0xE3)");
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 2;
		transparencyIndexSpinner.setLayoutData(gd);

		// --- Alpha threshold ---
		alphaThresholdLabel = new Label(composite, SWT.NONE);
		alphaThresholdLabel.setText("Alpha threshold:");

		alphaThresholdSpinner = new Spinner(composite, SWT.BORDER);
		alphaThresholdSpinner.setMinimum(1);
		alphaThresholdSpinner.setMaximum(255);
		alphaThresholdSpinner.setSelection(128);
		alphaThresholdSpinner.setToolTipText("Alpha values below this threshold are treated as transparent (used for formats with alpha channel like PNG)");
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 2;
		alphaThresholdSpinner.setLayoutData(gd);

		// --- Generate palette checkbox ---
		generatePaletteCheckbox = new Button(composite, SWT.CHECK);
		generatePaletteCheckbox.setText("Generate palette from source image");
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		generatePaletteCheckbox.setLayoutData(gd);
		generatePaletteCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updatePaletteControlsEnabled();
			}
		});

		// --- Custom palette file ---
		paletteLabel = new Label(composite, SWT.NONE);
		paletteLabel.setText("Custom palette:");

		paletteFileText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		paletteFileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		paletteButtons = new Composite(composite, SWT.NONE);
		GridLayout palBtnLayout = new GridLayout(2, true);
		palBtnLayout.marginWidth = 0;
		palBtnLayout.marginHeight = 0;
		paletteButtons.setLayout(palBtnLayout);

		Button paletteWsBrowseButton = new Button(paletteButtons, SWT.PUSH);
		paletteWsBrowseButton.setText("Workspace...");
		paletteWsBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.ui.dialogs.ResourceSelectionDialog dialog =
						new org.eclipse.ui.dialogs.ResourceSelectionDialog(
								getShell(),
								org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot(),
								"Select a palette file from the workspace");
				if (dialog.open() == org.eclipse.ui.dialogs.ResourceSelectionDialog.OK) {
					Object[] result = dialog.getResult();
					if (result != null && result.length > 0) {
						// ResourceSelectionDialog returns IResource objects
						IResource res = (IResource) result[0];
						String path = res.getFullPath().makeRelative().toString();
						paletteFileText.setText(path);
					}
				}
			}
		});

		Button paletteBrowseButton = new Button(paletteButtons, SWT.PUSH);
		paletteBrowseButton.setText("External...");
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

		// --- Hint labels ---
		Label hint = new Label(composite, SWT.WRAP);
		hint.setText("Leave output folder empty to place converted files next to the source image.\n"
				+ "Relative paths are resolved from the project root (e.g. gfx/converted). Absolute paths are used as-is.");
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		gd.widthHint = 400;
		hint.setLayoutData(gd);

		Label paletteHint = new Label(composite, SWT.WRAP);
		paletteHint.setText("Leave custom palette empty to use the default palette for the selected format.\n"
				+ "Supports .pal (9-bit) and .npl (9-bit with transparency) palette files.\n"
				+ "Relative paths are resolved from the project root. Absolute paths are used as-is.\n"
				+ "When 'Generate palette' is checked, a .npl file is created alongside the output.");
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		gd.widthHint = 400;
		paletteHint.setLayoutData(gd);

		// Load stored values
		loadProperties();

		// Initial state
		updateL2ResolutionEnabled();
		updateEmbedPaletteEnabled();
		updateTransparencyEnabled();
		updatePaletteControlsEnabled();

		return composite;
	}

	private void updateL2ResolutionEnabled() {
		int idx = formatCombo.getSelectionIndex();
		if (idx >= 0 && idx < ZXImageConverter.OutputFormat.values().length) {
			boolean enabled = ZXImageConverter.OutputFormat.values()[idx].supportsL2Resolution();
			l2ResolutionCombo.setEnabled(enabled);
			l2ResolutionLabel.setEnabled(enabled);
		}
	}

	private void updateEmbedPaletteEnabled() {
		int idx = formatCombo.getSelectionIndex();
		if (idx >= 0 && idx < ZXImageConverter.OutputFormat.values().length) {
			boolean supported = ZXImageConverter.OutputFormat.values()[idx].supportsEmbeddedPalette();
			embedPaletteCheckbox.setEnabled(supported);
		}
	}

	private void updateTransparencyEnabled() {
		int idx = formatCombo.getSelectionIndex();
		boolean supported = false;
		if (idx >= 0 && idx < ZXImageConverter.OutputFormat.values().length) {
			supported = ZXImageConverter.OutputFormat.values()[idx].supportsTransparency();
		}
		transparencyCheckbox.setEnabled(supported);
		boolean enabled = supported && transparencyCheckbox.getSelection();
		transparencyIndexLabel.setEnabled(enabled);
		transparencyIndexSpinner.setEnabled(enabled);
		alphaThresholdLabel.setEnabled(enabled);
		alphaThresholdSpinner.setEnabled(enabled);
	}

	private void updatePaletteControlsEnabled() {
		boolean genPal = generatePaletteCheckbox.getSelection();
		paletteLabel.setEnabled(!genPal);
		paletteFileText.setEnabled(!genPal);
		paletteButtons.setEnabled(!genPal);
		for (org.eclipse.swt.widgets.Control child : paletteButtons.getChildren()) {
			child.setEnabled(!genPal);
		}
	}

	private IFile getFile() {
		IResource resource = getElement().getAdapter(IResource.class);
		if (resource instanceof IFile file) {
			return file;
		}
		return null;
	}

	private void loadProperties() {
		IFile file = getFile();
		if (file == null) return;
		try {
			String convert = file.getPersistentProperty(qn(ZXImageConversionBuilder.PROP_CONVERT_ON_BUILD));
			convertCheckbox.setSelection("true".equals(convert));

			String format = file.getPersistentProperty(qn(ZXImageConversionBuilder.PROP_OUTPUT_FORMAT));
			if (format != null) {
				try {
					int idx = Integer.parseInt(format);
					if (idx >= 0 && idx < ZXImageConverter.OUTPUT_FORMATS.length) {
						formatCombo.select(idx);
					}
				} catch (NumberFormatException e) {
					// ignore
				}
			}

			String dither = file.getPersistentProperty(qn(ZXImageConversionBuilder.PROP_DITHER_MODE));
			if (dither != null) {
				try {
					int idx = Integer.parseInt(dither);
					if (idx >= 0 && idx < ZXImageConverter.DitherMode.values().length) {
						ditherCombo.select(idx);
					}
				} catch (NumberFormatException e) {
					// ignore
				}
			}

			String l2Res = file.getPersistentProperty(qn(ZXImageConversionBuilder.PROP_L2_RESOLUTION));
			if (l2Res != null) {
				try {
					int idx = Integer.parseInt(l2Res);
					if (idx >= 0 && idx < ZXImageConverter.L2Resolution.values().length) {
						l2ResolutionCombo.select(idx);
					}
				} catch (NumberFormatException e) {
					// ignore
				}
			}

			String folder = file.getPersistentProperty(qn(ZXImageConversionBuilder.PROP_OUTPUT_FOLDER));
			if (folder != null) {
				outputFolderText.setText(folder);
			}

			String palFile = file.getPersistentProperty(qn(ZXImageConversionBuilder.PROP_PALETTE_FILE));
			if (palFile != null) {
				paletteFileText.setText(palFile);
			}

			String genPal = file.getPersistentProperty(qn(ZXImageConversionBuilder.PROP_GENERATE_PALETTE));
			generatePaletteCheckbox.setSelection("true".equals(genPal));

			String embedPal = file.getPersistentProperty(qn(ZXImageConversionBuilder.PROP_EMBED_PALETTE));
			embedPaletteCheckbox.setSelection(embedPal == null || "true".equals(embedPal)); // default true

			String trans = file.getPersistentProperty(qn(ZXImageConversionBuilder.PROP_TRANSPARENCY));
			transparencyCheckbox.setSelection("true".equals(trans));

			String transIdx = file.getPersistentProperty(qn(ZXImageConversionBuilder.PROP_TRANSPARENCY_INDEX));
			if (transIdx != null) {
				try {
					int idx = Integer.parseInt(transIdx);
					if (idx >= 0 && idx <= 255) {
						transparencyIndexSpinner.setSelection(idx);
					}
				} catch (NumberFormatException e) {
					// ignore
				}
			}

			String alphaThresh = file.getPersistentProperty(qn(ZXImageConversionBuilder.PROP_ALPHA_THRESHOLD));
			if (alphaThresh != null) {
				try {
					int val = Integer.parseInt(alphaThresh);
					if (val >= 1 && val <= 255) {
						alphaThresholdSpinner.setSelection(val);
					}
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		} catch (CoreException e) {
			setErrorMessage("Failed to load properties: " + e.getMessage());
		}
	}

	@Override
	public boolean performOk() {
		IFile file = getFile();
		if (file == null) return true;
		try {
			file.setPersistentProperty(qn(ZXImageConversionBuilder.PROP_CONVERT_ON_BUILD),
					String.valueOf(convertCheckbox.getSelection()));
			file.setPersistentProperty(qn(ZXImageConversionBuilder.PROP_OUTPUT_FORMAT),
					String.valueOf(formatCombo.getSelectionIndex()));
			file.setPersistentProperty(qn(ZXImageConversionBuilder.PROP_OUTPUT_FOLDER),
					outputFolderText.getText().trim());
			file.setPersistentProperty(qn(ZXImageConversionBuilder.PROP_DITHER_MODE),
					String.valueOf(ditherCombo.getSelectionIndex()));
			file.setPersistentProperty(qn(ZXImageConversionBuilder.PROP_L2_RESOLUTION),
					String.valueOf(l2ResolutionCombo.getSelectionIndex()));
			file.setPersistentProperty(qn(ZXImageConversionBuilder.PROP_PALETTE_FILE),
					paletteFileText.getText().trim());
			file.setPersistentProperty(qn(ZXImageConversionBuilder.PROP_GENERATE_PALETTE),
					String.valueOf(generatePaletteCheckbox.getSelection()));
			file.setPersistentProperty(qn(ZXImageConversionBuilder.PROP_EMBED_PALETTE),
					String.valueOf(embedPaletteCheckbox.getSelection()));
			file.setPersistentProperty(qn(ZXImageConversionBuilder.PROP_TRANSPARENCY),
					String.valueOf(transparencyCheckbox.getSelection()));
			file.setPersistentProperty(qn(ZXImageConversionBuilder.PROP_TRANSPARENCY_INDEX),
					String.valueOf(transparencyIndexSpinner.getSelection()));
			file.setPersistentProperty(qn(ZXImageConversionBuilder.PROP_ALPHA_THRESHOLD),
					String.valueOf(alphaThresholdSpinner.getSelection()));
		} catch (CoreException e) {
			setErrorMessage("Failed to save properties: " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	protected void performDefaults() {
		convertCheckbox.setSelection(false);
		formatCombo.select(0);
		ditherCombo.select(1); // Floyd-Steinberg
		l2ResolutionCombo.select(0);
		outputFolderText.setText("");
		paletteFileText.setText("");
		generatePaletteCheckbox.setSelection(false);
		embedPaletteCheckbox.setSelection(true);
		transparencyCheckbox.setSelection(false);
		transparencyIndexSpinner.setSelection(uk.co.bithatch.zyxy.graphics.Palette.DEFAULT_TRANSPARENCY);
		alphaThresholdSpinner.setSelection(128);
		updateL2ResolutionEnabled();
		updateEmbedPaletteEnabled();
		updateTransparencyEnabled();
		updatePaletteControlsEnabled();
		super.performDefaults();
	}

	private static QualifiedName qn(String localName) {
		return new QualifiedName(Activator.PLUGIN_ID, localName);
	}
}
