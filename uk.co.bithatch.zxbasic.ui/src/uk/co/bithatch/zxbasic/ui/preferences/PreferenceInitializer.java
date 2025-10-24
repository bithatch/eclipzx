package uk.co.bithatch.zxbasic.ui.preferences;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import uk.co.bithatch.bitzx.DefaultAbstractPreferenceInitializer;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicArchitecture;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicOutputFormat;
import uk.co.bithatch.zxbasic.ui.library.ContributedSDKRegistry;
import uk.co.bithatch.zxbasic.ui.library.ZXSDK;

public class PreferenceInitializer extends DefaultAbstractPreferenceInitializer {

	public PreferenceInitializer() {
		super(ZXBasicPreferencesAccess.get());
	}

	@Override
	protected void onInit(IEclipsePreferences prefs, Set<String> keys) {
		setIfNotSet(prefs, ZXBasicPreferenceConstants.OUTPUT_PATH, "bin", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.ARCHITECTURE, BorielZXBasicArchitecture.LEGACY.name(), keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.OPTIMIZATION_LEVEL, "2", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.OUTPUT_FORMAT, BorielZXBasicOutputFormat.BIN.name(), keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.DEFINES, "ECLIPZX", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.LIB_PATHS, "lib", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.STRICT, "false", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.STRICT_BOOLEAN, "false", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.IGNORE_VARIABLE_CASE, "true", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.LEGACY_INSTRUCTIONS, "false", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.DEBUG_LEVEL, "0", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.DEBUG_ARRAYS, "false", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.DEBUG_MEMORY, "false", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.ARRAY_BASE, "0", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.STRING_BASE, "0", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.BASIC_LOADER, "false", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.AUTORUN, "false", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.BREAK_DETECTION, "false", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.HEAP_SIZE, "0", keys);
		setIfNotSet(prefs, ZXBasicPreferenceConstants.HEAP_ADDRESS, "0", keys);
        
        List<ZXSDK> sdks = ContributedSDKRegistry.getAllSDKs();
        if(!sdks.isEmpty() && !keys.contains(ZXBasicPreferenceConstants.SDK)) { 
        	prefs.put(ZXBasicPreferenceConstants.SDK, sdks.get(0).location().getAbsolutePath());
        }
	}
}

