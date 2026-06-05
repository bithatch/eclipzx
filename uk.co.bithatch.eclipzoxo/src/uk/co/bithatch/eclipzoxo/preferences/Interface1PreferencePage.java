package uk.co.bithatch.eclipzoxo.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.zoxo.interface1.Interface1Settings;

public class Interface1PreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

//    private Button iface1Connected;
    private Button v2ROM;
    private Spinner microdriveUnits;
	private Spinner cartridgeSize;

    public Interface1PreferencePage() {
        super("Interface 1");
        setDescription("Interface 1 Options.");
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

		v2ROM = new Button(container, SWT.CHECK);
		v2ROM.setText("Use V2 Rom");
		v2ROM.setToolTipText("Use the version 2 ROM");
		v2ROM.setLayoutData(GridDataFactory.defaultsFor(v2ROM).span(3, 0).create());
		v2ROM.addSelectionListener(SelectionListener.widgetSelectedAdapter(e->setAvailable()));

        var microdriveUnitsLabel = new Label(container, SWT.NONE);
        microdriveUnitsLabel.setText("Microdrive Units:");
        microdriveUnits = new Spinner(container, SWT.NONE);
        microdriveUnits.setValues(0, 1, 8, 0, 1, 1);
        microdriveUnits.setLayoutData(GridDataFactory.defaultsFor(microdriveUnits).span(2, 0).create());

        var sectorsLabel = new Label(container, SWT.NONE);
        sectorsLabel.setText("# Sectors (10-253):");
        cartridgeSize = new Spinner(container, SWT.NONE);
        cartridgeSize.setToolTipText("180 sectors is the standard cartridge. Maximum should be 200");
        cartridgeSize.setValues(0, 10, 253, 0, 1, 1);
        cartridgeSize.setLayoutData(GridDataFactory.defaultsFor(microdriveUnits).span(2, 0).create());

        var iface1Settings = ZoxoPreferencesAccess.get().settings(Interface1Settings.class);
//        var jspeccySettings = Activator.getDefault().settings().jspeccy();
//        var pax = ZoxoPreferencesAccess.get();

//        iface1Connected.setSelection(jspeccySettings.getInterface1Settings().isConnectedIF1());
//        iface1Connected.setSelection(pax.getPreferences().getBoolean(Interface1SettingsImpl., isControlCreated()));
//        microdriveUnits.setSelection(jspeccySettings.getInterface1Settings().getMicrodriveUnits());
//        cartridgeSize.setSelection(jspeccySettings.getInterface1Settings().getCartridgeSize());
        microdriveUnits.setSelection(iface1Settings.getMicrodriveUnits());
        cartridgeSize.setSelection(iface1Settings.getCartridgeSize());

        setAvailable();
        
        return container;
    }

    @Override
    public boolean performOk() {
//        var settings = Activator.getDefault().settings();
        var pax = ZoxoPreferencesAccess.get();
        pax.getPreferenceStore().putValue(Interface1SettingsImpl.CARTRIDGE_SIZE, String.valueOf(cartridgeSize.getSelection()));
        pax.getPreferenceStore().putValue(Interface1SettingsImpl.MICRODRIVE_UNITS, String.valueOf(microdriveUnits.getSelection()));
//		iface1Settings.setConnectedIF1(iface1Connected.getSelection());
//        iface1Settings.setCartridgeSize(cartridgeSize.getSelection());
//        iface1Settings.setMicrodriveUnits((byte)microdriveUnits.getSelection());
//        settings.save();
        return true;
    }

    @Override
    protected void performDefaults() {
//    	iface1Connected.setSelection(false);
    	cartridgeSize.setSelection(Interface1Settings.Defaults.DEFAULT.getCartridgeSize());
    	microdriveUnits.setSelection(Interface1Settings.Defaults.DEFAULT.getMicrodriveUnits());
    	v2ROM.setSelection(Interface1Settings.Defaults.DEFAULT.isV2ROM());
        super.performDefaults();
    }
    
    private void setAvailable() {
//    	microdriveUnits.setEnabled(iface1Connected.getSelection());
//    	cartridgeSize.setEnabled(iface1Connected.getSelection());
    }
}
