package uk.co.bithatch.zxbasic.ui.preferences;

import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;

public class PreprocessorPreferencePage extends AbstractProjectSpecificPreferencePage {
    

	public PreprocessorPreferencePage() {
        super(ZXBasicPreferencesAccess.get(), ZXBasicPreferenceConstants.PREPROCESSOR, GRID);
        setDescription("Defines a given macro for the pre-processor. A macro value may be blank. ");
    }

	@Override
	protected void createFieldEditors() { 
		super.createFieldEditors();
		
		addField(new DefinesTableFieldEditor(
            ZXBasicPreferenceConstants.DEFINES,
            "",
            getFieldEditorParent()
        ));
	}
}
