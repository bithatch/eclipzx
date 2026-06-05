package uk.co.bithatch.eclipzoxo.preferences;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.zoxo.ay8912.AY8912Settings;
import uk.co.bithatch.zoxo.ay8912.AY8912Settings.SoundMode;

public class AY8912PreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Combo audioMode;
	private Spinner frequency;
	private Spinner channels;
	private final List<SoundMode> soundModes = Arrays.asList(SoundMode.values());

    public AY8912PreferencePage() {
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

        var frequencyLabel = new Label(container, SWT.NONE);
        frequencyLabel.setText("Frequency:");
        frequency = new Spinner(container, SWT.NONE);
        frequency.setValues(0, 1, 8, 0, 1, 1);
        frequency.setLayoutData(GridDataFactory.defaultsFor(frequency).span(2, 0).create());

        var channelsLabel = new Label(container, SWT.NONE);
        channelsLabel.setText("Channels:");
        channels = new Spinner(container, SWT.NONE);
        channels.setValues(0, 1, 8, 0, 1, 1);
        channels.setLayoutData(GridDataFactory.defaultsFor(channels).span(2, 0).create());

        var hardwareLabel = new Label(container, SWT.NONE);
        hardwareLabel.setText("Audio Mode:");
        audioMode = new Combo(container, SWT.READ_ONLY);
		audioMode.setItems(soundModes.stream().map(f -> f.name()).toList().toArray(new String[0]));
        audioMode.setLayoutData(GridDataFactory.defaultsFor(audioMode).span(2, 0).create());
        
        var jspeccySettings = ZoxoPreferencesAccess.get().settings(AY8912Settings.class);

        audioMode.select(soundModes.indexOf(jspeccySettings.getSoundMode()));
        frequency.setSelection(jspeccySettings.getFrequency());
        channels.setSelection(jspeccySettings.getChannels());

        return container;
    }

    @Override
    public boolean performOk() {
        var pax = ZoxoPreferencesAccess.get();
        pax.getPreferenceStore().putValue(AY8912SettingsImpl.CHANNELS, String.valueOf(channels.getSelection()));
        pax.getPreferenceStore().putValue(AY8912SettingsImpl.FREQUENCY, String.valueOf(frequency.getSelection()));
        return true;
    }

    @Override
    protected void performDefaults() {
    	audioMode.select(0);
    	var settings = ZoxoPreferencesAccess.get().settings(AY8912Settings.class);
		frequency.setSelection(settings.getFrequency());
    	channels.setSelection(settings.getChannels());
        super.performDefaults();
    }
}
