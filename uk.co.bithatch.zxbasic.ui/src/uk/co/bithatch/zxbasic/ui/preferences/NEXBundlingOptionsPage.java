package uk.co.bithatch.zxbasic.ui.preferences;

import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_ADDRESS;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BANK;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_BAR_1;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_BAR_2;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_BORDER;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_DELAY_1;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_DELAY_2;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_DO_NOT_SAVE_PALETTE;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_USE_8_BIT_PALETTE;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BUNDLE;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BUNDLE_TYPE;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.getProperty;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.setProperty;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.dialogs.PropertyPage;

import uk.co.bithatch.zxbasic.ui.api.BundleType;
import uk.co.bithatch.zxbasic.ui.builder.ResourceProperties;


public class NEXBundlingOptionsPage extends PropertyPage  {

	private Button includeInNEX;
	private List<Button> typeButtons = new  ArrayList<>();
	private Button setBank;
	private Button setAddress;
	private Spinner bank;
	private Spinner address;
	private Button doNotSavePalette;
	private Button use8BitPalette;
	private Label borderLabel;
	private Spinner border;
	private Label bar1Label;
	private Spinner bar1;
	private Label bar2Label;
	private Spinner bar2;
	private Label delay1Label;
	private Spinner delay1;
	private Label delay2Label;
	private Spinner delay2;

	@Override
	protected Control createContents(Composite parent) {
		var composite = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = layout.verticalSpacing = 8;
		composite.setLayout(layout);

		includeInNEX = new Button(composite, SWT.CHECK);
		includeInNEX.setText("Include in NEX.");
		includeInNEX.setLayoutData(swtDefaults().
				align(SWT.FILL, SWT.CENTER).
				grab(true, false).
				span(4, 1).
				create());
		includeInNEX.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
		
		var file = getElement().getAdapter(IResource.class);

		var i = 0;
		for(var type : BundleType.values()) {
			if(type == BundleType.BY_FILE_TYPE && !BundleType.isSupportedFileType(file.getFileExtension())) {
				continue;
			}
			
			var typeButton = new Button(composite, SWT.RADIO);
			if(type == BundleType.BY_FILE_TYPE) {
				typeButton.setText(type.description() + " (." + file.getFileExtension() + ")");
			}
			else {
				typeButton.setText(type.description());
			}
			typeButton.setData(type);
			typeButtons.add(typeButton);			
			typeButton.setLayoutData(swtDefaults().
					align(SWT.FILL, SWT.CENTER).
					grab(true, false).
					indent(16, i == 0 ? 16 : 0).
					span(4, 1).
					create());
			typeButton.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
			i++;
			
			if(i == 1 && file.getFileExtension().equalsIgnoreCase("bmp")) {
				doNotSavePalette = new Button(composite, SWT.CHECK);
				doNotSavePalette.setText("Do not save palette");
				doNotSavePalette.setLayoutData(swtDefaults().
						align(SWT.FILL, SWT.CENTER).
						grab(true, false).
						span(4, 1).
						indent(64, 8).
						create());
				doNotSavePalette.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
				
				use8BitPalette = new Button(composite, SWT.CHECK);
				use8BitPalette.setText("Use 8-bit palette");
				use8BitPalette.setLayoutData(swtDefaults().
						align(SWT.FILL, SWT.CENTER).
						grab(true, false).
						indent(64, 0).
						span(4, 1).
						create());
				use8BitPalette.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
				
				borderLabel = new Label(composite, SWT.NONE);
				borderLabel.setText("Border:");
				borderLabel.setLayoutData(swtDefaults().
						indent(64, 0).
						create());
				
				border = new Spinner(composite, SWT.CHECK);
				border.setValues(0, 0, 255, 0, 4, 16);
				border.setLayoutData(swtDefaults().
						align(SWT.BEGINNING, SWT.CENTER).
						grab(true, false).
						span(3, 1).
						create());
				border.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
				
				bar1Label = new Label(composite, SWT.NONE);
				bar1Label.setText("Bar 1:");
				bar1Label.setLayoutData(swtDefaults().
						indent(64, 0).
						create());
				
				bar1 = new Spinner(composite, SWT.CHECK);
				bar1.setValues(0, 0, 255, 0, 4, 16);
				bar1.setLayoutData(swtDefaults().
						align(SWT.BEGINNING, SWT.CENTER).
						grab(true, false).
						span(3, 1).
						create());
				bar1.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
				
				bar2Label = new Label(composite, SWT.NONE);
				bar2Label.setText("Bar 2:");
				bar2Label.setLayoutData(swtDefaults().
						indent(64, 0).
						create());
				
				bar2 = new Spinner(composite, SWT.CHECK);
				bar2.setValues(0, 0, 255, 0, 4, 16);
				bar2.setLayoutData(swtDefaults().
						align(SWT.BEGINNING, SWT.CENTER).
						grab(true, false).
						span(3, 1).
						create());
				bar2.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
				
				delay1Label = new Label(composite, SWT.NONE);
				delay1Label.setText("Delay 1:");
				delay1Label.setLayoutData(swtDefaults().
						indent(64, 0).
						create());
				
				delay1 = new Spinner(composite, SWT.CHECK);
				delay1.setValues(0, 0, 255, 0, 4, 16);
				delay1.setLayoutData(swtDefaults().
						align(SWT.BEGINNING, SWT.CENTER).
						grab(true, false).
						span(3, 1).
						create());
				delay1.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
				
				delay2Label = new Label(composite, SWT.NONE);
				delay2Label.setText("Delay 2:");
				delay2Label.setLayoutData(swtDefaults().
						indent(64, 0).
						create());
				
				delay2 = new Spinner(composite, SWT.CHECK);
				delay2.setValues(0, 0, 255, 0, 4, 16);
				delay2.setLayoutData(swtDefaults().
						align(SWT.BEGINNING, SWT.CENTER).
						grab(true, false).
						span(3, 1).
						create());
				delay2.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
			}
		}
		
		setBank = new Button(composite, SWT.CHECK);
		setBank.setText("Bank:");
		setBank.setLayoutData(swtDefaults().
				align(SWT.BEGINNING, SWT.CENTER).
				indent(64, 0).
				span(2, 1).
				grab(true, false).
				create());
		setBank.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
		
		bank = new Spinner(composite, SWT.CHECK);
		bank.setValues(0, 0, 256, 0, 1, 16);
		bank.setLayoutData(swtDefaults().
				align(SWT.BEGINNING, SWT.CENTER).
				grab(true, false).
				span(2, 1).
				create());
		bank.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
		
		setAddress = new Button(composite, SWT.CHECK);
		setAddress.setText("Address");
		setAddress.setLayoutData(swtDefaults().
				indent(64, 0).
				align(SWT.BEGINNING, SWT.CENTER).
				grab(true, false).
				span(2, 1).
				create());
		setAddress.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
		
		address = new Spinner(composite, SWT.CHECK);
		address.setValues(0, 0, 0x4000, 0, 16, 256);
		address.setLayoutData(swtDefaults().
				align(SWT.BEGINNING, SWT.CENTER).
				grab(true, false).
				span(2, 1).
				create());
		address.addSelectionListener(widgetSelectedAdapter(evt -> updateState()));
		
		includeInNEX.setSelection(ResourceProperties.getProperty(file, ResourceProperties.NEX_BUNDLE, false));
		setSelectedType(BundleType.valueOf(ResourceProperties.getProperty(file, ResourceProperties.NEX_BUNDLE_TYPE, BundleType.defaultForFilename(file.getName()).name())));
		var bankVal = ResourceProperties.getProperty(file, ResourceProperties.NEX_BANK, -1);
		var addressVal = ResourceProperties.getProperty(file, ResourceProperties.NEX_ADDRESS, -1);
		setBank.setSelection(bankVal > -1);
		setAddress.setSelection(addressVal > -1);
		bank.setSelection(Math.max(bankVal, 0));
		address.setSelection(Math.max(addressVal, 0));

		if(doNotSavePalette != null) {
			doNotSavePalette.setSelection(getProperty(file, NEX_BMP_DO_NOT_SAVE_PALETTE, false));
			use8BitPalette.setSelection(getProperty(file, NEX_BMP_USE_8_BIT_PALETTE, true));
			border.setSelection(getProperty(file, NEX_BMP_BORDER, 0));
			bar1.setSelection(getProperty(file, NEX_BMP_BAR_1, 0));
			bar2.setSelection(getProperty(file, NEX_BMP_BAR_2, 0));
			delay1.setSelection(getProperty(file, NEX_BMP_DELAY_1, 0));
			delay2.setSelection(getProperty(file, NEX_BMP_DELAY_2, 0));
		}
		
		updateState();
		return composite;
	}


