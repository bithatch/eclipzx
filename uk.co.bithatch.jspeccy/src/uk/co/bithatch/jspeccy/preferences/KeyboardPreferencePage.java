package uk.co.bithatch.jspeccy.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.jspeccy.Activator;

public class KeyboardPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Combo joystickModel;
    private Button mapPcKeys;
    private Button recreatedZx;
	private Button issue2;
    
    public KeyboardPreferencePage() {
        super("Keyboard");
        setDescription("Keyboard Options.");
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        var container = new Composite(parent, SWT.NONE);
        var layout = new GridLayout(3, false);
        layout.verticalSpacing = 16;
        layout.marginTop = 24;
		container.setLayout(layout);

        issue2 = new Button(container, SWT.CHECK);
        issue2.setText("Use Issue 2 keyboard");
        issue2.setToolTipText("The keyboard of later 48k Spectrums behave slightly\n"
        		+ "differently to earlier ones. If you have an older game, where\n"
        		+ "the keyboard is responsive, try enabling Issue 2 keyboard emulation.");
        issue2.setLayoutData(GridDataFactory.defaultsFor(issue2).span(3, 0).create());

        mapPcKeys = new Button(container, SWT.CHECK);
        mapPcKeys.setText("PC Keyboard -> Spectrum");
        mapPcKeys.setLayoutData(GridDataFactory.defaultsFor(mapPcKeys).span(3, 0).create());

        recreatedZx = new Button(container, SWT.CHECK);
        recreatedZx.setText("Recreated ZX Keyboard (Game Mode)");
        recreatedZx.setLayoutData(GridDataFactory.defaultsFor(recreatedZx).span(3, 0).create());

        var joystickEmulationLabel = new Label(container, SWT.NONE);
        joystickEmulationLabel.setText("Joystick Emulation:");
        joystickModel = new Combo(container, SWT.READ_ONLY);
        joystickModel.setItems("None", "Kempston", "Sinclair 1", "Sinclair 2", "Cursor/AGF/Protek", "Fuller");
        joystickModel.setLayoutData(GridDataFactory.defaultsFor(joystickModel).span(2, 0).create());
        
        var jssettings = Activator.getDefault().settings().jspeccy().getKeyboardJoystickSettings();
        
		issue2.setSelection(jssettings.isIssue2());
        joystickModel.select(jssettings.getJoystickModel());
        mapPcKeys.setSelection(jssettings.isMapPCKeys());
        recreatedZx.setSelection(jssettings.isRecreatedZX());

        return container;
    }

    @Override
    public boolean performOk() {
        var settings = Activator.getDefault().settings();
        var keyboardSettings = settings.jspeccy().getKeyboardJoystickSettings();
		keyboardSettings.setIssue2(issue2.getSelection());
		keyboardSettings.setMapPCKeys(mapPcKeys.getSelection());
        keyboardSettings.setRecreatedZX(recreatedZx.getSelection());
        keyboardSettings.setJoystickModel(joystickModel.getSelectionIndex());
        
        settings.save();

        return true;
    }

    @Override
    protected void performDefaults() {
    	issue2.setSelection(false);
    	mapPcKeys.setSelection(false);
    	joystickModel.select(1);
    	recreatedZx.setSelection(false);
        super.performDefaults();
    }
}
