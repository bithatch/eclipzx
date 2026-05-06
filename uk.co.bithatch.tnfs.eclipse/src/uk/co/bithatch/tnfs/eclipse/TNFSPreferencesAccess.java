package uk.co.bithatch.tnfs.eclipse;

import uk.co.bithatch.bitzx.AbstractPreferencesAccess;
import uk.co.bithatch.tnfs.lib.TNFS;

public class TNFSPreferencesAccess extends AbstractPreferencesAccess {
	public final static class Defaults {
		private final static TNFSPreferencesAccess DEFAULT = new TNFSPreferencesAccess();
	}

	public static TNFSPreferencesAccess get() {
		return Defaults.DEFAULT;
	}

	protected TNFSPreferencesAccess() {
		super(TNFSActivator.PLUGIN_ID);
	}
	
	public int getPort() {
		return getPreferences().getInt(TNFSPreferenceConstants.PORT, TNFS.DEFAULT_PORT);
	}
	
	public String getAddress() {
		return getPreferences().get(TNFSPreferenceConstants.ADDRESS, "0.0.0.0");
	}
	
	public boolean isUDP() {
		return getPreferences().getBoolean(TNFSPreferenceConstants.UDP, true);
	}
	
	public boolean isTCP() {
		return getPreferences().getBoolean(TNFSPreferenceConstants.TCP, true);
	}
	
	public boolean isEnabled() {
		return getPreferences().getBoolean(TNFSPreferenceConstants.ENABLE, true);
	}


}
