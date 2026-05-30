package uk.co.bithatch.eclipz80.ui.preferences;

import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;
import uk.co.bithatch.widgetzx.preferences.DefinesTableFieldEditor;

/**
 * Preference / property page for managing preprocessor defines.
 * Follows the same project-specific / workspace pattern as
 * {@link AsmCompilerPreferencePage}.
 */
public class AsmDefinesPreferencePage extends AbstractProjectSpecificPreferencePage {

	public AsmDefinesPreferencePage() {
		super(AsmPreferencesAccess.get(), AsmPreferenceConstants.COMPILER, GRID);
	}

	@Override
	protected void createFieldEditors() {
		super.createFieldEditors();

		addField(new DefinesTableFieldEditor(
				AsmPreferenceConstants.DEFINES,
				"Preprocessor Defines:",
				getFieldEditorParent()));
	}
}
