package uk.co.bithatch.eclipz80.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import uk.co.bithatch.bitzx.ISDK;
import uk.co.bithatch.bitzx.LanguageSystemPreferencesAccess;
import uk.co.bithatch.eclipz80.ui.internal.Eclipz80Activator;
import uk.co.bithatch.eclipz80.ui.language.AsmLanguageSystemProvider;

public class AsmPreferencesAccess extends LanguageSystemPreferencesAccess {

	private static final class Defaults {
		private static final AsmPreferencesAccess DEFAULT = new AsmPreferencesAccess();
	}

	public static AsmPreferencesAccess get() {
		return Defaults.DEFAULT;
	}

	protected AsmPreferencesAccess() {
		super(Eclipz80Activator.PLUGIN_ID, AsmLanguageSystemProvider.class);
	}

	public String getOutputPath(IProject project) {
		return getPreference(project, AsmPreferenceConstants.OUTPUT_PATH, "bin");
	}

	public IFolder getOutputFolder(IProject project) {
		return project.getFolder(getOutputPath(project));
	}

	public String getAssemblerMode(IProject project) {
		return getPreference(project, AsmPreferenceConstants.ASSEMBLER_MODE,
				AsmPreferenceConstants.ASSEMBLER_MODE_BUILTIN);
	}

	public boolean isBuiltinAssembler(IProject project) {
		return AsmPreferenceConstants.ASSEMBLER_MODE_BUILTIN.equals(getAssemblerMode(project));
	}

	public String getExternalCommand(IProject project) {
		return getPreference(project, AsmPreferenceConstants.EXTERNAL_COMMAND, "");
	}

	/**
	 * Returns the list of defines configured for the given project (or
	 * workspace-level defaults if no project-specific settings exist).
	 * Each entry is either {@code NAME} or {@code NAME=VALUE}.
	 */
	public List<String> getDefines(IProject project) {
		String raw = getPreference(project, AsmPreferenceConstants.DEFINES, "");
		if (raw.isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(Arrays.asList(raw.split(AsmPreferenceConstants.DEFINES_SEPARATOR)));
	}

	@Override
	public Optional<? extends ISDK> getSDK(IProject project) {
		return Optional.of(SDKDefaults.SDK);
	}
	
	private final static class SDKDefaults {
		private final static ISDK SDK = new ISDK() {

			@Override
			public String name() {
				return "EclipZ80";
			}
			
		};
	}
}
