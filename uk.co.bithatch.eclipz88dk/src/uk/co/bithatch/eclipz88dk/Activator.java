package uk.co.bithatch.eclipz88dk;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionListener;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

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
	}

	private final ICProjectDescriptionListener descListener = event -> {
		var pd = event.getNewCProjectDescription();
		if (pd == null || event.getOldCProjectDescription() != null)
			return; // nothing to do
		
		var project = pd.getProject();
		
		CdtType.forProject(project).ifPresent(type -> {
			try {
				CdtProjectCreator.createManagedCProject(type, project.getName(), new NullProgressMonitor());
			} catch (Exception e) {
				LOG.error("Failed to enable Z88DK features on project.", e);
			}
		});
		
//		try {
//			
//			PlatformUI.getWorkbench().getAdapter(IWorkbenchWindow.class).run(true, false, new WorkspaceModifyOperation() {
//				@Override
//				protected void execute(IProgressMonitor monitor) throws CoreException {
//					
//				}
//			});
//		} catch (Exception e) {
//			LOG.error("Failed to enable Z88DK features on project.", e);
//		}
		
//		CdtType.forProject(project).ifPresent(type -> {
//			
//			var operation = new WorkspaceModifyOperation() {
//				@Override
//				protected void execute(IProgressMonitor monitor) throws CoreException {
//					try {
//						delegate.call(monitor);
//						checkPerspective();
//					} catch (CoreException ce) {
//						throw ce;
//					} catch (Exception e) {
//						throw new CoreException(Status.error("Failed to create project.", e));
//					}
//				}
//			};
//
//			try {
//				getContainer().run(true, true, operation);
//			} catch (InvocationTargetException | InterruptedException e) {
//				// Handle exception or cancel
//				return false;
//			}
//			
//			
//			
//			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
//				/*
//				 * Get of this thread, we are here as a result of project description changing,
//				 * and connect recursively persist it
//				 */
//				try {
//					CdtProjectCreator.createManagedCProject(type, project.getName(), new NullProgressMonitor());
////					Z88DK.enableOnProject(project);
//					
////					var plc = LanguageManager.getInstance().getLanguageConfiguration(project);
////					System.out.println("Mappings: " + plc.getContentTypeMappings()); // should show your cfgIds with cSource/cHeader → LANG_ID
////
////					var npd = CoreModel.getDefault().getProjectDescription(project, /* write */ true);
////					var activeCfg = npd.getActiveConfiguration();
////					IFile f = npd.getProject().getFile("hiworld.c");
////					var lang = org.eclipse.cdt.core.model.LanguageManager.getInstance()
////					             .getLanguageForFile(f, activeCfg);
////					System.out.println("Resolved language: " + (lang != null ? lang.getId() : "<none>"));
//				} catch (Exception e) {
//					LOG.error("Failed to enable Z88DK features on project.", e);
//				}
//			});			
//		});
	};

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
