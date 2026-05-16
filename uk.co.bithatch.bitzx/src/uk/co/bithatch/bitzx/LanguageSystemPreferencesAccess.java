package uk.co.bithatch.bitzx;

import java.util.Optional;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

public abstract class LanguageSystemPreferencesAccess extends AbstractPreferencesAccess {

	private final Class<? extends ILanguageSystemProvider> clazz;

	protected LanguageSystemPreferencesAccess(String activatorId, Class<? extends ILanguageSystemProvider> clazz) {
		super(activatorId);
		this.clazz = clazz;
	}
	
	public abstract Optional<? extends ISDK> getSDK(IProject project);
	
	public final IOutputFormat getOutputFormat(IProject project) {
		return LanguageSystem.outputFormatOrDefault(project, getPreference(project, LanguageSystemPreferenceConstants.OUTPUT_FORMAT, ""));
	}

	public final void setArchitecture(IProject project, IArchitecture arch) {
		var prefs = getPreferences(project);
		prefs.put(LanguageSystemPreferenceConstants.ARCHITECTURE, arch.name());
		if (project != null)
			setProjectSpecificFor(project, LanguageSystemPreferenceConstants.ARCHITECTURE, true);
		flushSilently(prefs);
	}

	public final void setOutputFormat(IProject project, IOutputFormat fmt) {
		var prefs = getPreferences(project);
		prefs.put(LanguageSystemPreferenceConstants.OUTPUT_FORMAT, fmt.name());
		if (project != null)
			setProjectSpecificFor(project, LanguageSystemPreferenceConstants.OUTPUT_FORMAT, true);
		flushSilently(prefs);
	}

	public final IArchitecture getArchitecture(IProject project) {
		var defaultArch = getPreference(project, LanguageSystemPreferenceConstants.ARCHITECTURE, "");
		return getLanguageSystem().architectureOrDefault(project, defaultArch);
	}

	public final ILanguageSystemProvider getLanguageSystem() {
		return LanguageSystem.languageSystem(clazz);
	}

	public abstract IFolder getOutputFolder(IProject project);

}
