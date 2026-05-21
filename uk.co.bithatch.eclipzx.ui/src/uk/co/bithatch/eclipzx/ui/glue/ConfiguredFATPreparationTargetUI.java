package uk.co.bithatch.eclipzx.ui.glue;

import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import uk.co.bithatch.emuzx.ui.ILaunchPreparationUI;
import uk.co.bithatch.emuzx.ui.IPreparationTargetUI;
import uk.co.bithatch.fatexplorer.FATDiskImageManager;
import uk.co.bithatch.fatexplorer.FATDiskImageMount;

public class ConfiguredFATPreparationTargetUI implements IPreparationTargetUI {

	public static final String FAT_IMAGE_PATH = PREPARATION + ".fatImagePath";
	
	private Combo diskImage;
	private boolean available;
	private Label diskImageLabel;
	private Label info;
	private ILaunchPreparationUI prepUi;
	private List<String> currentImagePaths = new ArrayList<>();

	@Override
	public void createControls(Composite parent, Runnable onUpdate) {
		
		var grid = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(2, false);
		layout.horizontalSpacing = 24;
		layout.verticalSpacing = 8;
		grid.setLayout(layout);
        
        info = new Label(grid, SWT.NONE);
        info.setFont(JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));
        info.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
        info.setText("The full image path is available as the ${fat_image} variable for use in emulator arguments.\nManage disk images in the project properties under \"FAT Disk Images\".");

        diskImageLabel = new Label(grid, SWT.NONE);
        diskImageLabel.setLayoutData(GridDataFactory.swtDefaults().create());
        diskImageLabel.setText("Disk Image:");

        diskImage = new Combo(grid, SWT.DROP_DOWN | SWT.READ_ONLY);
        diskImage.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        rebuildItems();
        
        updateState();
	}

	protected void rebuildItems() {
		currentImagePaths.clear();
		if (prepUi != null) {
			var project = prepUi.resolveProject();
			if (project != null && project.isOpen()) {
				var mounts = FATDiskImageManager.getMounts(project);
				for (var m : mounts) {
					currentImagePaths.add(m.getImagePath());
				}
			}
		}
		diskImage.setItems(currentImagePaths.toArray(new String[0]));
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		rebuildItems();
		var path = config.getAttribute(FAT_IMAGE_PATH, "");
		var idx = currentImagePaths.indexOf(path);
		if(currentImagePaths.size() > 0)
			diskImage.select(idx == -1 ? 0 : idx);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		if (diskImage.getSelectionIndex() >= 0)
			config.setAttribute(FAT_IMAGE_PATH, diskImage.getItem(diskImage.getSelectionIndex()));
	}

	@Override
	public boolean isValid(ILaunchConfiguration config, Consumer<String> messageAcceptor) {
		return !available || diskImage.getSelectionIndex() != -1;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		if(diskImage.getItemCount() > 0)
			diskImage.select(0);
	}

	@Override
	public void setAvailable(boolean available) {
		this.available = available;
		updateState();		
	}

	@Override
	public void init(ILaunchPreparationUI prepUi) {
		this.prepUi = prepUi;
		rebuildItems();
	}
	
	private void updateState() {
		diskImage.setEnabled(available);
		diskImageLabel.setEnabled(available);
		info.setEnabled(available);
	}

}
