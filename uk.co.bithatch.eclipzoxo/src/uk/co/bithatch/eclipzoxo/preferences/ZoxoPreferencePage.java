package uk.co.bithatch.eclipzoxo.preferences;

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.zoxo.system.Machine.MachineFactory;

public class ZoxoPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Combo hardware;
    private Spinner fastSpeed;
    private Spinner refreshRate;
	private Button border;
	private Button extendBorder;
	private Button disableExtendBorderWhileLoading;
	private List<MachineFactory<?, ?>> machines = ZoxoPreferencesAccess.get().machines();
	private Button confirmActions;

    public ZoxoPreferencePage() {
        super("Zoxo Emulator");
        setDescription("Options for the Zoxo emulator.");
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

        
		infoRow(container, "System");

        var hardwareLabel = new Label(container, SWT.NONE);
        hardwareLabel.setText("Default Model:");
        hardware = new Combo(container, SWT.READ_ONLY);
        hardware.setItems(machines.stream().map(MachineFactory::name).toList().toArray(new String[0]));
        hardware.setLayoutData(GridDataFactory.defaultsFor(hardware).span(2, 0).create());
		
		var refreshRateLabel = new Label(container, SWT.NONE);
		refreshRateLabel.setText("Frame Rate");
		
		refreshRate = new Spinner(container, SWT.NONE);
		refreshRate.setValues(0, 1, 100, 0, 1, 1);
		refreshRate.setLayoutData(GridDataFactory.defaultsFor(refreshRate).create());
		refreshRate.setLayoutData(GridDataFactory.defaultsFor(hardware).span(2, 0).create());

        var cpuSpeedLabel = new Label(container, SWT.NONE);
        cpuSpeedLabel.setText("Fast CPU Speed:");
        fastSpeed = new Spinner(container, SWT.NONE);
        fastSpeed.setValues(0, 2, 10, 0, 1, 1);
        fastSpeed.setLayoutData(GridDataFactory.defaultsFor(fastSpeed).create());
        var mhzLabel = new Label(container, SWT.NONE);
        mhzLabel.setText("x3.5 Mhz");
        
		infoRow(container, "Display");

        border = new Button(container, SWT.CHECK);
        border.setText("Enable Border");
        border.setLayoutData(GridDataFactory.defaultsFor(border).span(3, 0).create());

        extendBorder = new Button(container, SWT.CHECK);
        extendBorder.setText("Extend border beyond screen area");
        extendBorder.setLayoutData(GridDataFactory.defaultsFor(extendBorder).span(3, 0).create());
        extendBorder.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
        	disableExtendBorderWhileLoading.setEnabled(extendBorder.getSelection());
        }));

        disableExtendBorderWhileLoading = new Button(container, SWT.CHECK);
        disableExtendBorderWhileLoading.setText("Except when loading");
        disableExtendBorderWhileLoading.setLayoutData(GridDataFactory.defaultsFor(disableExtendBorderWhileLoading).span(3, 0).indent(24, 0).create());
        
		infoRow(container, "Other");

        confirmActions = new Button(container, SWT.CHECK);
        confirmActions.setText("Confirm destructive actions");
        confirmActions.setLayoutData(GridDataFactory.defaultsFor(confirmActions).span(3, 0).create());
        
        var pax = ZoxoPreferencesAccess.get();

        hardware.select(machines.indexOf(pax.getDefaultModel()));
        fastSpeed.setSelection(pax.getFastSpeed());
        border.setSelection(pax.isBorderEnabled());
        extendBorder.setSelection(pax.isExtendBorderEnabled());
        confirmActions.setSelection(pax.isConfirmDestructiveActions());
        disableExtendBorderWhileLoading.setSelection(pax.isExtendBorderDisabledDuringLoad());
    	disableExtendBorderWhileLoading.setEnabled(extendBorder.getSelection());
    	refreshRate.setSelection(pax.getRefreshRate());

        return container;
    }

	private void infoRow(Composite container, String text) {
		var info = new Label(container, SWT.NONE);
        info.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        info.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
        info.setText(text);
	}

    @Override
    public boolean performOk() {

        var pax = ZoxoPreferencesAccess.get();
        
        pax.setExtendBorderDisabledDuringLoad(disableExtendBorderWhileLoading.getSelection()); 
        pax.setBorderEnabled(border.getSelection());
        pax.setExtendBorderEnabled(extendBorder.getSelection());
        pax.setDefaultMachine(machines.get(hardware.getSelectionIndex()));
        pax.setFastSpeed(fastSpeed.getSelection()); 
        pax.setConfirmDestructiveActions(confirmActions.getSelection());
        pax.setRefreshRate(refreshRate.getSelection());

        return true;
    }

    @Override
    protected void performDefaults() {
    	hardware.select(machines.indexOf(ZoxoPreferencesAccess.get().getFallbackMachine()));
    	border.setSelection(true);
    	extendBorder.setSelection(true);
    	disableExtendBorderWhileLoading.setSelection(true);
    	confirmActions.setSelection(true);
    	refreshRate.setSelection(50);
        super.performDefaults();
    }
}
