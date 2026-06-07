package uk.co.bithatch.eclipz80.ui.preferences;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;
import uk.co.bithatch.widgetzx.preferences.LibraryFolderListEditor;

public class AsmIncludePathsPreferencePage extends AbstractProjectSpecificPreferencePage {
    

	public AsmIncludePathsPreferencePage() {
        super(AsmPreferencesAccess.get(), AsmPreferenceConstants.INCLUDE_PATHS, GRID);
        setDescription("Additional folder paths to search when locating INCLUDE resources in assemlby");
    }
	
	@Override
	protected void createFieldEditors() {
		super.createFieldEditors();
		
		addField(new LibraryFolderListEditor(AsmPreferenceConstants.INCLUDE_PATHS, "Include Folders:", getFieldEditorParent(), 
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
