package uk.co.bithatch.jspeccy.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.jspeccy.Activator;

public class MultifacePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Combo multifaceModel;
	private Button multiface;
    
    public MultifacePreferencePage() {
        super("Multiface");
        setDescription("Multiface Options.");
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

        multiface = new Button(container, SWT.CHECK);
        multiface.setText("Multiface One/128/+3");
        multiface.setLayoutData(GridDataFactory.defaultsFor(multiface).span(3, 0).create());
        multiface.addSelectionListener(SelectionListener.widgetSelectedAdapter(e->setAvailable()));

        var emulationLabel = new Label(container, SWT.NONE);
        emulationLabel.setText("Emulation on 16k/48k:");
        multifaceModel = new Combo(container, SWT.READ_ONLY);
        multifaceModel.setToolTipText("JSpeccy can emulate the Multiface One or Multiface 128 when emulating 16k or 48k Spectrum");
        multifaceModel.setItems("Multiface One", "Multiface 128");
        multifaceModel.setLayoutData(GridDataFactory.defaultsFor(multifaceModel).span(2, 0).hint(200, SWT.DEFAULT).create());
        
        var jssettings = Activator.getDefault().settings().jspeccy().getSpectrumSettings();
        
		multiface.setSelection(jssettings.isMultifaceEnabled());
        multifaceModel.select(jssettings.isMf128On48K() ? 1 : 0);
        
        setAvailable();

        return container;
    }

    @Override
    public boolean performOk() {
        var settings = Activator.getDefault().settings();
        var spectrumSettings = settings.jspeccy().getSpectrumSettings();
        spectrumSettings.setMultifaceEnabled(multiface.getSelection());
        spectrumSettings.setMf128On48K(multifaceModel.getSelectionIndex() == 1);
        settings.save();

        return true;
    }

    @Override
    protected void performDefaults() {
    	multiface.setSelection(false);
    	multifaceModel.select(0);
        super.performDefaults();
    }
    
    private void  setAvailable() {
    	multifaceModel.setEnabled(multiface.getSelection());
    	
    }
}
