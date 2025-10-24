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

public class AudioPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private Button muted;
	private Button loadingNoise;
	private Button hifiSound;
	private Button ayFor48k;
	private Combo audioMode;

    public AudioPreferencePage() {
        super("Audio");
        setDescription("Audio Options.");
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        var container = new Composite(parent, SWT.NONE);
        var layout = new GridLayout(3, false);
        layout.verticalSpacing = 12;
        layout.marginTop = 24;
		container.setLayout(layout);

        muted = new Button(container, SWT.CHECK);
        muted.setText("Sound Muted");
        muted.setLayoutData(GridDataFactory.defaultsFor(muted).span(3, 0).create());

        loadingNoise = new Button(container, SWT.CHECK);
        loadingNoise.setText("Loading Noise");
        loadingNoise.setLayoutData(GridDataFactory.defaultsFor(muted).span(3, 0).create());

        hifiSound = new Button(container, SWT.CHECK);
        hifiSound.setText("High Quality Sound");
        hifiSound.setLayoutData(GridDataFactory.defaultsFor(muted).span(3, 0).create());

        ayFor48k = new Button(container, SWT.CHECK);
        ayFor48k.setText("AY-3-8912 for 48k");
        ayFor48k.setLayoutData(GridDataFactory.defaultsFor(muted).span(3, 0).create());

        var hardwareLabel = new Label(container, SWT.NONE);
        hardwareLabel.setText("Audio Mode:");
        audioMode = new Combo(container, SWT.READ_ONLY);
        audioMode.setItems("Mono", "Stereo ABC", "Stereo ACB", "Stereo BAC");
        audioMode.setLayoutData(GridDataFactory.defaultsFor(audioMode).span(2, 0).create());
        
        var jspeccySettings = Activator.getDefault().settings().jspeccy();

        audioMode.select(jspeccySettings.getAY8912Settings().getSoundMode());
        muted.setSelection(jspeccySettings.getSpectrumSettings().isMutedSound());
        loadingNoise.setSelection(jspeccySettings.getSpectrumSettings().isLoadingNoise());
        hifiSound.setSelection(jspeccySettings.getSpectrumSettings().isHifiSound());
        ayFor48k.setSelection(jspeccySettings.getSpectrumSettings().isAYEnabled48K());

        return container;
    }

    @Override
    public boolean performOk() {
        var settings = Activator.getDefault().settings();
		var jspeccySettings = settings.jspeccy();
        jspeccySettings.getAY8912Settings().setSoundMode(audioMode.getSelectionIndex());
        jspeccySettings.getSpectrumSettings().setMutedSound(muted.getSelection());
        jspeccySettings.getSpectrumSettings().setLoadingNoise(loadingNoise.getSelection());
        jspeccySettings.getSpectrumSettings().setHifiSound(hifiSound.getSelection());
        jspeccySettings.getSpectrumSettings().setAYEnabled48K(ayFor48k.getSelection());
        
        settings.save();

        return true;
    }

    @Override
    protected void performDefaults() {
    	audioMode.select(0);
    	muted.setSelection(false);
    	loadingNoise.setSelection(true);
    	hifiSound.setSelection(false);
    	ayFor48k.setSelection(false);
        super.performDefaults();
    }
}
