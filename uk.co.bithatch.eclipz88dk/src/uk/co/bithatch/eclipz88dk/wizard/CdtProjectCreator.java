package uk.co.bithatch.eclipz88dk.wizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.cdt.build.core.scannerconfig.ScannerConfigNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.language.ProjectLanguageConfiguration;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.LanguageManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedCProjectNature;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

public final class CdtProjectCreator {

	public enum CdtType {
		EXECUTABLE("uk.co.bithatch.eclipz88dk.executable", "uk.co.bithatch.eclipz88dk.debug.exe",
				"uk.co.bithatch.eclipz88dk.release.exe", "org.eclipse.cdt.build.core.buildArtefactType.exe", "bin"),
		LIBRARY("uk.co.bithatch.eclipz88dk.library", "uk.co.bithatch.eclipz88dk.debug.so",
				"uk.co.bithatch.eclipz88dk.release.so", "org.eclipse.cdt.build.core.buildArtefactType.sharedLibrary",
				"lib");

		private final String projectTypeId;
		private final String debugId;
		private final String releaseId;
		private final String artefactType;
		private final String artefactExt;

		private CdtType(String projectTypeId, String debugId, String releaseId, String artfactType,
				String artefactExt) {
			this.projectTypeId = projectTypeId;
			this.debugId = debugId;
			this.releaseId = releaseId;
			this.artefactType = artfactType;
			this.artefactExt = artefactExt;
		}

		public String pojectTypeId() {
			return projectTypeId;
		}

		public String debugId() {
			return debugId;
		}

		public String releaseId() {
			return releaseId;
		}

		public String artefactType() {
			return artefactType;
		}

		public String artefactExt() {
			return artefactExt;
		}

		public static Optional<CdtType> forProject(IProject project) {

			var info = ManagedBuildManager.getBuildInfo(project);
			var cfg = (info != null) ? info.getDefaultConfiguration() : null;
			var pt = (cfg != null) ? cfg.getProjectType() : null;
			if (pt != null) {
				for (var t : values()) {
					if (t.projectTypeId.equals(pt.getId())) {
						return Optional.of(t);
					}
				}
			}
			return Optional.empty();
		}
	}

	// --- Your IDs from plugin.xml ---

	private static final String TOOLCHAIN_ID = "uk.co.bithatch.eclipz88dk.toolChain";
	private static final String LSP_ID = "uk.co.bithatch.eclipz88dk.languageSettingsProvider";
	private static final String LANG_ID = "uk.co.bithatch.eclipz88dk.language.c";
	private static final String CTSRC_ID = "org.eclipse.cdt.core.cSource";
	private static final String CTHDR_ID = "org.eclipse.cdt.core.cHeader";
	// Build artefact metadata

	private CdtProjectCreator() {
	}

	/**
	 * Create a Managed Build CDT project using your projectType & configurations.
	 * 
	 * @throws BuildException
	 */
	public static IProject createManagedCProject(CdtType cdtType, String projectName, IProgressMonitor pm)
			throws CoreException, BuildException {
		if (pm == null)
			pm = new NullProgressMonitor();
		pm.beginTask("Create CDT project: " + projectName, IProgressMonitor.UNKNOWN);

		// 1) Create/open IProject
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (!project.exists())
			project.create(pm);
		if (!project.isOpen())
			project.open(pm);
		
		// true = writable description (we’re going to modify it)
		ICProjectDescription pd = CoreModel.getDefault().getProjectDescription(project, /* write */ true);
		if (pd == null)
			throw new IllegalArgumentException("Cannot get project descrpition.");

//		configureType(cdtType, pm, project);
		addContentTypeMappings(project);

		addLanguageSettingsProvider(pd, project);

		// Persist to .cproject
		CoreModel.getDefault().setProjectDescription(project, pd);

		ICProject cproj = CoreModel.getDefault().create(project); // project -> ICProject
		if (cproj != null) {
			CCorePlugin.getIndexManager().reindex(cproj);
		}
		return project;
	}

