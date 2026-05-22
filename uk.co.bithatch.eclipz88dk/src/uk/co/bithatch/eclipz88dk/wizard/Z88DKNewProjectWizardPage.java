package uk.co.bithatch.eclipz88dk.wizard;

import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.eclipz88dk.Z88DKLanguageSystemProvider.Z88DKArchitecture;
import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.eclipz88dk.toolchain.Z88DKSDK;

public class Z88DKNewProjectWizardPage extends AbstractZ88DKProjectWizardPage {

	private Combo sdk;
	private Combo arch;
	private Combo cLibrary;
	private List<Z88DKSDK> allSDKs;
	private Button overridePreferences;
	private Label sdkLabel;
	private Label archLabel;
	private Label libsLabel;

	public Z88DKNewProjectWizardPage() {
		super("Z8DK New Project");
		setTitle("Z88DK New Project");
		setDescription(
				"Create a new Z88DK C project for the ZX Spectrum or ZX Spectrum Next");
    }

	public IArchitecture getArchitecture() {
		var sdk = getSDK();
		if(sdk == null) {
			return null;
		}
		else {
			var sel = arch.getSelectionIndex();
			var archs = Z88DKPreferencesAccess.get().getLanguageSystem().architectures(null,  sdk.name());
			return sel == -1 || sel >= archs.size() ? null : archs.get(sel);
		}
	}

	public Z88DKSDK getSDK() {
		var idx = this.sdk.getSelectionIndex();
		if(idx < 0) {
			return null;
		}
		return this.allSDKs.get(idx);
	}
	
	public boolean isOverridePreferences() {
		return overridePreferences.getSelection();
	}
	
	public String getCLibrary() {
			var sel = cLibrary.getSelectionIndex();
			return sel == -1 ? null : cLibrary.getItems()[sel];
	}
	
	@Override
	protected void createFields(Composite container) {

        overridePreferences = new Button(container, SWT.CHECK);
        overridePreferences.setText("I want to choose my own basic project setup");
        overridePreferences.setToolTipText(
        		"When deselected, the workspace global preferences will be used for this project.");
        overridePreferences.addSelectionListener(widgetSelectedAdapter(e -> {
        	if(!overridePreferences.getSelection()) {
        		setSdkDefaults();
        	}
        	updateState(); 
        }));
        fillDefaults().grab(true, false).span(3, 1).indent(0, 24).applyTo(overridePreferences);

        sdkLabel = new Label(container, SWT.NONE);
        sdkLabel.setText("SDK:");

        sdk = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        fillDefaults().grab(true, false).span(2, 1).applyTo(sdk);
        sdk.addSelectionListener(widgetSelectedAdapter(e -> {
        	rebuildArchs();
			setArchFromSDKSelectionAndPreferences();
			rebuildCLibs();
			setCLibraryFromSDKSelectionAndPreferences();
        }));
        allSDKs = Z88DKPreferencesAccess.get().getAllSDKs();
		sdk.setItems(allSDKs.stream().map(s -> s.name()).toList().toArray(new String[0]));
        
        archLabel = new Label(container, SWT.NONE);
        archLabel.setText("System:");
        
        arch= new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        fillDefaults().grab(true, false).span(2, 1).applyTo(arch);
		arch.addSelectionListener(widgetSelectedAdapter(e -> {
			rebuildCLibs();
			setCLibraryFromSDKSelectionAndPreferences();
		}));

        libsLabel = new Label(container, SWT.NONE);
        libsLabel.setText("C Library");
        fillDefaults().grab(true, false).indent(0, 16).applyTo(libsLabel);

        cLibrary = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        fillDefaults().grab(true, false).span(2, 1).applyTo(cLibrary);
        
        setSdkDefaults();
        rebuildArchs();
        setArchFromSDKSelectionAndPreferences();
        rebuildCLibs();
        setCLibraryFromSDKSelectionAndPreferences();
        updateState();
	}

	protected void rebuildArchs() {
		var sdk = getSDK();
		if (sdk == null) {
			arch.setItems(new String[0]);
		} else {
			arch.setItems(Z88DKPreferencesAccess.get().getArchitectures(null).stream().map(s -> s.name()).toList()
					.toArray(new String[0]));
		}
		
	}

	protected void rebuildCLibs() {
		var arch = getArchitecture();
		if(arch == null) {
			cLibrary.setItems(new String[0]);
		}
		else {
			cLibrary.setItems(((Z88DKArchitecture)arch).configuration().cLibraries().toArray(String[]::new));
		}
	}
	
	protected void setArchFromSDKSelectionAndPreferences() {
		var sdk = getSDK();
		if(sdk != null) {
			var prefArch = Z88DKPreferencesAccess.get().getArchitecture(null);
			var archs = Z88DKPreferencesAccess.get().getArchitectures(null);
			var index = archs.indexOf(prefArch);
			if(index == -1 && archs.size() > 0) {
				index = 0;
			}
			if(index == -1) {
				arch.clearSelection();
			}
			else {
				arch.select(index);
			}
		}
		else {
			arch.clearSelection();
		}
	}
	
	protected void setCLibraryFromSDKSelectionAndPreferences() {
		var arch = getArchitecture();
		if(arch != null) {
			var prefCLib = Z88DKPreferencesAccess.get().getCLibrary(null);
			cLibrary.select(((Z88DKArchitecture)arch).configuration().cLibraries().indexOf(prefCLib));
		}
		else {
			cLibrary.clearSelection();
		}
	}
	
	protected void setSdkDefaults() {
		if(sdk.getItemCount() == 0) {
        	setErrorMessage("There are no Z88DK SDKs installed");
        	arch.clearSelection();
        	cLibrary.clearSelection();
        }
        else {
        	var prefSdk = Z88DKPreferencesAccess.get().getSDK(null).orElse(null);
        	var sdkIdx = Arrays.asList(sdk.getItems()).indexOf(prefSdk.name());
        	if(sdkIdx < 0) {
				sdk.clearSelection();
			}
        	else {
        		sdk.select(sdkIdx);
        	}
        }
	}
	
	protected void updateState() {
		var sel = isOverridePreferences();
		sdkLabel.setEnabled(sel);
		sdk.setEnabled(sel);
		archLabel.setEnabled(sel);
		arch.setEnabled(sel);
		libsLabel.setEnabled(sel);
		cLibrary.setEnabled(sel);
	}
	
}
