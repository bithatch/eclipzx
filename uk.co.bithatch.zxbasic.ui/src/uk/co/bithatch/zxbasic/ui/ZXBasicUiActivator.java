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

import uk.co.bithatch.zxbasic.asm.AsmPackage;
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
    public static final String CHIP_PATH = "icons/chip16.png";
    public static final String LAUNCH_PATH = "icons/launch16.png";
    public static final String PREPARE_PATH = "icons/prepare16.png";
    public static final String SUB_PATH = "icons/sub16.png";
    public static final String FUNCTION_PATH = "icons/function16.png";
    public static final String COMMENT_PATH = "icons/comment16.png";
    public static final String LABEL_PATH = "icons/label16.png";
    public static final String VAR_PATH = "icons/var16.png";
    public static final String CONST_PATH = "icons/const16.png";
    public static final String INCLUDE_PATH = "icons/include16.png";
    public static final String PROGRAM_PATH = "icons/program16.png";
    public static final String LOCAL_PATH = "icons/local16.png";
    public static final String DATA_PATH = "icons/data16.png";
    public static final String PP_PATH = "icons/pp16.png";
    public static final String DEFINE_PATH = "icons/define16.png";
    
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
		
        EPackage.Registry.INSTANCE.put(AsmPackage.eNS_URI, AsmPackage.eINSTANCE);
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
		reg.put(CHIP_PATH, imageDescriptorFromPlugin(PLUGIN_ID, CHIP_PATH));
		reg.put(LAUNCH_PATH, imageDescriptorFromPlugin(PLUGIN_ID, LAUNCH_PATH));
		reg.put(PREPARE_PATH, imageDescriptorFromPlugin(PLUGIN_ID, PREPARE_PATH));
		reg.put(SUB_PATH, imageDescriptorFromPlugin(PLUGIN_ID, SUB_PATH));
		reg.put(FUNCTION_PATH, imageDescriptorFromPlugin(PLUGIN_ID, FUNCTION_PATH));
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

	private ImageDescriptor checkDescriptor(ImageDescriptor d) {
		if(d == null)
			throw new IllegalArgumentException("Image is missing.");
		return d;
	}

	public static void BRK() {

    	System.out.println("FACT CON");    	System.out.println("FACT CON");
		
	}
}