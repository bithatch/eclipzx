package uk.co.bithatch.eclipzoxo.preferences;

import uk.co.bithatch.zoxo.audio.AudioSettings;
import uk.co.bithatch.zoxo.ay8912.AY8912Settings;

public class AudioSettingsImpl implements AudioSettings {
	public static final String CHANNELS = "zoxo.audio.channels";
	public static final String FREQUENCY = "zoxo.audio.frequency";

	@Override
	public int getChannels() {
		return ZoxoPreferencesAccess.get().getPreferences().getInt(CHANNELS,
				AY8912Settings.Defaults.DEFAULT.getChannels());
	}

	@Override
	public int getFrequency() {
		return ZoxoPreferencesAccess.get().getPreferences().getInt(FREQUENCY,
				AY8912Settings.Defaults.DEFAULT.getFrequency());
	}

}
