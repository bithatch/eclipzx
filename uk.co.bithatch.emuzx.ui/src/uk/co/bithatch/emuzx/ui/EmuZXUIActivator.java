package uk.co.bithatch.emuzx.ui;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class EmuZXUIActivator extends AbstractUIPlugin {
    public static final String CHIP_PATH = "icons/chip16.png";
    public static final String CONFIG_FILE_PATH = "icons/config16.png";
    public static final String PREPARE_PATH = "icons/prepare16.png";

	// The plug-in ID
	public static final String PLUGIN_ID = "uk.co.bithatch.emuzx.ui"; //$NON-NLS-1$

	// The shared instance
	private static EmuZXUIActivator plugin;
	
	/**
	 * The constructor
	 */
	public EmuZXUIActivator() {
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
	public static EmuZXUIActivator getDefault() {
		return plugin;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(PREPARE_PATH, imageDescriptorFromPlugin(PLUGIN_ID, PREPARE_PATH));
		reg.put(CHIP_PATH, imageDescriptorFromPlugin(PLUGIN_ID, CHIP_PATH));
		reg.put(CONFIG_FILE_PATH, imageDescriptorFromPlugin(PLUGIN_ID, CONFIG_FILE_PATH));
	}
}
