package uk.co.bithatch.eclipz80.ui.preferences;

import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import uk.co.bithatch.bitzx.DefaultAbstractPreferenceInitializer;

public class AsmPreferenceInitializer extends DefaultAbstractPreferenceInitializer {

	public AsmPreferenceInitializer() {
		super(AsmPreferencesAccess.get());
	}

	@Override
	protected void onInit(IEclipsePreferences prefs, Set<String> keys) {
		setIfNotSet(prefs, AsmPreferenceConstants.OUTPUT_PATH, "bin", keys);
		setIfNotSet(prefs, AsmPreferenceConstants.ASSEMBLER_MODE,
				AsmPreferenceConstants.ASSEMBLER_MODE_BUILTIN, keys);
		setIfNotSet(prefs, AsmPreferenceConstants.EXTERNAL_COMMAND, "", keys);
		setIfNotSet(prefs, AsmPreferenceConstants.DEFINES, "", keys);
	}
}
