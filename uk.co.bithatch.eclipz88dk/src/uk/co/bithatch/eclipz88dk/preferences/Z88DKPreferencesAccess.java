package uk.co.bithatch.eclipz88dk.preferences;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import uk.co.bithatch.bitzx.IArchitecture;
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

	public final void setCLibrary(IProject project, String clib) {
		var prefs = getPreferences(project);
		prefs.put(PreferenceConstants.CLIB, clib);
		if (project != null)
			setProjectSpecificFor(project, PreferenceConstants.CLIB, true);
		flushSilently(prefs);
	}
	
	public List<? extends IArchitecture> getArchitectures(IProject project) {
		var sdkOpt = getSDK(project);
		if(sdkOpt.isPresent()) {
			return getLanguageSystem().architectures(project, sdkOpt.get().name());
		}
		else {
			return List.of();
		}
	}
	
	@Override
	public IFolder getOutputFolder(IProject project) {

		/* Get the CDT managed build info for the project */
		IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
		if (buildInfo == null) {
			throw new IllegalArgumentException("No CDT managed build information found for project: " + project.getName());
		}
		
		IConfiguration buildCfg = buildInfo.getDefaultConfiguration();
		if (buildCfg == null) {
			throw new IllegalArgumentException("No default build configuration found for project: " + project.getName());
		}
		
		/* Determine the output directory and file from the build configuration */
		return project.findMember(buildCfg.getName()).getAdapter(IFolder.class);
	}

	public boolean isAllArchitectures(IProject project) {
		return "true".equals(getPreference(project, PreferenceConstants.ALL_ARCHITECTURES, "false"));
	}

	public String getCLibrary(IProject project) {
		return getPreference(project, PreferenceConstants.CLIB, PreferenceInitializer.DEFAULT_CLIB);
	}

	public void setSDK(IProject project, Z88DKSDK sdk) {
		var prefs = getPreferences(project);
		prefs.put(PreferenceConstants.SDK, sdk.location().getAbsolutePath());
		if (project != null)
			setProjectSpecificFor(project, PreferenceConstants.SDK, true);
		flushSilently(prefs);
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
