package uk.co.bithatch.zxbasic.ui.wizard;

import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.widgetzx.LanguageSystemUI;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicArchitecture;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicLanguageSystemProvider;
import uk.co.bithatch.zxbasic.ui.library.ContributedLibraryRegistry;
import uk.co.bithatch.zxbasic.ui.library.ContributedSDKRegistry;
import uk.co.bithatch.zxbasic.ui.library.ZXLibrary;
import uk.co.bithatch.zxbasic.ui.library.ZXSDK;
import uk.co.bithatch.zxbasic.ui.preferences.LibraryLabelProvider;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class BasicProjectWizardPage extends AbstractBasicProjectWizardPage {

	private CheckboxTableViewer viewer;
	private Combo sdk;
	private Combo arch;
	private List<ZXSDK> allSDKs;
	private Button overridePreferences;
	private Label sdkLabel;
	private Label archLabel;
	private Label libsLabel;

	public BasicProjectWizardPage() {
        super("ZX BASIC Project");
        setTitle("ZX BASIC Project");
        setDescription("Create a new ZX BASIC project.");
    }

	@Override
	protected void createFields(Composite container) {

        overridePreferences = new Button(container, SWT.CHECK);
        overridePreferences.setText("I want to choose my own basic project setup");
        overridePreferences.setToolTipText(
        		"When deselected, the workspace global preferences will be used for this project.");
        overridePreferences.addSelectionListener(widgetSelectedAdapter(e -> {
        	
        	if(!overridePreferences.getSelection()) {
        		setSdkAndArchAsDefafults();
        	}
        	updateState(); 
        }));
        fillDefaults().grab(true, false).span(2, 1).indent(0, 24).applyTo(overridePreferences);

        sdkLabel = new Label(container, SWT.NONE);
        sdkLabel.setText("SDK:");

        sdk = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        fillDefaults().grab(true, false).applyTo(sdk);
        sdk.addSelectionListener(widgetSelectedAdapter(e -> rebuildItems()));
        allSDKs = ContributedSDKRegistry.getAllSDKs();
		sdk.setItems(allSDKs.stream().map(s -> s.name()).toList().toArray(new String[0]));
        
        archLabel = new Label(container, SWT.NONE);
        archLabel.setText("Architecture:");
        
        arch= new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        fillDefaults().grab(true, false).applyTo(arch);
        arch.addSelectionListener(widgetSelectedAdapter(e -> rebuildItems()));
		arch.setItems(LanguageSystemUI.describedNames(LanguageSystem.languageSystem(BorielZXBasicLanguageSystemProvider.class).architectures(null)));
       	arch.select(0);

        setSdkAndArchAsDefafults();

        libsLabel = new Label(container, SWT.NONE);
        libsLabel.setText("Optional Libraries");
        fillDefaults().grab(true, false).indent(0, 16).applyTo(libsLabel);
        
       	viewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new LibraryLabelProvider());
		viewer.setInput(ContributedLibraryRegistry.getContributedLibraries());
        
        fillDefaults().grab(true, false).span(2, 1).hint(200, 128).applyTo(viewer.getControl());
        
        rebuildItems();
        updateState();
	}

	protected void setSdkAndArchAsDefafults() {
		if(sdk.getItemCount() == 0) {
        	setErrorMessage("There are no Boriel Basic SDKs installed");
        }
        else {
        	var prefSdk = ZXBasicPreferencesAccess.get().getSDK(null);
        	sdk.select(Arrays.asList(sdk.getItems()).indexOf(prefSdk.name()));
        }

    	var prefArch = ZXBasicPreferencesAccess.get().getArchitecture(null);
    	sdk.select(Arrays.asList(sdk.getItems()).indexOf(prefArch.description()));
	}
	
	public boolean isOverridePreferences() {
		return overridePreferences.getSelection();
	}
	
	public ZXSDK getSDK() {
		return this.allSDKs.get(this.sdk.getSelectionIndex());
	}
	
	public BorielZXBasicArchitecture getArchitecture() {
		return BorielZXBasicArchitecture.values()[this.arch.getSelectionIndex()];
	}
	
	protected void updateState() {
		var sel = isOverridePreferences();
		sdkLabel.setEnabled(sel);
		sdk.setEnabled(sel);
		archLabel.setEnabled(sel);
		arch.setEnabled(sel);
		libsLabel.setEnabled(sel);
		rebuildItems();
	}
	
	protected void rebuildItems() {
		if(isOverridePreferences())
			viewer.setInput(ContributedLibraryRegistry.getContributedLibraries(getArchitecture()));
		else
			viewer.setInput(Collections.emptyList());
	}


	public List<ZXLibrary> getLibraries() {
        return Arrays.stream(viewer.getCheckedElements())
            .map(obj -> ((ZXLibrary) obj))
            .toList();
	}
}
