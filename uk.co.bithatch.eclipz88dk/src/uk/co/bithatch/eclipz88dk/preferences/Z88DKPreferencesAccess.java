package uk.co.bithatch.eclipz88dk.preferences;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.core.resources.IProject;

import uk.co.bithatch.bitzx.LanguageSystemPreferencesAccess;
import uk.co.bithatch.eclipz88dk.Activator;
import uk.co.bithatch.eclipz88dk.Z88DKLanguageSystemProvider;
import uk.co.bithatch.eclipz88dk.toolchain.Z88DKSDK;

public class Z88DKPreferencesAccess extends LanguageSystemPreferencesAccess {
	
	public final static class Defaults {
		private final static Z88DKPreferencesAccess DEFAULT = new Z88DKPreferencesAccess();
	}

	protected Z88DKPreferencesAccess() {
		super(Activator.PLUGIN_ID, Z88DKLanguageSystemProvider.class);
	}
	
	public static Z88DKPreferencesAccess get() {
		return Defaults.DEFAULT;
	}

	@Deprecated
	public String getSystem(IProject project) {
		return getPreference(project, PreferenceConstants.SYSTEM, PreferenceInitializer.DEFAULT_SYSTEM);
	}

	public String getCLibrary(IProject project) {
		return getPreference(project, PreferenceConstants.CLIB, PreferenceInitializer.DEFAULT_CLIB);
	}
	
	public Optional<Z88DKSDK> getSDK(IProject project) {
		var sdk = getPreference(project, PreferenceConstants.SDK, "");
		if(sdk.equals("")) {
			try {
				return Optional.of(getAllSDKs().getFirst());
			}
			catch(NoSuchElementException nsee) {
				return Optional.empty();
			}
		}
		else {
			return getSDKByPath(sdk);
		}
	}

	public List<File> getAllSDKPaths() {
    	return getPathListPreference(null, PreferenceConstants.SDK_PATHS).stream().map(File::new).toList();
	}

	public Optional<Z88DKSDK> getSDKByName(String name) {
		return getAllSDKs().stream().filter(s -> s.name().equals(name)).findFirst();
	}
	
	public Optional<Z88DKSDK> getSDKByPath(String path) {
		return getAllSDKs().stream().filter(s -> s.location().getAbsolutePath().equals(path)).findFirst();
	}

	public List<Z88DKSDK> getAllSDKs() {
		return getAllSDKPaths().stream().map(f ->
    		Z88DKSDK.fromLocation(f)
    	).toList();
	}

	public String[][] getAllSDKsAsOptions() {
		return getAllSDKs().stream().map(sdk -> new String[] { sdk.name(), sdk.location().getAbsolutePath() }).toList().toArray(new String[0][0]);
	}

	public Optional<Z88DKSDK> getDefaultSDK() {
		var l = getAllSDKs();
		return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
	}
	

}
