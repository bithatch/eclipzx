package uk.co.bithatch.eclipz88dk.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import uk.co.bithatch.bitzx.AbstractPreferencesAccess;
import uk.co.bithatch.bitzx.DefaultAbstractPreferenceInitializer;

public class PreferenceInitializer extends DefaultAbstractPreferenceInitializer {

	public static final String DEFAULT_SYSTEM = "zx";
	public static final String DEFAULT_CLIB = "new";

	public PreferenceInitializer() {
		super(Z88DKPreferencesAccess.get());
	}

	@Override
	protected void onInit(IEclipsePreferences prefs, Set<String> keys) { 
		setIfNotSet(prefs, Z88DKPreferenceConstants.ARCHITECTURE, DEFAULT_SYSTEM, keys);
		setIfNotSet(prefs, Z88DKPreferenceConstants.CLIB, DEFAULT_CLIB, keys);
    }

	public static void checkForZCCCFG(AbstractPreferencesAccess pax) {
		var paths = pax.getPathListPreference(null, Z88DKPreferenceConstants.SDK_PATHS);
        var zcccfg = System.getenv("ZCCCFG");
        if(zcccfg != null) {
        	var zcccfgFile = new File(zcccfg);
        	if(zcccfgFile.exists()) {
        		var zcc = zcccfgFile.getParentFile().getParentFile();
        		if(!paths.contains(zcc.getAbsolutePath())) {
        			paths = new ArrayList<String>(paths);
        			paths.add(zcc.getAbsolutePath());
        			pax.setPathListPreference(null, Z88DKPreferenceConstants.SDK_PATHS, paths);
        		}
        	}
        }
	}
}

