package uk.co.bithatch.zximgconv;

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 * <p>
 * Also installs a resource change listener that automatically adds the
 * ZX Image Conversion nature to projects that have known ZX development
 * natures (Z88DK, ZX Basic) — without requiring a compile-time dependency
 * on those plugins.
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "uk.co.bithatch.zximgconv"; //$NON-NLS-1$

	private static final ILog LOG = ILog.of(Activator.class);

	/**
	 * Nature IDs from other plugins that should trigger automatic addition of the
	 * image conversion nature. Listed as strings to avoid a hard dependency.
	 */
	private static final String[] AUTO_TRIGGER_NATURES = {
		"uk.co.bithatch.eclipz88dk.Z88DKNature",
		"uk.co.bithatch.zxbasic.ZXBasicNature",
	};

	private static Activator plugin;

	private IResourceChangeListener natureListener;

	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		installNatureAutoAddListener();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (natureListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(natureListener);
			natureListener = null;
		}
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Listens for project description changes. When a project gains one of the
	 * known ZX development natures, this automatically adds
	 * {@link ZXImageConversionNature} if not already present.
	 * <p>
	 * The actual nature addition is deferred to a {@link WorkspaceJob} to avoid
	 * modifying the workspace from within a {@code POST_CHANGE} listener (which
	 * would cause a re-entrancy / {@code IllegalStateException}).
	 */
	private void installNatureAutoAddListener() {
		natureListener = event -> {
			if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
				return;
			}
			IResourceDelta rootDelta = event.getDelta();
			if (rootDelta == null) return;

			try {
				rootDelta.accept((IResourceDeltaVisitor) delta -> {
					IResource resource = delta.getResource();

					// At workspace root level, continue into children
					if (resource.getType() == IResource.ROOT) {
						return true;
					}

					// At project level, check for description changes or new projects
					if (resource instanceof IProject project) {
						int kind = delta.getKind();
						int flags = delta.getFlags();

						boolean isDescChange = (flags & IResourceDelta.DESCRIPTION) != 0;
						boolean isNewProject = (kind == IResourceDelta.ADDED);
						boolean isOpened = (flags & IResourceDelta.OPEN) != 0;

						if ((isDescChange || isNewProject || isOpened) && project.isOpen()) {
							LOG.info("Detected project change for '" + project.getName()
									+ "' (kind=" + kind + ", flags=" + flags
									+ ", desc=" + isDescChange
									+ ", new=" + isNewProject
									+ ", opened=" + isOpened + ")");
							scheduleNatureCheck(project);
						}
						return false; // don't visit children of project
					}

					return false; // don't go deeper than project level
				});
			} catch (CoreException e) {
				LOG.warn("Error in nature auto-add listener: " + e.getMessage(), e);
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				natureListener, IResourceChangeEvent.POST_CHANGE);
	}

	private void scheduleNatureCheck(IProject project) {
		WorkspaceJob job = new WorkspaceJob("Add ZX Image Conversion nature to " + project.getName()) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				checkAndAddNature(project);
				return Status.OK_STATUS;
			}
		};
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.setSystem(true);
		job.schedule();
	}

	private void checkAndAddNature(IProject project) {
		try {
			if (!project.isOpen() || !project.isAccessible()) {
				LOG.info("Project '" + project.getName() + "' is not open/accessible, skipping");
				return;
			}

			// Log all natures for debugging
			String[] natures = project.getDescription().getNatureIds();
			LOG.info("Project '" + project.getName() + "' has natures: " + Arrays.toString(natures));

			if (project.hasNature(ZXImageConversionNature.NATURE_ID)) {
				LOG.info("Project '" + project.getName() + "' already has ZX Image Conversion nature");
				return;
			}

			boolean hasTrigger = false;
			for (String natureId : AUTO_TRIGGER_NATURES) {
				// Check both via hasNature() and by scanning the nature IDs directly
				boolean hasIt = false;
				try {
					hasIt = project.hasNature(natureId);
				} catch (CoreException e) {
					// Nature definition not installed — check raw IDs
				}
				if (!hasIt) {
					// Fallback: check the raw nature ID list
					for (String n : natures) {
						if (n.equals(natureId)) {
							hasIt = true;
							break;
						}
					}
				}
				if (hasIt) {
					LOG.info("Project '" + project.getName() + "' has trigger nature: " + natureId);
					hasTrigger = true;
					break;
				}
			}
			if (hasTrigger) {
				LOG.info("Adding ZX Image Conversion nature to '" + project.getName() + "'");
				NatureUtil.addNature(project);
			} else {
				LOG.info("Project '" + project.getName() + "' has no trigger natures, skipping");
			}
		} catch (CoreException e) {
			LOG.warn("Failed to auto-add ZX Image Conversion nature to " + project.getName() + ": " + e.getMessage(), e);
		}
	}
}