	@Override
	protected void performDefaults() {
		super.performDefaults();
		includeInNEX.setSelection(false);
		setSelectedType(BundleType.BY_FILE_TYPE);
		setBank.setSelection(false);
		setAddress.setSelection(false);
		bank.setSelection(0);
		address.setSelection(0);
	}

	@Override
	public boolean performOk() {
		var file = getElement().getAdapter(IResource.class);

		setProperty(file, NEX_BUNDLE, includeInNEX.getSelection());
		setProperty(file, NEX_BUNDLE_TYPE, getSelectedType().name());
		setProperty(file, NEX_BANK, setBank.getSelection() ? bank.getSelection() : -1);
		setProperty(file, NEX_ADDRESS, setAddress.getSelection() ? address.getSelection() : -1);
		

		if(doNotSavePalette != null) {
			setProperty(file, NEX_BMP_DO_NOT_SAVE_PALETTE, doNotSavePalette.getSelection());
			setProperty(file, NEX_BMP_USE_8_BIT_PALETTE, use8BitPalette.getSelection());
			setProperty(file, NEX_BMP_BORDER, border.getSelection());
			setProperty(file, NEX_BMP_BAR_1, bar1.getSelection());
			setProperty(file, NEX_BMP_BAR_2, bar2.getSelection());
			setProperty(file, NEX_BMP_DELAY_1, delay1.getSelection());
			setProperty(file, NEX_BMP_DELAY_2, delay2.getSelection());
		}
		return true;
	}

	private void setSelectedType(BundleType type) {
		for(var btn : typeButtons) {
			btn.setSelection(type == (BundleType)btn.getData());
		}
	}
	
	private BundleType getSelectedType() {
		for(var btn : typeButtons) {
			if(btn.getSelection())
				return (BundleType)btn.getData();
		}
		return BundleType.BY_FILE_TYPE;
	}

	private void updateState() {
		var sel = includeInNEX.getSelection();

		
		var type = getSelectedType();
		var bankAddress = sel && type.supportsBankAddress();
		var ftype = type == BundleType.BY_FILE_TYPE;
		
		if(doNotSavePalette != null) {
			for(var c : new Control[] {
					doNotSavePalette, use8BitPalette, border, borderLabel, bar1, bar1Label, bar2, bar2Label, delay1Label, delay1, delay2Label, delay2}) {
					c.setEnabled(sel && ftype);
			}
		}
		
		address.setEnabled(bankAddress && setAddress.getSelection() && setBank.getSelection());
		setAddress.setEnabled(bankAddress && setBank.getSelection());
		bank.setEnabled(bankAddress && setBank.getSelection());
		setBank.setEnabled(bankAddress);
		
		for(var btn : typeButtons) {
			btn.setEnabled(sel);
		}

	}
}
