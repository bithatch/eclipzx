package uk.co.bithatch.eclipz80.ui;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

import uk.co.bithatch.eclipz80.ui.internal.Eclipz80Activator;

public class AsmUiActivator extends Eclipz80Activator {
	
    public static final String ASM_PATH = "icons/asm16.png";
    public static final String ORG_PATH = "icons/org16.png";
    public static final String INCBIN_PATH = "icons/incbin16.png";
    
	public static AsmUiActivator getInstance() {
		return (AsmUiActivator)Eclipz80Activator.getInstance();
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(new AsmResourceListener());
        super.start(context);
    }

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(ASM_PATH, checkDescriptor(imageDescriptorFromPlugin(PLUGIN_ID, ASM_PATH)));
		reg.put(ORG_PATH, checkDescriptor(imageDescriptorFromPlugin(PLUGIN_ID, ORG_PATH)));
		reg.put(INCBIN_PATH, checkDescriptor(imageDescriptorFromPlugin(PLUGIN_ID, INCBIN_PATH)));
	}

	private ImageDescriptor checkDescriptor(ImageDescriptor d) {
		if(d == null)
			throw new IllegalArgumentException("Image is missing.");
		return d;
	}

}