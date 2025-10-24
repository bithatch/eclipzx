package uk.co.bithatch.zxbasic.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;

import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;
import uk.co.bithatch.zxbasic.preprocessor.Warning;


public class ErrorsAndWarningsPreferencePage extends AbstractProjectSpecificPreferencePage {
	
	public ErrorsAndWarningsPreferencePage() {
        super(ZXBasicPreferencesAccess.get(), ZXBasicPreferenceConstants.ERRORS_AND_WARNINGS , GRID);
        setDescription("Set which warnings should be suppressed by the compiler.");
	}
	

	@Override
	protected void createFieldEditors() {
		super.createFieldEditors();
		
		for(var wrn : Warning.values()) { 
	        addField(new BooleanFieldEditor(
	        	ZXBasicPreferenceConstants.ERRORS_AND_WARNINGS + "." + wrn.name(),
	            wrn.description() + " (W" + wrn.code() + ")",
	            getFieldEditorParent()
	        ));
		}
	}
	
}
