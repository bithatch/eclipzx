package uk.co.bithatch.zxbasic.ui.preferences;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;

public class UserLibrariesPreferencePage extends AbstractProjectSpecificPreferencePage {
    

	public UserLibrariesPreferencePage() {
        super(ZXBasicPreferencesAccess.get(), ZXBasicPreferenceConstants.USER_LIBRARIES, GRID);
        setDescription("User libraries can exist anywhere in your project or on your file system. "
        		+ "These are added to Contributed Libraries to make up the final set of libraries "
        		+ "available to your project. ");
    }
	
	@Override
	protected void createFieldEditors() {
		super.createFieldEditors();
		
		addField(new LibraryFolderListEditor(ZXBasicPreferenceConstants.LIB_PATHS, "User Library Folders:", getFieldEditorParent(), 
				getWorkbench().getAdapter(IWorkspace.class).getRoot()) {

			@Override
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
		        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, numColumns, 1);
				gd.minimumHeight = 260;
			}

		});

	}
}
