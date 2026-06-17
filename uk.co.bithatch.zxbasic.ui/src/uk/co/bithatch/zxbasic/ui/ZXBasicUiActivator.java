package uk.co.bithatch.zxbasic.ui;

import java.io.File;

import javax.swing.UIManager;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

import uk.co.bithatch.zxbasic.basic.BasicPackage;
import uk.co.bithatch.zxbasic.ui.internal.ZxbasicActivator;
import uk.co.bithatch.zxbasic.ui.navigator.LibraryFileNode;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferenceConstants;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class ZXBasicUiActivator extends ZxbasicActivator {
	public static final String PLUGIN_ID = "uk.co.bithatch.zxbasic.ui";
	
    public static final String LIBRARY_PATH = "icons/library16.png";
    public static final String LIBRARY_DECORATOR_PATH = "icons/library8.png";
    public static final String REF_PATH = "icons/ref16.png";
    public static final String LAUNCH_PATH = "icons/launch16.png";
    public static final String SUB_PATH = "icons/sub16.png";
    public static final String FUNCTION_PATH = "icons/function16.png";
    
	public static ZXBasicUiActivator getInstance() {
		return (ZXBasicUiActivator) ZxbasicActivator.getInstance();
	}
	
	public String getDefaultEmulatorPath() {
		// TODO
		return "fuse";
	}

	@Override
	public void start(BundleContext context) throws Exception {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e) { 
		}
		
        EPackage.Registry.INSTANCE.put(BasicPackage.eNS_URI, BasicPackage.eINSTANCE);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(new ZXBasicResourceListener());
        
        /* TODO Find a better way. Triggers early preference initialization. */
		ZXBasicPreferencesAccess.get().getPreferenceStore().getString(ZXBasicPreferenceConstants.ARCHITECTURE);
		
		Platform.getAdapterManager().loadAdapter(new LibraryFileNode(null, new File("/tmp/foo.bas")), IStorage.class.getName());

		
        super.start(context);
    }

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(LIBRARY_PATH, checkDescriptor(imageDescriptorFromPlugin(PLUGIN_ID, LIBRARY_PATH)));
		reg.put(LIBRARY_DECORATOR_PATH, imageDescriptorFromPlugin(PLUGIN_ID, LIBRARY_DECORATOR_PATH));
		reg.put(REF_PATH, imageDescriptorFromPlugin(PLUGIN_ID, REF_PATH));
		reg.put(LAUNCH_PATH, imageDescriptorFromPlugin(PLUGIN_ID, LAUNCH_PATH));
		reg.put(SUB_PATH, imageDescriptorFromPlugin(PLUGIN_ID, SUB_PATH));
		reg.put(FUNCTION_PATH, imageDescriptorFromPlugin(PLUGIN_ID, FUNCTION_PATH));
	}

	private ImageDescriptor checkDescriptor(ImageDescriptor d) {
		if(d == null)
			throw new IllegalArgumentException("Image is missing.");
		return d;
	}

	public static void BRK() {

    	System.out.println("FACT CON");    	System.out.println("FACT CON");
		
	}
}