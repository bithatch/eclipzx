package uk.co.bithatch.eclipzoxo.preferences;

import uk.co.bithatch.zoxo.ay8912.AY8912Settings;

public class AY8912SettingsImpl implements AY8912Settings {
	public static final String CHANNELS = "zoxo.ay8912.channels";
	public static final String FREQUENCY = "zoxo.ay8912.frequency";
	public static final String MODE = "zoxo.ay8912.mode";

	@Override
	public int getChannels() {
		return ZoxoPreferencesAccess.get().getPreferences().getInt(CHANNELS, AY8912Settings.Defaults.DEFAULT.getChannels());
	}

	@Override
	public int getFrequency() {
		return ZoxoPreferencesAccess.get().getPreferences().getInt(FREQUENCY, AY8912Settings.Defaults.DEFAULT.getFrequency());
	}

	@Override
	public SoundMode getSoundMode() {
		try {
			return SoundMode.valueOf(ZoxoPreferencesAccess.get().getPreferences().get(MODE, AY8912Settings.Defaults.DEFAULT.getSoundMode().name()));
		}
		catch(IllegalArgumentException iae) {
			return SoundMode.STEREO_ABC;
		}
	}

}
