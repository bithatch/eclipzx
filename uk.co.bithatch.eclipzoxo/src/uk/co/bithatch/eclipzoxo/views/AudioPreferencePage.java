package uk.co.bithatch.eclipzoxo.views;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.eclipzoxo.preferences.AudioSettingsImpl;
import uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferencesAccess;
import uk.co.bithatch.zoxo.audio.AudioSettings;

public class AudioPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Spinner frequency;
	private Spinner channels;

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

        var jspeccySettings = ZoxoPreferencesAccess.get().settings(AudioSettings.class);

        frequency.setSelection(jspeccySettings.getFrequency());
        channels.setSelection(jspeccySettings.getChannels());

        return container;
    }

    @Override
    public boolean performOk() {
        var pax = ZoxoPreferencesAccess.get();
        var ps = pax.getPreferenceStore();
		ps.putValue(AudioSettingsImpl.CHANNELS, String.valueOf(channels.getSelection()));
        ps.putValue(AudioSettingsImpl.FREQUENCY, String.valueOf(frequency.getSelection()));
        return true;
    }

    @Override
    protected void performDefaults() {
    	var asettings = ZoxoPreferencesAccess.get().settings(AudioSettings.class);
		frequency.setSelection(asettings.getFrequency());
		channels.setSelection(asettings.getChannels());
        super.performDefaults();
    }
}
