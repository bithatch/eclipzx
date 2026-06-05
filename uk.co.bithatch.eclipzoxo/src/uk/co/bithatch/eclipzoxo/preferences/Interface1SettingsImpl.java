package uk.co.bithatch.eclipzoxo.preferences;

import uk.co.bithatch.zoxo.interface1.Interface1Settings;

public class Interface1SettingsImpl implements Interface1Settings {

	public static final String CARTRIDGE_SIZE = "zoxo.interface1.cartridgeSize";
	public static final String MICRODRIVE_UNITS = "zoxo.interface1.microdriveUnits";
	public static final String V2_ROM = "zoxo.interface1.v2ROM";

	@Override
	public int getCartridgeSize() {
		return ZoxoPreferencesAccess.get().getPreferences().getInt(CARTRIDGE_SIZE, Interface1Settings.Defaults.DEFAULT.getCartridgeSize());
	}

	@Override
	public byte getMicrodriveUnits() {
		return (byte)ZoxoPreferencesAccess.get().getPreferences().getInt(CARTRIDGE_SIZE, Interface1Settings.Defaults.DEFAULT.getMicrodriveUnits());
	}

	@Override
	public boolean isV2ROM() {
		return ZoxoPreferencesAccess.get().getPreferences().getBoolean(CARTRIDGE_SIZE, Interface1Settings.Defaults.DEFAULT.isV2ROM());
	}

}