	private static void addContentTypeMappings(IProject project) throws CoreException {
		

		ProjectLanguageConfiguration plc = LanguageManager.getInstance().getLanguageConfiguration(project);
		// Build the expected structure: cfgId -> { contentTypeId -> languageId }
//		Map<String, Map<String, String>> mappings = new HashMap<>();
//		for (ICConfigurationDescription cfg : pd.getConfigurations()) {
//			Map<String, String> ctMap = new HashMap<>();
//			ctMap.put(CTSRC_ID, LANG_ID);
//			ctMap.put(CTHDR_ID, LANG_ID);
//			mappings.put(cfg.getId(), ctMap);
//
//			// Point source entry at /<project>/src (recommended)
//			ICSourceEntry[] entries = new ICSourceEntry[] { new CSourceEntry(project.getFullPath(), null
//					/* exclusions */, 0 /* flags */) };
//			cfg.setSourceEntries(entries);
//
//		}
		var mappings = Map.of("", Map.of(
			CTSRC_ID, LANG_ID,
			CTHDR_ID, LANG_ID
		));

		plc.setContentTypeMappings(mappings); // apply per-config language mappings


	}

	private static IProject configureType(CdtType cdtType, IProgressMonitor pm, IProject project)
			throws CoreException, BuildException {
		// 2) Add natures: C, Managed Build, Scanner Config
		addCdtNatures(project, pm);

		// 3) Create the Managed Build info BEFORE creating the managed project
		// (prevents NPE)
		ManagedBuildManager.createBuildInfo(project);
		ManagedBuildManager.setNewProjectVersion(project); // optional, good hygiene

		// 4) Writable C project description (.cproject in memory)
		ICProjectDescriptionManager pdm = CoreModel.getDefault().getProjectDescriptionManager();
		ICProjectDescription projDesc = pdm.createProjectDescription(project, /* isNew */ true,
				/* usePlatformDefaults */ true);

		// 5) Managed Project (tie the MBS model to the IProject)
		IProjectType ptype = ManagedBuildManager.getExtensionProjectType(cdtType.projectTypeId);
		if (ptype == null)
			throw new CoreException(err("ProjectType not found: " + cdtType.projectTypeId));

		IManagedProject mproj = ManagedBuildManager.createManagedProject(project, ptype);

		// 6) Clone your extension configurations into the project
		IConfiguration extDebug = findExtConfigById(ptype, cdtType.debugId);
		IConfiguration extRelease = findExtConfigById(ptype, cdtType.releaseId);
		if (extDebug == null || extRelease == null) {
			throw new CoreException(
					err("Extension configurations not found under projectType: " + cdtType.projectTypeId));
		}

		IConfiguration projDebugCfg = cloneExtConfigIntoProject(projDesc, mproj, extDebug, "Debug", project.getName(),
				cdtType.artefactType, cdtType.artefactExt);
		cloneExtConfigIntoProject(projDesc, mproj, extRelease, "Release", project.getName(), cdtType.artefactType,
				cdtType.artefactExt);

		// 7) Make Debug active
		ICConfigurationDescription active = projDesc.getConfigurationById(projDebugCfg.getConfigurationData().getId());
		if (active != null)
			projDesc.setActiveConfiguration(active);

		// 8) Persist .cproject and build info
		CoreModel.getDefault().setProjectDescription(project, projDesc);
		ManagedBuildManager.saveBuildInfo(project, true);

		// 9) Ensure MBS builders are present
		ensureMbsBuilders(project, pm);

		pm.done();
		return project;
	}

	// ---------- helpers ----------

