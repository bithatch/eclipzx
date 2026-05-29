package uk.co.bithatch.eclipz80.ui.preferences;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;

/**
 * A {@link ListEditor} that lets the user add and remove preprocessor defines.
 * Each entry is stored as either {@code NAME} (flag-style define) or
 * {@code NAME=VALUE}.
 * <p>
 * The list is persisted as a single preference string with entries separated by
 * {@link AsmPreferenceConstants#DEFINES_SEPARATOR}.
 */
public class DefinesFieldEditor extends ListEditor {

	public DefinesFieldEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
	}

	@Override
	protected String getNewInputObject() {
		InputDialog dlg = new InputDialog(
				getShell(),
				"New Define",
				"Enter a define (NAME or NAME=VALUE):",
				"",
				new IInputValidator() {
					@Override
					public String isValid(String newText) {
						if (newText == null || newText.isBlank()) {
							return "Define must not be empty";
						}
						String name = newText.contains("=") ? newText.substring(0, newText.indexOf('=')) : newText;
						if (name.isBlank()) {
							return "Define name must not be empty";
						}
						if (!name.matches("[A-Za-z_][A-Za-z0-9_.]*")) {
							return "Define name must be a valid identifier";
						}
						return null;
					}
				});

		if (dlg.open() == Window.OK) {
			return dlg.getValue().trim();
		}
		return null;
	}

	@Override
	protected String createList(String[] items) {
		return String.join(AsmPreferenceConstants.DEFINES_SEPARATOR, items);
	}

	@Override
	protected String[] parseString(String stringList) {
		if (stringList == null || stringList.isEmpty()) {
			return new String[0];
		}
		return stringList.split(AsmPreferenceConstants.DEFINES_SEPARATOR);
	}
}
