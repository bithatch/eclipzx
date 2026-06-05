package uk.co.bithatch.eclipzoxo.preferences;

import uk.co.bithatch.zoxo.zxspectrum.ZXSpectrumSettings;

public class ZXSpectrumSettingsImpl implements ZXSpectrumSettings {

	@Override
	public int refreshRate() {
		return ZoxoPreferencesAccess.get().getRefreshRate();
	}

}
