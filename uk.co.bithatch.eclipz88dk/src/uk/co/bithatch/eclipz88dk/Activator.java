package uk.co.bithatch.eclipz88dk;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionListener;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import uk.co.bithatch.eclipz88dk.preferences.PreferenceInitializer;
import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.eclipz88dk.toolchain.Z88DKNature;
import uk.co.bithatch.eclipz88dk.wizard.CdtProjectCreator;
import uk.co.bithatch.eclipz88dk.wizard.CdtProjectCreator.CdtType;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	
	public final static ILog LOG = ILog.of(Activator.class);

	// The plug-in ID
	public static final String PLUGIN_ID = "uk.co.bithatch.eclipz88dk"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		CoreModel.getDefault().getProjectDescriptionManager().addCProjectDescriptionListener(descListener,
				CProjectDescriptionEvent.APPLIED /* CProjectDescriptionEvent.APPLIED *//* | CProjectDescriptionEvent.CREATED */);
		PreferenceInitializer.checkForZCCCFG(Z88DKPreferencesAccess.get());
	}

	private final ICProjectDescriptionListener descListener = event -> {
		var pd = event.getNewCProjectDescription();
		if (pd == null)
			return;
		
		var project = pd.getProject();
		
		/* Skip if already has Z88DK nature */
		try {
			if (project.hasNature(Z88DKNature.NATURE_ID))
				return;
		} catch (Exception e) {
			/* project not open or nature not installed yet */
		}
		
		/* Check if this is a Z88DK project by project type OR toolchain */
		if (isZ88DKProject(project)) {
			Job.create("Enable Z88DK features", monitor -> {
				try {
					CdtProjectCreator.enableZ88DKFeatures(project);
				} catch (Exception e) {
					LOG.error("Failed to enable Z88DK features on project.", e);
				}
			}).schedule();
		}
	};

	/**
	 * Check whether a project uses the Z88DK toolchain, either by matching
	 * one of our known project types, or by checking if any configuration's
	 * toolchain derives from the Z88DK toolchain.  This catches projects
	 * created via the standard CDT "New C/C++ Project" wizard.
	 */
	private static boolean isZ88DKProject(org.eclipse.core.resources.IProject project) {
		/* 1. Check project type (our own wizard) */
		if (CdtType.forProject(project).isPresent())
			return true;
		
		/* 2. Check toolchain (standard CDT wizard with Z88DK toolchain selected) */
		try {
			var info = ManagedBuildManager.getBuildInfo(project);
			if (info != null) {
				for (var cfg : info.getManagedProject().getConfigurations()) {
					var tc = cfg.getToolChain();
					while (tc != null) {
						var id = tc.getId();
						if (id != null && id.contains("uk.co.bithatch.eclipz88dk")) {
							return true;
						}
						tc = tc.getSuperClass();
					}
				}
			}
		} catch (Exception e) {
			/* MBS not ready yet, that's fine */
		}
		
		return false;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}