package uk.co.bithatch.eclipz80.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;

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

	public boolean isGenerateMap(IProject project) {
		return "true".equals(getPreference(project, AsmPreferenceConstants.GENERATE_MAP, "false"));
	}

	public List<String> getDefines(IProject project) {
		String raw = getPreference(project, AsmPreferenceConstants.DEFINES, "");
		if (raw.isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(Arrays.asList(raw.split(AsmPreferenceConstants.DEFINES_SEPARATOR)));
	}
	
	public Map<String, String> getDefinesMap(IProject project) {
		return getDefines(project).stream().collect(Collectors.toMap(v -> {
			var idx = v.indexOf('=');
			return idx == -1 ? v : v.substring(0, idx);
		}, v -> { 
			var idx = v.indexOf('=');
			return idx == -1 ? null : v.substring(idx + 1);
		}));
	}
	
	public Map<String, String> getAllDefinesMap(IProject project) {
		return getAllProjectReferences(project).
				stream().
				flatMap(prj ->
					getDefinesMap(prj).entrySet().stream()
				).
				collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public Optional<? extends ISDK> getSDK(IProject project) {
		return Optional.of(SDKDefaults.SDK);
	}


	public List<IProject> getAllProjectReferences(IProject project) {
		try {
			return Arrays.asList(project.getDescription().getReferencedProjects()).
				stream().
				flatMap(prj -> 
					Stream.concat(
						Stream.of(project), 
						getAllProjectReferences(prj).stream()
					)).
				toList();

		} catch (CoreException e) {
			return Collections.emptyList();
		}
	}

	public List<URI> getProjectReferencesURIs(IProject project) {
		return getAllProjectReferences(project).stream().map(prj -> URI.createURI(prj.getLocationURI().toString())).toList();
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
