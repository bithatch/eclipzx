package uk.co.bithatch.eclipzx.ui.glue;

import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_CLEAR_BEFORE_USE;

import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import uk.co.bithatch.fatexplorer.preferences.FATPreferencesAccess;
import uk.co.bithatch.zxbasic.ui.api.ILaunchPreparationUI;
import uk.co.bithatch.zxbasic.ui.api.IPreparationTargetUI;

public class ConfiguredFATPreparationTargetUI implements IPreparationTargetUI {

	public static final String FAT_IMAGE_PATH = PREPARATION + ".fatImagePath";
	public static final String FAT_FOLDER = PREPARATION + ".fatFolder";
	
	private Combo diskImage;
	private Text folder;
	private Button clearBeforeUse;
	private boolean available;
	private Button manage;
	private Label diskImageLabel;
	private Label folderLabel;
	private ControlDecoration clearBeforeUseDecoration;
	private Label info;

	@Override
	public void createControls(Composite parent, Runnable onUpdate) {
		
		var grid = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(3, false);
		layout.horizontalSpacing = 24;
		layout.verticalSpacing = 8;
		grid.setLayout(layout);
        
        info = new Label(grid, SWT.NONE);
        info.setFont(JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));
        info.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
        info.setText("The full image path is available as the ${fat_image} variable for use in emulator arguments.");

        diskImageLabel = new Label(grid, SWT.NONE);
        diskImageLabel.setLayoutData(GridDataFactory.swtDefaults().create());
        diskImageLabel.setText("Disk Image:");

        diskImage = new Combo(grid, SWT.DROP_DOWN | SWT.READ_ONLY);
        diskImage.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        rebuildItems();
        
        manage = new Button(grid, SWT.PUSH);
        manage.setText("Manage");
        manage.setLayoutData(GridDataFactory.fillDefaults().create());
        manage.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
        	var dialog = PreferencesUtil.createPreferenceDialogOn(
        		    parent.getShell(),
        		    "uk.co.bithatch.fatexplorer.preferences.diskImagesPreferences",
        		    null,
        		    null
        		);
			if (dialog != null) {
				dialog.open();
				rebuildItems();
			}
        }));
        
        folderLabel = new Label(grid, SWT.NONE);
        folderLabel.setLayoutData(GridDataFactory.swtDefaults().create());
        folderLabel.setText("Folder:");

        folder = new Text(grid, SWT.NONE);
        folder.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        folder.setToolTipText("The full path to folder on the disk image. Empty is the root folder. Use ${fat_default} for a default path based on the launcher project.");
        folder.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
	        	onUpdate.run();
			}
		});
        
        clearBeforeUse = new Button(grid, SWT.CHECK);
        clearBeforeUse.setText("Clear Before Use");
        clearBeforeUse.setLayoutData(GridDataFactory.swtDefaults().create());
        clearBeforeUse.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
        	onUpdate.run();
        }));
        
        clearBeforeUseDecoration = new ControlDecoration(clearBeforeUse, SWT.LEFT | SWT.TOP);
        clearBeforeUseDecoration.setImage(PlatformUI.getWorkbench().getSharedImages()
		        .getImage(ISharedImages.IMG_OBJS_WARN_TSK));
        clearBeforeUseDecoration.setDescriptionText("Are you sure you wish to clear the root folder on every launch?");
        clearBeforeUseDecoration.hide();
        
        updateState();
	}

	protected void rebuildItems() {
		diskImage.setItems(FATPreferencesAccess.getURIs().toArray(new String[0]));
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		var uri = config.getAttribute(FAT_IMAGE_PATH, "");
		var uris = FATPreferencesAccess.getURIs();
		var idx = uris.indexOf(uri);
		if(uris.size() > 0)
			diskImage.select(idx == -1 ? 0 : idx);

		folder.setText(config.getAttribute(FAT_FOLDER, "${fat_default}"));

        clearBeforeUse.setSelection(config.getAttribute(PREPARATION_CLEAR_BEFORE_USE, false));
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(FAT_IMAGE_PATH, diskImage.getItem(diskImage.getSelectionIndex()));
		config.setAttribute(FAT_FOLDER, folder.getText());
		config.setAttribute(PREPARATION_CLEAR_BEFORE_USE, clearBeforeUse.getSelection());
		
	}

	@Override
	public boolean isValid(ILaunchConfiguration config, Consumer<String> messageAcceptor) {
		return !available || diskImage.getSelectionIndex() != -1;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		if(diskImage.getItemCount() > 0)
			diskImage.select(0);
		folder.setText("${fat_default}");
		clearBeforeUse.setSelection(false);
	}

	@Override
	public void setAvailable(boolean available) {
		this.available = available;
		updateState();		
	}

	@Override
	public void init(ILaunchPreparationUI prepUi) {
	}
	
	private void updateState() {
		diskImage.setEnabled(available);
		manage.setEnabled(available);
		diskImageLabel.setEnabled(available);
		clearBeforeUse.setEnabled(available);
		folder.setEnabled(available);
		info.setEnabled(available);
		
		var folderText = folder.getText();
		if(clearBeforeUse.getSelection() && (folderText.equals("") || folderText.equals("/") || folderText.equals("\\"))) {
			clearBeforeUseDecoration.show();	
		}
		else {
			clearBeforeUseDecoration.hide();	
		}
	}

}
