package uk.co.bithatch.jspeccy.preferences;

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

import uk.co.bithatch.jspeccy.Activator;

public class Interface1PreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private Button iface1Connected;
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

        iface1Connected = new Button(container, SWT.CHECK);
        iface1Connected.setText("Connected");
        iface1Connected.setToolTipText("Interface I works on 16k, 48k, 127k and Plus2 models");
        iface1Connected.setLayoutData(GridDataFactory.defaultsFor(iface1Connected).span(3, 0).create());
        iface1Connected.addSelectionListener(SelectionListener.widgetSelectedAdapter(e->setAvailable()));

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
        
        var jspeccySettings = Activator.getDefault().settings().jspeccy();

        iface1Connected.setSelection(jspeccySettings.getInterface1Settings().isConnectedIF1());
        microdriveUnits.setSelection(jspeccySettings.getInterface1Settings().getMicrodriveUnits());
        cartridgeSize.setSelection(jspeccySettings.getInterface1Settings().getCartridgeSize());

        setAvailable();
        
        return container;
    }

    @Override
    public boolean performOk() {
        var settings = Activator.getDefault().settings();
        var iface1Settings = settings.jspeccy().getInterface1Settings();
		iface1Settings.setConnectedIF1(iface1Connected.getSelection());
        iface1Settings.setCartridgeSize(cartridgeSize.getSelection());
        iface1Settings.setMicrodriveUnits((byte)microdriveUnits.getSelection());
        settings.save();
        return true;
    }

    @Override
    protected void performDefaults() {
    	iface1Connected.setSelection(false);
    	cartridgeSize.setSelection(180);
    	microdriveUnits.setSelection(1);
        super.performDefaults();
    }
    
    private void setAvailable() {
    	microdriveUnits.setEnabled(iface1Connected.getSelection());
    	cartridgeSize.setEnabled(iface1Connected.getSelection());
    }
}
