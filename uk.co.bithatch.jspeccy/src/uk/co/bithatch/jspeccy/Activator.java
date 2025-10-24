package uk.co.bithatch.jspeccy;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "uk.co.bithatch.jspeccy"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	

    public static final String PLAY_PATH = "icons/play16.png";
    public static final String PAUSE_PATH = "icons/pause16.png";
    public static final String FAST_PATH = "icons/fwd16.png";
    public static final String UNMUTED_PATH = "icons/unmuted16.png";
    public static final String MUTED_PATH = "icons/muted16.png";
    public static final String RESET_PATH = "icons/softreset16.png";
    public static final String HARD_RESET_PATH = "icons/hardreset16.png";
    public static final String KEYBOARD_PATH = "icons/keyboard48k.png";
	public static final String ROM_PATH = "icons/rom16.png";
	public static final String SNAPSHOT_PATH = "icons/snapshot16.png";
	public static final String TAPE_PATH = "icons/tape16.png";
	public static final String MICRODRIVE_PATH = "icons/microdrive16.png";
	public static final String POKE_PATH = "icons/poke16.png";
    
	private ZXEmulatorSettings settings;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		settings = new ZXEmulatorSettings();
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(PLAY_PATH, imageDescriptorFromPlugin(PLUGIN_ID, PLAY_PATH));
		reg.put(PAUSE_PATH, imageDescriptorFromPlugin(PLUGIN_ID, PAUSE_PATH));
		reg.put(FAST_PATH, imageDescriptorFromPlugin(PLUGIN_ID, FAST_PATH));
		reg.put(MUTED_PATH, imageDescriptorFromPlugin(PLUGIN_ID, MUTED_PATH));
		reg.put(UNMUTED_PATH, imageDescriptorFromPlugin(PLUGIN_ID, UNMUTED_PATH));
		reg.put(RESET_PATH, imageDescriptorFromPlugin(PLUGIN_ID, RESET_PATH));
		reg.put(HARD_RESET_PATH, imageDescriptorFromPlugin(PLUGIN_ID, HARD_RESET_PATH));
		reg.put(KEYBOARD_PATH, imageDescriptorFromPlugin(PLUGIN_ID, KEYBOARD_PATH));
		reg.put(SNAPSHOT_PATH, imageDescriptorFromPlugin(PLUGIN_ID, SNAPSHOT_PATH));
		reg.put(TAPE_PATH, imageDescriptorFromPlugin(PLUGIN_ID, TAPE_PATH));
		reg.put(ROM_PATH, imageDescriptorFromPlugin(PLUGIN_ID, ROM_PATH));
		reg.put(MICRODRIVE_PATH, imageDescriptorFromPlugin(PLUGIN_ID, MICRODRIVE_PATH));
		reg.put(POKE_PATH, imageDescriptorFromPlugin(PLUGIN_ID, POKE_PATH));
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public ZXEmulatorSettings settings() {
		return settings;
	}

}
