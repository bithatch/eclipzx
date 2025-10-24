package uk.co.bithatch.bitzx;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public abstract class DefaultAbstractPreferenceInitializer extends AbstractPreferenceInitializer {
	
	protected final AbstractPreferencesAccess pax;

	protected DefaultAbstractPreferenceInitializer(AbstractPreferencesAccess pax) {
		this.pax = pax;
	}
	
    @Override
    public final void initializeDefaultPreferences() {
        var prefs = pax.getPreferences();
        
        Set<String> keys;
		try {
			keys = Set.of(prefs.keys());
		} catch (BackingStoreException e) {
			keys = Collections.emptySet();
		}
		
		onInit(prefs, keys);
        
    }
    
    protected abstract void onInit(IEclipsePreferences prefs, Set<String> keys);

	protected static void setIfNotSet(Preferences prefs, String key, String defaultValue, Set<String> keys) {
    	if(!keys.contains(key)) {
    		prefs.put(key, defaultValue);
    	}
    }
}

