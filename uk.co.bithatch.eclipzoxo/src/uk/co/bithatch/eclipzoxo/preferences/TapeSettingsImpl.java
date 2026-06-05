package uk.co.bithatch.eclipzoxo.preferences;

import uk.co.bithatch.zoxo.system.TapeSettings;

public class TapeSettingsImpl implements TapeSettings {
	public static final String ACCELERATE_LOADING = "zoxo.tape.accelerateLoading";
	public static final String AUTO_LOAD = "zoxo.tape.autoLoad";
	public static final String LOAD_TRAPS = "zoxo.tape.loadTraps";
	public static final String SAVE_TRAPS = "zoxo.tape.saveTraps";
	public static final String FLASH_LOAD = "zoxo.tape.flashLoad";
	public static final String HIGH_SAMPLING_FREQUENCY = "zoxo.tape.highSamplingFrequency";
	public static final String INVERTED_EAR = "zoxo.tape.invertedEar";
	public static final String ISSUE2 = "zoxo.tape.issue2";
	public static final String LOADING_NOISE = "zoxo.tape.loadingNoise";

	@Override
	public boolean isAccelerateLoading() {
		return ZoxoPreferencesAccess.get().getPreferences().getBoolean(ACCELERATE_LOADING, TapeSettings.Defaults.DEFAULT.isAccelerateLoading());
	}

	@Override
	public boolean isAutoLoad() {
		return ZoxoPreferencesAccess.get().getPreferences().getBoolean(AUTO_LOAD, TapeSettings.Defaults.DEFAULT.isAutoLoad());
	}

	@Override
	public boolean isEnableLoadTraps() {
		return ZoxoPreferencesAccess.get().getPreferences().getBoolean(LOAD_TRAPS, TapeSettings.Defaults.DEFAULT.isEnableLoadTraps());
	}

	@Override
	public boolean isEnableSaveTraps() {
		return ZoxoPreferencesAccess.get().getPreferences().getBoolean(SAVE_TRAPS, TapeSettings.Defaults.DEFAULT.isEnableSaveTraps());
	}

	@Override
	public boolean isFlashLoad() {
		return ZoxoPreferencesAccess.get().getPreferences().getBoolean(FLASH_LOAD, TapeSettings.Defaults.DEFAULT.isFlashLoad());
	}

	@Override
	public boolean isHighSamplingFreq() {
		return ZoxoPreferencesAccess.get().getPreferences().getBoolean(HIGH_SAMPLING_FREQUENCY, TapeSettings.Defaults.DEFAULT.isHighSamplingFreq());
	}

	@Override
	public boolean isInvertedEar() {
		return ZoxoPreferencesAccess.get().getPreferences().getBoolean(INVERTED_EAR, TapeSettings.Defaults.DEFAULT.isInvertedEar());
	}

	@Override
	public boolean isIssue2() {
		return ZoxoPreferencesAccess.get().getPreferences().getBoolean(ISSUE2, TapeSettings.Defaults.DEFAULT.isIssue2());
	}

	@Override
	public boolean isLoadingNoise() {
		return ZoxoPreferencesAccess.get().getPreferences().getBoolean(LOADING_NOISE, TapeSettings.Defaults.DEFAULT.isLoadingNoise());
	}

}
