package uk.co.bithatch.eclipzoxo.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.zoxo.interface2.Interface2Settings;

public class Interface2PreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private Button unlockOnLoad;

    public Interface2PreferencePage() {
        super("Interface 2");
        setDescription("Interface 2 Options.");
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        var container = new Composite(parent, SWT.NONE);
        var layout = new GridLayout(3, false);
        layout.marginTop = 24;
        layout.verticalSpacing = 16;
		container.setLayout(layout);

		unlockOnLoad = new Button(container, SWT.CHECK);
		unlockOnLoad.setText("Unlock on load");
		unlockOnLoad.setToolTipText("Unlock on load");
		unlockOnLoad.setLayoutData(GridDataFactory.defaultsFor(unlockOnLoad).span(3, 0).create());

        var iface2Settings = ZoxoPreferencesAccess.get().settings(Interface2Settings.class);
        unlockOnLoad.setSelection(iface2Settings.isUnlockOnLoad());
        
        return container;
    }

    @Override
    public boolean performOk() {
        var pax = ZoxoPreferencesAccess.get();
        pax.getPreferenceStore().putValue(Interface2SettingsImpl.UNLOCK_ON_LOAD, String.valueOf(unlockOnLoad.getSelection()));
        return true;
    }

    @Override
    protected void performDefaults() {
    	unlockOnLoad.setSelection(Interface2Settings.Defaults.DEFAULT.isUnlockOnLoad());
        super.performDefaults();
    }
    
}
