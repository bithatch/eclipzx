package uk.co.bithatch.zxbasic.ui.preferences;

import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BANK;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_CORE;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_OVERRIDE_PROJECT;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_PC;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_SP;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_SYSVARS;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_SYSVARS_LOCATION;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.getProperty;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.setProperty;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;


public class NEXBuildOptionsPage extends PropertyPage {

	private Spinner pc;
	private Spinner sp;
	private Spinner entryBank;
	private Button pcOverride;
	private Button spOverride;
	private Button entryBankOverride;
	private Text core;
	private Button includeSysVar;
	private Label alternativeLocationLabel;
	private Text alternativeLocation;
	private Button browseAlternativeLocation;
	private Button overrideNEXProjectSettings;
	private Label coreLabel;

	@Override
	protected Control createContents(Composite parent) {
		var composite = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(3, false);
		layout.verticalSpacing = 8;
		layout.horizontalSpacing = 8;
		composite.setLayout(layout);
		
		pcOverride = new Button(composite, SWT.CHECK);
		pcOverride.setText("SP:");
		pcOverride.addSelectionListener(widgetSelectedAdapter(evt -> {
			updateState();
		}));
		pc = new Spinner(composite, SWT.NONE);
		pc.setValues(0, 0, 65536, 0, 1, 256);
		pc.setToolTipText("Set the top address of the stack pointer.");
		pc.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).create());

		spOverride = new Button(composite, SWT.CHECK);
		spOverride.setText("PC:");
		spOverride.addSelectionListener(widgetSelectedAdapter(evt -> {
			updateState();
		}));
		sp = new Spinner(composite, SWT.NONE);
		sp.setToolTipText("Set the address of the program counter.");
		sp.setValues(0, 0, 65536, 0, 1, 256);
		sp.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).create());

		entryBankOverride = new Button(composite, SWT.CHECK);
		entryBankOverride.setText("Entry Bank:");
		entryBank = new Spinner(composite, SWT.NONE);
		entryBank.setValues(0, 0, 256, 0, 1, 16);
		entryBank.setToolTipText("Set the entry bank.");
		entryBank.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).create());
		entryBank.addSelectionListener(widgetSelectedAdapter(evt -> {
			updateState();
		}));

		overrideNEXProjectSettings = new Button(composite, SWT.CHECK);
		overrideNEXProjectSettings.setText("Override project settings");
		overrideNEXProjectSettings.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).indent(0, 24).create());
		overrideNEXProjectSettings.addSelectionListener(widgetSelectedAdapter(e-> updateState()));

		coreLabel = new Label(composite, SWT.CHECK);
		coreLabel.setText("Core:");
		coreLabel.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).indent(24, SWT.DEFAULT).create());
		
		core = new Text(composite, SWT.NONE);
		core.setToolTipText("The minimum required core version.");
		core.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).create());
		
		includeSysVar = new Button(composite, SWT.CHECK);
		includeSysVar.setText("Include sysvar.bin");
		includeSysVar.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).indent(24, SWT.DEFAULT).create());
		
		alternativeLocationLabel = new Label(composite, SWT.NONE);
		alternativeLocationLabel.setText("Alternative Location:");
		alternativeLocationLabel.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).indent(24, SWT.DEFAULT).create());
		
		alternativeLocation = new Text(composite, SWT.NONE);
		alternativeLocation .setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).create());

		browseAlternativeLocation = new Button(composite, SWT.NONE);
		browseAlternativeLocation.setText("File System");
		browseAlternativeLocation.addSelectionListener(widgetSelectedAdapter(e -> {
            var dialog = new FileDialog(getShell(), SWT.OPEN);
            dialog.setFilterExtensions(new String[] { "*.bin", "*.*"});
            dialog.setFilterNames(new String[] { "BIN files (*.bin)", "All file (*.*)"});
            var result = dialog.open();
            if (result != null) {
            	alternativeLocation.setText(result);
            }
		}));
		
		var file = (IFile) getElement().getAdapter(IFile.class);

		includeSysVar.addSelectionListener(widgetSelectedAdapter(evt -> {
			updateState();
		}));
		
		var pcVal = getProperty(file, NEX_PC, -1);
		pc.setSelection(Math.max(0, pcVal));
		pcOverride.setSelection(pcVal > -1);
		
		var spVal = getProperty(file, NEX_SP, -1);
		sp.setSelection(Math.max(0, spVal));
		spOverride.setSelection(spVal > -1);
		
		var entryBankVal = getProperty(file, NEX_BANK, -1);
		entryBank.setSelection(Math.max(0, entryBankVal));
		entryBankOverride.setSelection(entryBankVal > -1);

		core.setText(getProperty(file, NEX_CORE, ""));
		core.setMessage(ZXBasicPreferencesAccess.get().getNEXCore(file.getProject()));

		alternativeLocation.setText(getProperty(file, NEX_SYSVARS_LOCATION, ""));
		includeSysVar.setSelection(getProperty(file, NEX_SYSVARS, true));
		overrideNEXProjectSettings.setSelection(getProperty(file, NEX_OVERRIDE_PROJECT, false));
	
		updateState();
		return composite;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
//		pc.setSelection(0);
//		sp.setSelection(0);
//		entryBank.setSelection(0);
//		overrideNEXProjectSettings.setS
		updateState();
	}

	@Override
	public boolean performOk() {
		var file = getElement().getAdapter(IFile.class);
		setProperty(file, NEX_CORE, core.getText().trim());
		setProperty(file, NEX_PC, pcOverride.getSelection() ? -1 : pc.getSelection());
		setProperty(file, NEX_SP, spOverride.getSelection() ? -1 :sp.getSelection());
		setProperty(file, NEX_BANK, entryBankOverride.getSelection() ? -1 :entryBank.getSelection());
		setProperty(file, NEX_OVERRIDE_PROJECT, overrideNEXProjectSettings.getSelection());
		setProperty(file, NEX_SYSVARS, includeSysVar.getSelection());
		setProperty(file, NEX_SYSVARS_LOCATION, alternativeLocation.getText());
		return true;
	}
	
	private void updateState() {
		entryBank.setEnabled(entryBankOverride.getSelection());
		sp.setEnabled(spOverride.getSelection());
		pc.setEnabled(pcOverride.getSelection());
		
		var overrideProject = overrideNEXProjectSettings.getSelection();
		
		core.setEnabled(overrideProject);
		coreLabel.setEnabled(overrideProject);
		alternativeLocation.setEnabled(overrideProject && includeSysVar.getSelection());
		alternativeLocationLabel.setEnabled(overrideProject && includeSysVar.getSelection());
		browseAlternativeLocation.setEnabled(overrideProject && includeSysVar.getSelection());
		includeSysVar.setEnabled(overrideProject);
	}

}
