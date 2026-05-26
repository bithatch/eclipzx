package uk.co.bithatch.eclipz80.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

import uk.co.bithatch.eclipz80.ui.internal.Eclipz80Activator;

public class AsmUiActivator extends Eclipz80Activator {
	
    public static final String ASM_PATH = "icons/asm16.png";
    
	public static AsmUiActivator getInstance() {
		return (AsmUiActivator)Eclipz80Activator.getInstance();
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		
//        EPackage.Registry.INSTANCE.put(AsmPackage.eNS_URI, AsmPackage.eINSTANCE);
//        ResourcesPlugin.getWorkspace().addResourceChangeListener(new ZXBasicResourceListener());
        
        /* TODO Find a better way. Triggers early preference initialization. */
//		ZXBasicPreferencesAccess.get().getPreferenceStore().getString(ZXBasicPreferenceConstants.ARCHITECTURE);
		
//		Platform.getAdapterManager().loadAdapter(new LibraryFileNode(null, new File("/tmp/foo.bas")), IStorage.class.getName());

		
        super.start(context);
    }

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(ASM_PATH, checkDescriptor(imageDescriptorFromPlugin(PLUGIN_ID, ASM_PATH)));
	}

	private ImageDescriptor checkDescriptor(ImageDescriptor d) {
		if(d == null)
			throw new IllegalArgumentException("Image is missing.");
		return d;
	}

}