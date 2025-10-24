package uk.co.bithatch.bitzx;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

public abstract class AbstractPreferencesAccess {
	
	public static void flushSilently(IEclipsePreferences prfs) {
		try {
			prfs.flush();
		} catch (BackingStoreException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static File resolve(IProject project, String path) {
		if (path.startsWith(File.separator)) {
			return new File(path);
		} else {
			return new File(project.getLocation().toFile(), path);
		}
	}
	
	private final String activatorId;
	
	protected AbstractPreferencesAccess(String activatorId) {
		this.activatorId = activatorId;
	}

	public final void copyToProjectPreferences(IProject project, String key) {
		if (project == null)
			throw new IllegalArgumentException();
		var wsprefs = getPreferences();
		var val = wsprefs.get(key, null);
		if (val != null)
			getPreferences(project).put(key, val);
	}

	public final List<String> getPathListPreference(IProject project, String key) {
		var str = getPreference(project, key, "");
		if (str.equals(""))
			return Collections.emptyList();
		else
			return Arrays.asList(str.split(Pattern.quote(File.pathSeparator)));
	}

	public final IEclipsePreferences getPreferences() {
		return InstanceScope.INSTANCE.getNode(activatorId);
	}

	public final IEclipsePreferences getPreferences(IProject project) {
		if (project == null)
			return getPreferences();
		else {
			IEclipsePreferences projectPrefs = new ProjectScope(project).getNode(activatorId);
			return projectPrefs;
		}
	}

	public final IPreferenceStore getPreferenceStore() {
		return new ScopedPreferenceStore(InstanceScope.INSTANCE, activatorId);
	}
	

	public final IPreferenceStore getPreferenceStore(IProject project) {
		return new ScopedPreferenceStore(new ProjectScope(project), activatorId);
	}

	public final String id() {
		return activatorId;
	}

	public final boolean isProjectSpecific(IProject project, String category) {
		return getPreferences(project).getBoolean(category + "." + IPreferenceConstants.PROJECT_SPECIFIC_SETTINGS,
				false);
	}

	public final boolean isProjectSpecificFor(IProject project, String key) {
		if (key.equals(IPreferenceConstants.PROJECT_SPECIFIC_SETTINGS))
			return false;
		else
			return isProjectSpecific(project, key.substring(0, key.indexOf('.')));
	}

	public final void setPathListPreference(IProject project, String key, List<String> values) {
		var prefs = getPreferences(project);
		prefs.put(key, String.join(File.pathSeparator, values));
	}

	public final void setProjectSpecific(IProject project, String category, boolean specific) {
		var prfs = getPreferences(project);
		try {
			var prfKeys = Arrays.asList(prfs.keys());
			var wsprefs = getPreferences();
			prfs.putBoolean(category + "." + IPreferenceConstants.PROJECT_SPECIFIC_SETTINGS, specific);
			if (specific) {
				for (var key : wsprefs.keys()) {
					if (key.startsWith(category + ".") && !prfKeys.contains(key)) {
						copyToProjectPreferences(project, key);
					}
				}
			}
			flushSilently(prfs);
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}


	public final void setProjectSpecificFor(IProject project, String key, boolean specific) {
		if (!key.equals(IPreferenceConstants.PROJECT_SPECIFIC_SETTINGS))
			setProjectSpecific(project, key.substring(0, key.indexOf('.')), specific);
	}

	protected final String getPreference(IProject project, String key, String defaultValue) {
		IEclipsePreferences projectPrefs = isProjectSpecificFor(project, key) ? getPreferences(project)
				: getPreferences();
		try {
			if (Arrays.asList(projectPrefs.keys()).contains(key)) {
				return projectPrefs.get(key, defaultValue);
			}
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		IEclipsePreferences instancePrefs = InstanceScope.INSTANCE.getNode(activatorId);
		return instancePrefs.get(key, defaultValue);
	}
}
