package uk.co.bithatch.zxbasic.ui.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import uk.co.bithatch.bitzx.LanguageSystemPreferencesAccess;
import uk.co.bithatch.zxbasic.tools.NexConverter;
import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicLanguageSystemProvider;
import uk.co.bithatch.zxbasic.ui.library.ContributedLibraryRegistry;
import uk.co.bithatch.zxbasic.ui.library.ContributedSDKRegistry;
import uk.co.bithatch.zxbasic.ui.library.ZXLibrary;
import uk.co.bithatch.zxbasic.ui.library.ZXSDK;
import uk.co.bithatch.zxbasic.ui.tools.ZXBC.Warning;

public class ZXBasicPreferencesAccess extends LanguageSystemPreferencesAccess {
	public final static class Defaults {
		private final static ZXBasicPreferencesAccess DEFAULT = new ZXBasicPreferencesAccess();
	}

	public static ZXBasicPreferencesAccess get() {
		return Defaults.DEFAULT;
	}

	protected ZXBasicPreferencesAccess() {
		super(ZXBasicUiActivator.PLUGIN_ID, BorielZXBasicLanguageSystemProvider.class);
	}
	
	public String getPython() {
		return getPreferences().get(ZXBasicPreferenceConstants.SDK_PYTHON_LOCATION, "");
	}

	public String getNEXCore(IProject project) {
		return getPreferences(project).get(ZXBasicPreferenceConstants.NEX_CORE, NexConverter.DEFAULT_CORE_STR);
	}

	public String getNEXSysvarLocation(IProject project) {
		return getPreferences(project).get(ZXBasicPreferenceConstants.NEX_SYSVAR_LOCATION, "");
	}

	public boolean isNEXIncludeSysvar(IProject project) {
		return getPreferences(project).getBoolean(ZXBasicPreferenceConstants.NEX_INCLUDE_SYSVAR, true);
	}

	public IFolder getOutputFolder(IProject project) {
		return project.getFolder(getOutputPath(project));
	}

	public String getOutputPath(IProject project) {
		return getPreference(project, ZXBasicPreferenceConstants.OUTPUT_PATH, "bin/");
	}

	public boolean isStrict(IProject project) {
		return Boolean.parseBoolean(getPreference(project, ZXBasicPreferenceConstants.STRICT, "false"));
	}

	public boolean isAutorun(IProject project) {
		return Boolean.parseBoolean(getPreference(project, ZXBasicPreferenceConstants.AUTORUN, "false"));
	}

	public boolean isBasicLoader(IProject project) {
		return Boolean.parseBoolean(getPreference(project, ZXBasicPreferenceConstants.BASIC_LOADER, "false"));
	}

	public boolean isBreakDetection(IProject project) {
		return Boolean.parseBoolean(getPreference(project, ZXBasicPreferenceConstants.BREAK_DETECTION, "false"));
	}

	public int getHeapAddress(IProject project) {
		return Integer.parseInt(getPreference(project, ZXBasicPreferenceConstants.HEAP_ADDRESS, "0"));
	}

	public int getHeapSize(IProject project) {
		return Integer.parseInt(getPreference(project, ZXBasicPreferenceConstants.HEAP_SIZE, "0"));
	}

	public boolean isStrictBoolean(IProject project) {
		return Boolean.parseBoolean(getPreference(project, ZXBasicPreferenceConstants.STRICT_BOOLEAN, "false"));
	}

	public boolean isExplicitDeclaration(IProject project) {
		return Boolean.parseBoolean(getPreference(project, ZXBasicPreferenceConstants.EXPLICIT_DECLARATION, "false"));
	}

	public int getArrayBase(IProject project) {
		return Integer.parseInt(getPreference(project, ZXBasicPreferenceConstants.ARRAY_BASE, "0"));
	}

	public int getStringBase(IProject project) {
		return Integer.parseInt(getPreference(project, ZXBasicPreferenceConstants.STRING_BASE, "0"));
	}

	public List<Warning> getSuppressedWarnings(IProject project) {
		var l = new ArrayList<Warning>();
		for (var w : Warning.values()) {
			if (Boolean.parseBoolean(
					getPreference(project, ZXBasicPreferenceConstants.ERRORS_AND_WARNINGS + "." + w.name(), "false"))) {
				l.add(w);
			}
		}
		return l;
	}

