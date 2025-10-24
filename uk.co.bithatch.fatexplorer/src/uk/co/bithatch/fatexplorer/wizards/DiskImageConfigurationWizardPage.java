package uk.co.bithatch.fatexplorer.wizards;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import de.waldheinz.fs.fat.FatType;

public class DiskImageConfigurationWizardPage extends WizardPage {

	private Spinner size;
	private Button mbr;
	private Text oemName;
	private Text volumeLabel;
	private Button fat12;
	private Button fat16;
	private Button fat32;
	private Button addToExplorer;

	public DiskImageConfigurationWizardPage() {
		super("Configuration");
		setTitle("Disk Image Configuration");
		setDescription("Configure the initial disk image.");
	}

	@Override
	public void createControl(Composite parent) {

		var container = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(1, false);
		layout.verticalSpacing = 8;
		container.setLayout(layout);


		/* Block Device */
		var blockDevice = new Group(container, SWT.TITLE);
		var blockDeviceLayout = new GridLayout(2, false);
		blockDeviceLayout.marginTop = blockDeviceLayout.marginBottom = 8;
		blockDevice.setText("Block Device");
		blockDevice.setLayoutData(
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).create());
		
		blockDeviceLayout.verticalSpacing = 12;
		blockDeviceLayout.horizontalSpacing = 12;
		blockDevice.setLayout(blockDeviceLayout);

		var label = new Label(blockDevice, SWT.NONE);
		label.setText("Size (MiB):");

		size = new Spinner(blockDevice, SWT.NONE);
		size.setValues(1024, 1, 2048, 0, 10, 100);
		size.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		addToExplorer = new Button(blockDevice, SWT.CHECK);
		addToExplorer.setText("Add To FAT Explorer");
		addToExplorer.setToolTipText("When selected, will be automatically added to the FAT Explorer view.");
		addToExplorer.setSelection(true);
		addToExplorer.setLayoutData(
				GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).span(2, 1).create());
		
		/* File System */
		var fileSystem = new Group(container, SWT.TITLE);
		var fileSystemLayout = new GridLayout(2, false);
		fileSystemLayout.marginTop = fileSystemLayout.marginBottom = 8;
		fileSystemLayout.verticalSpacing = 12;
		fileSystemLayout.horizontalSpacing = 12;
		fileSystem.setText("File System");
		fileSystem.setLayoutData(
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).create());
		
		fileSystem.setLayout(fileSystemLayout);

		label = new Label(fileSystem, SWT.NONE);
		label.setText("OEM Name:");

		oemName = new Text(fileSystem, SWT.NONE);
		oemName.setText("EclipZX");
		oemName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(fileSystem, SWT.NONE);
		label.setText("Volume Label:");

		volumeLabel = new Text(fileSystem, SWT.NONE);
		volumeLabel.setText("");
		volumeLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(fileSystem, SWT.NONE);
		label.setText("FAT Type:");
		fat12 = new Button(fileSystem, SWT.RADIO);
		fat12.setText("FAT12");
		fat12.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		label = new Label(fileSystem, SWT.NONE);
		fat16 = new Button(fileSystem, SWT.RADIO);
		fat16.setText("FAT16");
		fat16.setSelection(true);
		fat16.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		label = new Label(fileSystem, SWT.NONE);
		fat32 = new Button(fileSystem, SWT.RADIO);
		fat32.setText("FAT32");
		fat32.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		/* Other */
		var other = new Group(container, SWT.TITLE);
		var otherLayout = new GridLayout(2, false);
		otherLayout.marginTop = otherLayout.marginBottom = 8;
		otherLayout.verticalSpacing = 12;
		otherLayout.horizontalSpacing = 12;
		other.setLayout(otherLayout);
		other.setText("Other");
		other.setLayoutData(
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).create());

		mbr = new Button(other, SWT.CHECK);
		mbr.setText("Single Partion MBR");
		mbr.setToolTipText("When de-selected, the 'Super Floppy' format without an MBR will be used.");
		mbr.setSelection(true);
		mbr.setLayoutData(
				GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).span(2, 1).create());
		
		setControl(container);
		setMessage(null);
		setErrorMessage(null);

	}
	
	public String volumeLabel() {
		return volumeLabel.getText();
	}
	
	public String oemName() {
		return oemName.getText();
	}

	public boolean addToExplorer() {
		return addToExplorer.getSelection();
	}

	public boolean mbr() {
		return mbr.getSelection();
	}

	public int size() {
		return size.getSelection();
	}

	public FatType fatType() {
		if(fat12.getSelection()) {
			return FatType.FAT12;
		}
		else if(fat16.getSelection()) {
			return FatType.FAT16;
		}
		else if(fat32.getSelection()) {
			return FatType.FAT32;
		}
		else {
			throw new IllegalStateException();
		}
	}

}
