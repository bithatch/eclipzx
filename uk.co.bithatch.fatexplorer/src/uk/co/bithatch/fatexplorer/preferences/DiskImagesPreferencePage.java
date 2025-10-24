package uk.co.bithatch.fatexplorer.preferences;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class DiskImagesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    

	private IWorkbench workbench;

	public DiskImagesPreferencePage() {
        super(FLAT);
        setDescription("""
        		
        		Select this set of FAT32 or FAT16 disk images you wish to be able
        		to browse and copy files to and from your projects.
        		
        		Note, this is experimental and MAY MESS UP YOUR DISK IMAGES. Use
        		with care and always keep backups.
        		""");
    }
	
	@Override
	protected void createFieldEditors() {
		setPreferenceStore(FATPreferencesAccess.getPreferenceStore());
		addField(new DiskImageListEditor(PreferenceConstants.DISK_IMAGES, "Disk Images:", getFieldEditorParent(), 
				getWorkbench().getAdapter(IWorkspace.class).getRoot()) {

			@Override
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
				GridData gd = (GridData)getList().getLayoutData();
				gd.minimumHeight = 260;
			}

		});

	}

	@Override
	public void init(IWorkbench workbench) {
		this.workbench = workbench;
	}
	
	public IWorkbench getWorkbench() {
		if(workbench == null)
			return PlatformUI.getWorkbench();
		else
			return workbench;
	}
}
