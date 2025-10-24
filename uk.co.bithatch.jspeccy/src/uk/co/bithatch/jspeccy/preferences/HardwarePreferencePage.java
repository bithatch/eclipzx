package uk.co.bithatch.jspeccy.preferences;

import java.util.Arrays;

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

import machine.MachineTypes;
import uk.co.bithatch.jspeccy.Activator;

public class HardwarePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Combo hardware;
    private Button ulaPlus;
    private Spinner cpuSpeed;
	private Button border;
	private Button extendBorder;
	private Button disableExtendBorderWhileLoading;

    public HardwarePreferencePage() {
        super("Hardware");
        setDescription("Hardware Options.");
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
		
		var info = new Label(container, SWT.NONE);
        info.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        info.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
        info.setText("System");

        var hardwareLabel = new Label(container, SWT.NONE);
        hardwareLabel.setText("Default Model:");
        hardware = new Combo(container, SWT.READ_ONLY);
        hardware.setItems(Arrays.asList(MachineTypes.values()).stream().map(MachineTypes::getLongModelName).toList().toArray(new String[0]));
        hardware.setLayoutData(GridDataFactory.defaultsFor(hardware).span(2, 0).create());

        var cpuSpeedLabel = new Label(container, SWT.NONE);
        cpuSpeedLabel.setText("Fast CPU Speed:");
        cpuSpeed = new Spinner(container, SWT.NONE);
        cpuSpeed.setValues(0, 2, 10, 0, 1, 1);
        cpuSpeed.setLayoutData(GridDataFactory.defaultsFor(cpuSpeed).create());
        var mhzLabel = new Label(container, SWT.NONE);
        mhzLabel.setText("x3.5 Mhz");
        
		info = new Label(container, SWT.NONE);
        info.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        info.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
        info.setText("Display");

        ulaPlus = new Button(container, SWT.CHECK);
        ulaPlus.setText("ULAPlus Support (64 color palette)");
        ulaPlus.setLayoutData(GridDataFactory.defaultsFor(ulaPlus).span(3, 0).create());

        border = new Button(container, SWT.CHECK);
        border.setText("Enable Border");
        border.setLayoutData(GridDataFactory.defaultsFor(ulaPlus).span(3, 0).create());

        extendBorder = new Button(container, SWT.CHECK);
        extendBorder.setText("Extend border beyond screen area");
        extendBorder.setLayoutData(GridDataFactory.defaultsFor(ulaPlus).span(3, 0).create());
        extendBorder.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
        	disableExtendBorderWhileLoading.setEnabled(extendBorder.getSelection());
        }));

        disableExtendBorderWhileLoading = new Button(container, SWT.CHECK);
        disableExtendBorderWhileLoading.setText("Except when loading");
        disableExtendBorderWhileLoading.setLayoutData(GridDataFactory.defaultsFor(ulaPlus).span(3, 0).indent(24, 0).create());
        
        var zxsettings = Activator.getDefault().settings();
		var jspeccySettings = zxsettings.jspeccy();

        hardware.select(jspeccySettings.getSpectrumSettings().getDefaultModel());
        ulaPlus.setSelection(jspeccySettings.getSpectrumSettings().isULAplus());
        cpuSpeed.setSelection(jspeccySettings.getSpectrumSettings().getFramesInt());
        border.setSelection(zxsettings.isBorderEnabled());
        extendBorder.setSelection(zxsettings.isExtendBorderEnabled());
        disableExtendBorderWhileLoading.setSelection(zxsettings.isExtendBorderDisabledDuringLoad());
    	disableExtendBorderWhileLoading.setEnabled(extendBorder.getSelection());

        return container;
    }

    @Override
    public boolean performOk() {
        var zxsettings = Activator.getDefault().settings();
		var jspeccySettings = zxsettings.jspeccy();
        var spectrumsettings = jspeccySettings.getSpectrumSettings();
        
        zxsettings.setExtendBorderDisabledDuringLoad(disableExtendBorderWhileLoading.getSelection()); 
        zxsettings.setBorderEnabled(border.getSelection());
        zxsettings.setExtendBorderEnabled(extendBorder.getSelection());
		spectrumsettings.setULAplus(ulaPlus.getSelection());
        spectrumsettings.setDefaultModel(hardware.getSelectionIndex());
        spectrumsettings.setFramesInt(cpuSpeed.getSelection());
        
        zxsettings.save();

        return true;
    }

    @Override
    protected void performDefaults() {
    	ulaPlus.setSelection(false);
    	hardware.select(MachineTypes.SPECTRUM48K.ordinal());
    	border.setSelection(true);
    	extendBorder.setSelection(true);
    	disableExtendBorderWhileLoading.setSelection(true);
        super.performDefaults();
    }
}
