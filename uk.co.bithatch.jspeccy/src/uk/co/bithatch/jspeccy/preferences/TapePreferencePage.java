package uk.co.bithatch.jspeccy.preferences;

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

import uk.co.bithatch.jspeccy.Activator;

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
        flashload.setLayoutData(GridDataFactory.defaultsFor(loadTraps).span(3, 0).create());

        accelerated = new Button(container, SWT.CHECK);
        accelerated.setText("Accelerated Loading");
        accelerated.setLayoutData(GridDataFactory.defaultsFor(loadTraps).span(3, 0).create());

        autoload = new Button(container, SWT.CHECK);
        autoload.setText("Autoload on tape insertion");
        autoload.setLayoutData(GridDataFactory.defaultsFor(loadTraps).span(3, 0).create());

        invertedEAR = new Button(container, SWT.CHECK);
        invertedEAR.setText("Inverted EAR state for TZX states");
        invertedEAR.setLayoutData(GridDataFactory.defaultsFor(loadTraps).span(3, 0).create());

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
        
        var tapesettings = Activator.getDefault().settings().jspeccy().getTapeSettings();
        
		loadTraps.setSelection(tapesettings.isEnableLoadTraps());        
		flashload.setSelection(tapesettings.isFlashLoad());
		accelerated.setSelection(tapesettings.isAccelerateLoading());
        tzxBlock.select(tapesettings.isHighSamplingFreq() ? 1 : 0);
		autoload.setSelection(tapesettings.isAutoLoadTape());
		invertedEAR.setSelection(tapesettings.isInvertedEar());
		saveTraps.setSelection(tapesettings.isEnableSaveTraps());
        
        return container;
    }

    @Override
    public boolean performOk() {
        var settings = Activator.getDefault().settings();
        var tapeSettings = settings.jspeccy().getTapeSettings();
        tapeSettings.setEnableLoadTraps(loadTraps.getSelection());
        tapeSettings.setEnableSaveTraps(saveTraps.getSelection());
        tapeSettings.setFlashLoad(flashload.getSelection());
        tapeSettings.setAccelerateLoading(accelerated.getSelection());
        tapeSettings.setAutoLoadTape(autoload.getSelection());
        tapeSettings.setInvertedEar(invertedEAR.getSelection());
        tapeSettings.setHighSamplingFreq(tzxBlock.getSelectionIndex() == 1);
        settings.save();

        return true;
    }

    @Override
    protected void performDefaults() {
    	loadTraps.setSelection(true);
    	saveTraps.setSelection(true);
    	flashload.setSelection(false);
    	accelerated.setSelection(true);
    	autoload.setSelection(true);
    	tzxBlock.select(0);
    	invertedEAR.setSelection(false);
        super.performDefaults();
    }
    
}
