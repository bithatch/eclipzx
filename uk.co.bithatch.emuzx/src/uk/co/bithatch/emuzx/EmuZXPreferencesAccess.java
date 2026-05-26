package uk.co.bithatch.emuzx;

import org.eclipse.core.resources.IProject;

import uk.co.bithatch.bitzx.AbstractPreferencesAccess;

public class EmuZXPreferencesAccess extends AbstractPreferencesAccess {

	public final static class Defaults {
		private final static EmuZXPreferencesAccess DEFAULT = new EmuZXPreferencesAccess();
	}

	protected EmuZXPreferencesAccess() {
		super(Activator.PLUGIN_ID);
	}

	public static EmuZXPreferencesAccess get() {
		return Defaults.DEFAULT;
	}

	public String getNEXCore(IProject project) {
		return getPreferences(project).get(EmuZXPreferenceConstants.NEX_CORE, NexConverter.DEFAULT_CORE_STR);
	}

	public String getNEXSysvarLocation(IProject project) {
		return getPreferences(project).get(EmuZXPreferenceConstants.NEX_SYSVAR_LOCATION, "");
	}

	public boolean isNEXIncludeSysvar(IProject project) {
		return getPreferences(project).getBoolean(EmuZXPreferenceConstants.NEX_INCLUDE_SYSVAR, true);
	}
}
