package uk.co.bithatch.drawzx;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import uk.co.bithatch.drawzx.widgets.ColorCache;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "uk.co.bithatch.drawzx"; //$NON-NLS-1$

	public static final String ZOOM_IN = "icons/zoomIn16.png";
	public static final String ZOOM_OUT = "icons/zoomOut16.png";
	public static final String PAL = "icons/pal16.png";
	public static final String PAL_TRANS = "icons/palTrans16.png";

	// The shared instance
	private static Activator plugin;
	
	private ColorCache colorCache;
	
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
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		colorCache.dispose();
		super.stop(context);
	}

	public ColorCache getColorCache() {
		synchronized(this) {
			if(colorCache == null) {
				colorCache = new ColorCache(PlatformUI.getWorkbench().getDisplay());	
			}
			return colorCache;
		}
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
		reg.put(ZOOM_IN, imageDescriptorFromPlugin(PLUGIN_ID, ZOOM_IN));
		reg.put(ZOOM_OUT, imageDescriptorFromPlugin(PLUGIN_ID, ZOOM_OUT));
		reg.put(PAL, imageDescriptorFromPlugin(PLUGIN_ID, PAL));
		reg.put(PAL_TRANS, imageDescriptorFromPlugin(PLUGIN_ID, PAL_TRANS));
	}
}
