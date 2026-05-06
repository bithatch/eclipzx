package uk.co.bithatch.tnfs.eclipse;

import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import uk.co.bithatch.bitzx.DefaultAbstractPreferenceInitializer;
import uk.co.bithatch.tnfs.lib.TNFS;

public class TNFSPreferenceInitializer extends DefaultAbstractPreferenceInitializer {

	public TNFSPreferenceInitializer() {
		super(TNFSPreferencesAccess.get());
	}

	@Override
	protected void onInit(IEclipsePreferences prefs, Set<String> keys) {
		setIfNotSet(prefs, TNFSPreferenceConstants.ENABLE, "true", keys);
		setIfNotSet(prefs, TNFSPreferenceConstants.ADDRESS, "0.0.0.0", keys);
		setIfNotSet(prefs, TNFSPreferenceConstants.PORT, String.valueOf(TNFS.DEFAULT_PORT), keys);
		setIfNotSet(prefs, TNFSPreferenceConstants.UDP, "true", keys);
		setIfNotSet(prefs, TNFSPreferenceConstants.TCP, "true", keys);

	}
}

