package uk.co.bithatch.zxbasic.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;

import uk.co.bithatch.widgetzx.SpinnerFieldEditor;
import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;

public class CompliancePreferencePage extends AbstractProjectSpecificPreferencePage  {
    
	public CompliancePreferencePage() {
        super(ZXBasicPreferencesAccess.get(), ZXBasicPreferenceConstants.COMPLIANCE, GRID);
    }
	
	@Override
	protected void createFieldEditors() {
		super.createFieldEditors();

        addField(new SpinnerFieldEditor(
            ZXBasicPreferenceConstants.STRING_BASE,
            "String Base. The default lower index for strings",
            getFieldEditorParent(), 0, 65535, 1
        ));

        addField(new SpinnerFieldEditor(
            ZXBasicPreferenceConstants.ARRAY_BASE,
            "Array Base. The default lower index for arrays",
            getFieldEditorParent(), 0, 65535, 1
        ));

        addField(new BooleanFieldEditor(
            ZXBasicPreferenceConstants.STRICT,
            "Strict type declaration",
            getFieldEditorParent()
        ));

        addField(new BooleanFieldEditor(
            ZXBasicPreferenceConstants.STRICT_BOOLEAN,
            "Strict booleans (0 or 1)",
            getFieldEditorParent()
        ));

        addField(new BooleanFieldEditor(
            ZXBasicPreferenceConstants.IGNORE_VARIABLE_CASE,
            "Ignore variable case",
            getFieldEditorParent()
        ));

        addField(new BooleanFieldEditor(
            ZXBasicPreferenceConstants.LEGACY_INSTRUCTIONS,
            "Enable original ZX Spectrum instructions",
            getFieldEditorParent()
        ));
	}
	
}
