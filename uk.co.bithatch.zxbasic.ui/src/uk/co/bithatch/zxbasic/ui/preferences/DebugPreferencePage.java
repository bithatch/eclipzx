package uk.co.bithatch.zxbasic.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;

import uk.co.bithatch.widgetzx.SpinnerFieldEditor;
import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;

public class DebugPreferencePage extends AbstractProjectSpecificPreferencePage  {
    
	public DebugPreferencePage() {
        super(ZXBasicPreferencesAccess.get(), ZXBasicPreferenceConstants.DEBUG , GRID);
    }
	
	@Override
	protected void createFieldEditors() {
		super.createFieldEditors();

        addField(new SpinnerFieldEditor(
            ZXBasicPreferenceConstants.DEBUG_LEVEL,
            "Debug verbosity level",
            getFieldEditorParent(), 0, 9, 1
        ));

        addField(new BooleanFieldEditor(
            ZXBasicPreferenceConstants.DEBUG_ARRAYS,
            "Debug arrays",
            getFieldEditorParent()
        ));

        addField(new BooleanFieldEditor(
            ZXBasicPreferenceConstants.DEBUG_MEMORY,
            "Debug out-of-memory errors",
            getFieldEditorParent()
        ));
	}
	
}
