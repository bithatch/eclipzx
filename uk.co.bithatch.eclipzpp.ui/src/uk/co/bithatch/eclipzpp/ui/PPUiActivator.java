package uk.co.bithatch.eclipzpp.ui;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class PPUiActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "uk.co.bithatch.eclipzpp.ui"; //$NON-NLS-1$

    public static final String COMMENT_PATH = "icons/comment16.png";
    public static final String LABEL_PATH = "icons/label16.png";
    public static final String VAR_PATH = "icons/var16.png";
    public static final String CONST_PATH = "icons/const16.png";
    public static final String INCLUDE_PATH = "icons/include16.png";
    public static final String PROGRAM_PATH = "icons/program16.png";
    public static final String LOCAL_PATH = "icons/local16.png";
    public static final String DATA_PATH = "icons/data16.png";
    public static final String PP_PATH = "icons/pp16.png";
    public static final String DEFINE_PATH = "icons/define16.png";;

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(COMMENT_PATH, imageDescriptorFromPlugin(PLUGIN_ID, COMMENT_PATH));
		reg.put(LABEL_PATH, imageDescriptorFromPlugin(PLUGIN_ID, LABEL_PATH));
		reg.put(VAR_PATH, imageDescriptorFromPlugin(PLUGIN_ID, VAR_PATH));
		reg.put(CONST_PATH, imageDescriptorFromPlugin(PLUGIN_ID, CONST_PATH));
		reg.put(INCLUDE_PATH, imageDescriptorFromPlugin(PLUGIN_ID, INCLUDE_PATH));
		reg.put(PROGRAM_PATH, imageDescriptorFromPlugin(PLUGIN_ID, PROGRAM_PATH));
		reg.put(LOCAL_PATH, imageDescriptorFromPlugin(PLUGIN_ID, LOCAL_PATH));
		reg.put(DATA_PATH, imageDescriptorFromPlugin(PLUGIN_ID, DATA_PATH));
		reg.put(PP_PATH, imageDescriptorFromPlugin(PLUGIN_ID, PP_PATH));
		reg.put(DEFINE_PATH, imageDescriptorFromPlugin(PLUGIN_ID, DEFINE_PATH));
	}

	// The shared instance
	private static PPUiActivator plugin;
	
	/**
	 * The constructor
	 */
	public PPUiActivator() {
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
	public static PPUiActivator getDefault() {
		return plugin;
	}

}
