package uk.co.bithatch.drawzx.editor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import uk.co.bithatch.drawzx.editor.EditorFileProperties.PaletteSource;

public class SpritePropertyPage extends PropertyPage {

	private Text palettePathText;
	private Button defaultPalette;
	private Button defaultTransPalette;
	private Button filePalette;
	private Label palettePathLabel;
	private Button browseButton;

	@Override
	protected Control createContents(Composite parent) {
		var composite = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(3, false);
		layout.verticalSpacing = 8;
		composite.setLayout(layout);

		var headingLabel = new Label(composite, SWT.NONE);
		headingLabel.setText("Associated Palette");
		headingLabel.setLayoutData(
				GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).create());

		defaultPalette = new Button(composite, SWT.RADIO);
		defaultPalette.setText("Default palette (no transparency)");
		defaultPalette.setLayoutData(
				GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).create());
		defaultPalette.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateState()));

		defaultTransPalette = new Button(composite, SWT.RADIO);
		defaultTransPalette.setText("Default palette (with transparency)");
		defaultTransPalette.setLayoutData(
				GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).create());
		defaultTransPalette.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateState()));

		filePalette = new Button(composite, SWT.RADIO);
		filePalette.setText("Custom palette file");
		filePalette.setLayoutData(
				GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).create());
		filePalette.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateState()));

		palettePathLabel = new Label(composite, SWT.NONE);
		palettePathLabel.setText("File:");

		palettePathText = new Text(composite, SWT.BORDER);
		palettePathText
				.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).create());

		var file = (IFile) getElement().getAdapter(IFile.class);
		var path = EditorFileProperties.getProperty(file, EditorFileProperties.PALETTE_PROPERTY, "");
		if (!path.equals("")) {
			palettePathText.setText(path);
		}

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, e -> {

			var dialog = new ElementTreeSelectionDialog(parent.getShell(), new WorkbenchLabelProvider(),
					new WorkbenchContentProvider());

			dialog.setTitle("Select Palette");
			dialog.setMessage("Select palette for editing this sprite:");
			dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
			dialog.setAllowMultiple(false);

			dialog.addFilter(new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (element instanceof IFile file) {
						String ext = file.getFileExtension();
						return ext == null || !(ext.equalsIgnoreCase("zx0"));
					} else if (element instanceof IContainer) {
						return true; // allow folders
					}
					return false;
				}
			});

			if (dialog.open() == Window.OK) {
				palettePathText.setText(((IFile) dialog.getFirstResult()).getLocation().toString());
			}
		});

		switch (EditorFileProperties.paletteSource(file)) {
		case DEFAULT:
			defaultPalette.setSelection(true);
			break;
		case FILE:
			filePalette.setSelection(true);
			break;
		default:
			defaultTransPalette.setSelection(true);
			break;
		}

		updateState();

		return composite;
	}

	@Override
	public boolean performOk() {
		var file = (IFile) getElement().getAdapter(IFile.class);
		EditorFileProperties.setProperty(file, EditorFileProperties.PALETTE_PROPERTY, palettePathText.getText().trim());
		EditorFileProperties.setProperty(file, EditorFileProperties.PALETTE_SOURCE_PROPERTY, getSource().name());
		return true;
	}

	private PaletteSource getSource() {
		if (filePalette.getSelection()) {
			return PaletteSource.FILE;
		} else if (defaultPalette.getSelection()) {
			return PaletteSource.DEFAULT;
		} else {
			return PaletteSource.DEFAULT_TRANSPARENT;
		}
	}

	private void updateState() {
		var sel = filePalette.getSelection();
		palettePathText.setEnabled(sel);
		browseButton.setEnabled(sel);
		palettePathLabel.setEnabled(sel);
	}
}