	public void setSDK(IProject project, ZXSDK sdk) {
		var prefs = getPreferences(project);
		prefs.put(ZXBasicPreferenceConstants.SDK, sdk.name());
		if (project != null)
			setProjectSpecificFor(project, ZXBasicPreferenceConstants.SDK, true);
		flushSilently(prefs);
	}

	public int getOptimizationLevel(IProject project) {
		return Integer.parseInt(getPreference(project, ZXBasicPreferenceConstants.OPTIMIZATION_LEVEL, "2"));
	}

	public int getDebugLevel(IProject project) {
		return Integer.parseInt(getPreference(project, ZXBasicPreferenceConstants.DEBUG_LEVEL, "0"));
	}

	public boolean isDebugArrays(IProject project) {
		return Boolean.parseBoolean(getPreference(project, ZXBasicPreferenceConstants.DEBUG_ARRAYS, "false"));
	}

	public boolean isDebugMemory(IProject project) {
		return Boolean.parseBoolean(getPreference(project, ZXBasicPreferenceConstants.DEBUG_MEMORY, "false"));
	}

	public boolean isLegacyInstructions(IProject project) {
		return Boolean.parseBoolean(getPreference(project, ZXBasicPreferenceConstants.LEGACY_INSTRUCTIONS, "false"));
	}

	public boolean isIgnoreVariableCase(IProject project) {
		return Boolean.parseBoolean(getPreference(project, ZXBasicPreferenceConstants.IGNORE_VARIABLE_CASE, "false"));
	}

	public void addDependency(IProject project, ZXLibrary lib) {
		var list = new LinkedHashSet<>(getConfiguredDependencies(project));
		list.add(lib.name());
		IEclipsePreferences prefs = getPreferences(project);
		prefs.put(ZXBasicPreferenceConstants.DEPENDENCIES, String.join(File.pathSeparator, list));
		if (project != null)
			setProjectSpecificFor(project, ZXBasicPreferenceConstants.DEPENDENCIES, true);
		flushSilently(prefs);
	}

	public Map<String, String> getDefines(IProject project) {
		String val = getPreference(project, ZXBasicPreferenceConstants.DEFINES, "");
		Map<String, String> result = new LinkedHashMap<>();
		if (!val.isBlank()) {
			for (String entry : val.split(";")) {
				String[] kv = entry.split("=", 2);
				if (kv.length == 2) {
					result.put(kv[0], kv[1]);
				}
			}
		}
		return result;
	}

	public ZXSDK getSDK(IProject project) {
		return ContributedSDKRegistry.getSDKByPath(getPreference(project, ZXBasicPreferenceConstants.SDK, ""))
				.orElseGet(() -> ContributedSDKRegistry.getDefaultSDK());
	}

	public List<File> getExternalLibs(IProject project) {
		return getConfiguredLibraryPaths(project).stream().filter(p -> p.length() > 0).map(p -> {
			return resolve(project, p);
		}).toList();
	}

	public List<String> getConfiguredLibraryPaths(IProject project) {
		return getStringListPreference(project, ZXBasicPreferenceConstants.LIB_PATHS);
	}

	public List<String> getConfiguredDependencies(IProject project) {
		return getStringListPreference(project, ZXBasicPreferenceConstants.DEPENDENCIES);
	}

	public List<String> getStringListPreference(IProject project, String key) {
		var str = getPreference(project, key, "");
		if (str.equals(""))
			return Collections.emptyList();
		else
			return Arrays.asList(str.split(Pattern.quote(File.pathSeparator)));
	}

	public List<File> getAllLibs(IProject project) {
		return Stream
				.concat(Stream.concat(getProjectReferencesAndLibs(project).stream(), getExternalLibs(project).stream()),
						getContributedLibs(project).stream())
				.distinct().toList();
	}

	public List<File> getProjectReferencesAndLibs(IProject project) {
		try {
			return Arrays.asList(project.getDescription().getReferencedProjects()).stream()
					.flatMap(prj -> Stream.concat(Stream.of(prj.getLocation().toFile()), getAllLibs(prj).stream()))
					.toList();

		} catch (CoreException e) {
			return Collections.emptyList();
		}
	}

	public List<File> getContributedLibs(IProject project) {
		return ContributedLibraryRegistry.getAllLibraries(project).stream().filter(l -> isValid(project, l))
				.map(ZXLibrary::location).toList();
	}

	private boolean isValid(IProject project, ZXLibrary lib) {
		return lib.arch() == null || lib.arch() == getArchitecture(project);
	}

}
