package uk.co.bithatch.jspeccy.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.jspeccy.Activator;

public class EmulatorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private Button confirmActionsButton;
    private Button autosaveConfigButton;

    public EmulatorPreferencePage() {
        super("Emulator");
        setDescription("General emulator behavior options.");
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        var layout = new GridLayout(1, false);
        layout.verticalSpacing = 12;
        layout.marginTop = 24;
        container.setLayout(layout);

        confirmActionsButton = new Button(container, SWT.CHECK);
        confirmActionsButton.setText("Confirm Actions");
        confirmActionsButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        autosaveConfigButton = new Button(container, SWT.CHECK);
        autosaveConfigButton.setText("Autosave config on exit");
        autosaveConfigButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        
        var emsettings = Activator.getDefault().settings().jspeccy().getEmulatorSettings();
        
		confirmActionsButton.setSelection(emsettings.isAutosaveConfigOnExit());
        autosaveConfigButton.setSelection(emsettings.isConfirmActions());

        return container;
    }

    @Override
    public boolean performOk() {
        var settings = Activator.getDefault().settings();
        var emsettings = settings.jspeccy().getEmulatorSettings();
		emsettings.setAutosaveConfigOnExit(autosaveConfigButton.getSelection());
        emsettings.setConfirmActions(confirmActionsButton.getSelection());
        settings.save();
        return true;
    }

    @Override
    protected void performDefaults() {
        confirmActionsButton.setSelection(true);
        autosaveConfigButton.setSelection(false);
        super.performDefaults();
    }
}
