package uk.co.bithatch.ayzxfx;

import javax.swing.UIManager;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


public class Activator  extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "uk.co.bithatch.ayzxfx"; //$NON-NLS-1$

	public static final String EFFECT = "icons/afx16.png";
	public static final String EFFECT_BANK = "icons/afb16.png";

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}
	
	public static Image image(String key) {
		return getDefault().getImageRegistry().get(key);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e) { 
		}
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
		reg.put(EFFECT, imageDescriptorFromPlugin(PLUGIN_ID, EFFECT));
		reg.put(EFFECT_BANK, imageDescriptorFromPlugin(PLUGIN_ID, EFFECT_BANK));
	}
}
