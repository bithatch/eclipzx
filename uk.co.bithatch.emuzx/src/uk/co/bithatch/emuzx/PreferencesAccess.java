package uk.co.bithatch.emuzx;

import uk.co.bithatch.bitzx.AbstractPreferencesAccess;

public class PreferencesAccess extends AbstractPreferencesAccess {

	public final static class Defaults {
		private final static PreferencesAccess DEFAULT = new PreferencesAccess();
	}

	protected PreferencesAccess() {
		super(Activator.PLUGIN_ID);
	}

	public static PreferencesAccess get() {
		return Defaults.DEFAULT;
	}

}
