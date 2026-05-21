package uk.co.bithatch.fatexplorer;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "uk.co.bithatch.fatexplorer"; //$NON-NLS-1$

	public static final String DISK_IMAGE_PATH = "icons/diskImage16.png";
	public static final String NO_ENTRY_PATH = "icons/noentry16.png";

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
		FATDiskImageManager.installResourceListener();
		// Defer automounting to a background job so the workspace is fully ready
		Job.create("Mounting FAT disk images", monitor -> {
			FATDiskImageManager.mountAllAutomounts();
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}).schedule(1000);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		FATDiskImageManager.uninstallResourceListener();
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

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(DISK_IMAGE_PATH, imageDescriptorFromPlugin(PLUGIN_ID, DISK_IMAGE_PATH));
		reg.put(NO_ENTRY_PATH, imageDescriptorFromPlugin(PLUGIN_ID, NO_ENTRY_PATH));
	}

}