	private static IConfiguration cloneExtConfigIntoProject(ICProjectDescription projDesc, IManagedProject mproj,
			IConfiguration extCfg, String instanceName, String artefactBaseName, String artifactType,
			String arttifactExt) throws CoreException, BuildException {
		// Create a new per-project configuration from the extension config
		String newCfgId = ManagedBuildManager.calculateChildId(extCfg.getId(), null);
		IConfiguration projCfg = ManagedBuildManager.createConfigurationForProject(projDesc, mproj, extCfg, newCfgId);

		// Artefact metadata
		projCfg.setName(instanceName);
		projCfg.setArtifactName(artefactBaseName);
		projCfg.setBuildArtefactType(artifactType);
		projCfg.setArtifactExtension(arttifactExt);

		// Sanity: your toolchain definition should exist (comes via extCfg)
		if (ManagedBuildManager.getExtensionToolChain(TOOLCHAIN_ID) == null) {
			throw new CoreException(err("Toolchain not found: " + TOOLCHAIN_ID));
		}

		// Bridge MBS -> CCore: add an ICConfigurationDescription for this MBS config
		CConfigurationData data = projCfg.getConfigurationData();
		projDesc.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);

		return projCfg;
	}

	private static IConfiguration findExtConfigById(IProjectType ptype, String id) {
		return Arrays.stream(ptype.getConfigurations()).filter(c -> id.equals(c.getId())).findFirst().orElse(null);
	}

	private static void addCdtNatures(IProject project, IProgressMonitor pm) throws CoreException {
		IProjectDescription d = project.getDescription();
		d.setNatureIds(new String[] { CProjectNature.C_NATURE_ID, ManagedCProjectNature.MNG_NATURE_ID,
				ScannerConfigNature.NATURE_ID });
		project.setDescription(d, pm);
	}

	private static void addLanguageSettingsProvider(ICProjectDescription pd, IProject project) throws CoreException {
		for (ICConfigurationDescription cfg : pd.getConfigurations()) {
			if (!(cfg instanceof ILanguageSettingsProvidersKeeper keeper))
				continue;

			// Current providers for this configuration
			List<ILanguageSettingsProvider> list = new ArrayList<>(keeper.getLanguageSettingProviders());

			boolean present = list.stream().anyMatch(p -> LSP_ID.equals(p.getId()));
			if (!present) {
				// Make a project-scoped copy of your extension provider
				ILanguageSettingsProvider copy = LanguageSettingsManager.getExtensionProviderCopy(LSP_ID,
						/* deep */ true);
				if (copy != null) {
					list.add(copy);
					keeper.setLanguageSettingProviders(list);
				}
			}
		}
		// Optional but recommended: trigger a reindex so your language is used
		// immediately
//		org.eclipse.cdt.core.CCorePlugin.getIndexManager().reindex((ICProject) pd.getProject());

	}

	@Deprecated
	/* TODO not sure this is needed */
	private static void ensureMbsBuilders(IProject project, IProgressMonitor pm) throws CoreException {
		IProjectDescription d = project.getDescription();
		ICommand[] cmds = d.getBuildSpec();

		final String GENMAKE = "org.eclipse.cdt.managedbuilder.core.genmakebuilder";
		final String SCANNER = "org.eclipse.cdt.managedbuilder.core.ScannerConfigBuilder";

		boolean hasGen = false, hasSc = false;
		for (ICommand c : cmds) {
			if (GENMAKE.equals(c.getBuilderName()))
				hasGen = true;
			if (SCANNER.equals(c.getBuilderName()))
				hasSc = true;
		}
		if (hasGen && hasSc)
			return;

		ICommand gen = d.newCommand();
		gen.setBuilderName(GENMAKE);
		ICommand sc = d.newCommand();
		sc.setBuilderName(SCANNER);

		ICommand[] newSpec;
		if (!hasGen && !hasSc) {
			newSpec = new ICommand[] { gen, sc };
		} else {
			newSpec = new ICommand[cmds.length + (hasGen ? 0 : 1) + (hasSc ? 0 : 1)];
			int i = 0;
			for (ICommand c : cmds)
				newSpec[i++] = c;
			if (!hasGen)
				newSpec[i++] = gen;
			if (!hasSc)
				newSpec[i++] = sc;
		}
		d.setBuildSpec(newSpec);
		project.setDescription(d, pm);
	}

	private static IStatus err(String msg) {
		return new Status(IStatus.ERROR, "uk.co.bithatch.eclipz88dk", msg);
	}
}
