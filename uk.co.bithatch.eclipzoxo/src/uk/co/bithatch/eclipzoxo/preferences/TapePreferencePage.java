package uk.co.bithatch.eclipzoxo.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.zoxo.system.TapeSettings;

public class TapePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Combo tzxBlock;
	private Button loadTraps;
	private Button flashload;
	private Button accelerated;
	private Button autoload;
	private Button invertedEAR;
	private Button saveTraps;
    
    public TapePreferencePage() {
        super("Tape");
        setDescription("Tape Options.");
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
        info.setText("Tape Loading");

        loadTraps = new Button(container, SWT.CHECK);
        loadTraps.setText("Enable Load Traps");
        loadTraps.setLayoutData(GridDataFactory.defaultsFor(loadTraps).span(3, 0).create());

        flashload = new Button(container, SWT.CHECK);
        flashload.setText("Flashload Standard Speed Tape Blocks");
        flashload.setLayoutData(GridDataFactory.defaultsFor(flashload).span(3, 0).create());

        accelerated = new Button(container, SWT.CHECK);
        accelerated.setText("Accelerated Loading");
        accelerated.setLayoutData(GridDataFactory.defaultsFor(accelerated).span(3, 0).create());

        autoload = new Button(container, SWT.CHECK);
        autoload.setText("Autoload on tape insertion");
        autoload.setLayoutData(GridDataFactory.defaultsFor(autoload).span(3, 0).create());

        invertedEAR = new Button(container, SWT.CHECK);
        invertedEAR.setText("Inverted EAR state for TZX states");
        invertedEAR.setLayoutData(GridDataFactory.defaultsFor(invertedEAR).span(3, 0).create());

		info = new Label(container, SWT.NONE);
        info.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        info.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).indent(0, 24).create());
        info.setText("Tape Recoding");
        
        saveTraps = new Button(container, SWT.CHECK);
        saveTraps.setText("Enable Save Traps");
        saveTraps.setLayoutData(GridDataFactory.defaultsFor(loadTraps).span(3, 0).create());

        var tzxBlockLabel = new Label(container, SWT.NONE);
        tzxBlockLabel.setText("Record to TZX Block");
        tzxBlock = new Combo(container, SWT.READ_ONLY);
        tzxBlock.setItems("DRB (44.1 kHz)", "CSW Z-RLE (48 kHz)");
        tzxBlock.setLayoutData(GridDataFactory.defaultsFor(tzxBlock).span(2, 0).hint(200, SWT.DEFAULT).create());

        var tapesettings = ZoxoPreferencesAccess.get().settings(TapeSettings.class);
        
		loadTraps.setSelection(tapesettings.isEnableLoadTraps());        
		flashload.setSelection(tapesettings.isFlashLoad());
		accelerated.setSelection(tapesettings.isAccelerateLoading());
        tzxBlock.select(tapesettings.isHighSamplingFreq() ? 1 : 0);
		autoload.setSelection(tapesettings.isAutoLoad());
		invertedEAR.setSelection(tapesettings.isInvertedEar());
		saveTraps.setSelection(tapesettings.isEnableSaveTraps());
        
        return container;
    }

    @Override
    public boolean performOk() {
        var pax = ZoxoPreferencesAccess.get();
        pax.getPreferenceStore().putValue(TapeSettingsImpl.LOAD_TRAPS, String.valueOf(loadTraps.getSelection()));
        pax.getPreferenceStore().putValue(TapeSettingsImpl.SAVE_TRAPS, String.valueOf(saveTraps.getSelection()));
        pax.getPreferenceStore().putValue(TapeSettingsImpl.FLASH_LOAD, String.valueOf(flashload.getSelection()));
        pax.getPreferenceStore().putValue(TapeSettingsImpl.ACCELERATE_LOADING, String.valueOf(accelerated.getSelection()));
        pax.getPreferenceStore().putValue(TapeSettingsImpl.AUTO_LOAD, String.valueOf(autoload.getSelection()));
        pax.getPreferenceStore().putValue(TapeSettingsImpl.INVERTED_EAR, String.valueOf(invertedEAR.getSelection()));
        pax.getPreferenceStore().putValue(TapeSettingsImpl.HIGH_SAMPLING_FREQUENCY, String.valueOf(tzxBlock.getSelection()));
        return true;
    }

    @Override
    protected void performDefaults() {
    	loadTraps.setSelection(TapeSettings.Defaults.DEFAULT.isEnableLoadTraps());
    	saveTraps.setSelection(TapeSettings.Defaults.DEFAULT.isEnableSaveTraps());
    	flashload.setSelection(TapeSettings.Defaults.DEFAULT.isFlashLoad());
    	accelerated.setSelection(TapeSettings.Defaults.DEFAULT.isAccelerateLoading());
    	autoload.setSelection(TapeSettings.Defaults.DEFAULT.isAutoLoad());
    	tzxBlock.select(TapeSettings.Defaults.DEFAULT.isHighSamplingFreq() ? 1 : 0);
    	invertedEAR.setSelection(TapeSettings.Defaults.DEFAULT.isInvertedEar());
        super.performDefaults();
    }
    
}
