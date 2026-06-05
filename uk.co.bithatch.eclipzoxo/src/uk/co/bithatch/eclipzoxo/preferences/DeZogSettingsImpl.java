package uk.co.bithatch.eclipzoxo.preferences;

import uk.co.bithatch.zoxo.dezog.DeZogSettings;

public class DeZogSettingsImpl implements DeZogSettings {
	public static final String PORT = "zoxo.dezog.port";

	@Override
	public int getPort() {
		return ZoxoPreferencesAccess.get().getPreferences().getInt(PORT, DeZogSettings.Defaults.DEFAULT.getPort());
	}

}
