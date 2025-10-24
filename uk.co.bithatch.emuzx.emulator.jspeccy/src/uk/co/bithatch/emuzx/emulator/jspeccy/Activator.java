package uk.co.bithatch.emuzx.emulator.jspeccy;

import java.util.Arrays;
import java.util.List;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "uk.co.bithatch.emuzx.emulator.jspeccy"; //$NON-NLS-1$

	public static final String JSPECCY_FORMATS = null;

	public static final List<String> JSPECCY_RUNNABLE_FORMATS = Arrays.asList("tap", "tzx", "sna", "z80");

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

}
