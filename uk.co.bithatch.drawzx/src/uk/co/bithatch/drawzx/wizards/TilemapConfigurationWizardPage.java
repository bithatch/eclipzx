package uk.co.bithatch.drawzx.wizards;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Wizard page for configuring a new tilemap: size, colour depth, and tile definitions.
 */
public class TilemapConfigurationWizardPage extends WizardPage {

	private Combo sizeCombo;
	private Combo colourCombo;
	private Button radioCreateTil;
	private Button radioAttachTil;
	private Button radioNoTil;
	private Text tilPathText;
	private Button browseButton;

	public TilemapConfigurationWizardPage() {
		super("TilemapConfiguration");
		setTitle("Tilemap Configuration");
		setDescription("Configure the tilemap size, tile colour depth, and tile definition file.");
	}

	@Override
	public void createControl(Composite parent) {
		var container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		// Tilemap size
		new Label(container, SWT.NONE).setText("Tilemap Size:");
		sizeCombo = new Combo(container, SWT.READ_ONLY);
		sizeCombo.setItems("40x32 (Standard)", "80x32 (Hi-Res)");
		sizeCombo.select(0);
		sizeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Colour depth
		new Label(container, SWT.NONE).setText("Tile Colours:");
		colourCombo = new Combo(container, SWT.READ_ONLY);
		colourCombo.setItems("16 Colour (4-bit)", "2 Colour (1-bit)");
		colourCombo.select(0);
		colourCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Tile definitions group
		var tilGroup = new Group(container, SWT.NONE);
		tilGroup.setText("Tile Definitions (.TIL)");
		tilGroup.setLayout(new GridLayout(3, false));
		var tilGd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		tilGroup.setLayoutData(tilGd);

		radioCreateTil = new Button(tilGroup, SWT.RADIO);
		radioCreateTil.setText("Create a new .TIL tile definitions file");
		radioCreateTil.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		radioCreateTil.setSelection(true);

		radioAttachTil = new Button(tilGroup, SWT.RADIO);
		radioAttachTil.setText("Attach an existing .TIL file:");
		radioAttachTil.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		tilPathText = new Text(tilGroup, SWT.BORDER);
		tilPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		tilPathText.setEnabled(false);

		browseButton = new Button(tilGroup, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.setEnabled(false);

		radioNoTil = new Button(tilGroup, SWT.RADIO);
		radioNoTil.setText("Do not attach a .TIL file");
		radioNoTil.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		// Listeners
		var radioListener = SelectionListener.widgetSelectedAdapter(e -> {
			boolean attachSelected = radioAttachTil.getSelection();
			tilPathText.setEnabled(attachSelected);
			browseButton.setEnabled(attachSelected);
			validatePage();
		});
		radioCreateTil.addSelectionListener(radioListener);
		radioAttachTil.addSelectionListener(radioListener);
		radioNoTil.addSelectionListener(radioListener);

		browseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var dlg = new FileDialog(getShell(), SWT.OPEN);
			dlg.setText("Select Tile Definitions File");
			dlg.setFilterExtensions(new String[] { "*.til", "*.*" });
			dlg.setFilterNames(new String[] { "Tile Definition Files (*.til)", "All Files (*.*)" });
			var result = dlg.open();
			if (result != null) {
				tilPathText.setText(result);
				validatePage();
			}
		}));

		tilPathText.addModifyListener(e -> validatePage());

		setControl(container);
		validatePage();
	}

	private void validatePage() {
		if (radioAttachTil.getSelection() && tilPathText.getText().trim().isEmpty()) {
			setErrorMessage("Please select an existing .TIL file or choose another option.");
			setPageComplete(false);
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}

	/**
	 * @return true if 40x32, false if 80x32
	 */
	public boolean isStandard40x32() {
		return sizeCombo.getSelectionIndex() == 0;
	}

	/**
	 * @return 4 for 16-colour, 1 for 2-colour
	 */
	public int getBpp() {
		return colourCombo.getSelectionIndex() == 0 ? 4 : 1;
	}

	/**
	 * @return true if user chose to create a new .TIL file
	 */
	public boolean isCreateNewTil() {
		return radioCreateTil.getSelection();
	}

	/**
	 * @return true if user chose to attach an existing .TIL file
	 */
	public boolean isAttachExistingTil() {
		return radioAttachTil.getSelection();
	}

	/**
	 * @return the path to the existing .TIL file (only valid if isAttachExistingTil)
	 */
	public String getTilPath() {
		return tilPathText.getText().trim();
	}
}
