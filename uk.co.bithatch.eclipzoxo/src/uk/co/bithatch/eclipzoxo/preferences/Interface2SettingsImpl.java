package uk.co.bithatch.eclipzoxo.preferences;

import uk.co.bithatch.zoxo.interface2.Interface2Settings;

public class Interface2SettingsImpl implements Interface2Settings {

	public static final String UNLOCK_ON_LOAD = "zoxo.interface2.unlockOnLoad";

	@Override
	public boolean isUnlockOnLoad() {
		return ZoxoPreferencesAccess.get().getPreferences().getBoolean(UNLOCK_ON_LOAD, Interface2Settings.Defaults.DEFAULT.isUnlockOnLoad());
	}

}
