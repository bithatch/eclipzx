package uk.co.bithatch.zxbasic.borielsdk;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	public static final String STDLIB_PATH = "icons/stdlib16.png";
	public static final String RUNTIME_PATH = "icons/runtime16.png";

	// The plug-in ID
	public static final String PLUGIN_ID = "uk.co.bithatch.zxbasic.borielsdk"; //$NON-NLS-1$

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

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(STDLIB_PATH, imageDescriptorFromPlugin(PLUGIN_ID, STDLIB_PATH));
		reg.put(RUNTIME_PATH, imageDescriptorFromPlugin(PLUGIN_ID, RUNTIME_PATH));
	}

}
