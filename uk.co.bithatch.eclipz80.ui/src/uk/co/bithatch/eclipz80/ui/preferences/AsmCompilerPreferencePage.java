package uk.co.bithatch.eclipz80.ui.preferences;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;

import uk.co.bithatch.widgetzx.ContainerFieldEditor;
import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;

public class AsmCompilerPreferencePage extends AbstractProjectSpecificPreferencePage {

	private RadioGroupFieldEditor assemblerMode;
	private FileFieldEditor externalCommand;

	public AsmCompilerPreferencePage() {
		super(AsmPreferencesAccess.get(), AsmPreferenceConstants.COMPILER, GRID);
	}

	@Override
	protected void createFieldEditors() {
		super.createFieldEditors();

		addField(new ContainerFieldEditor(AsmPreferenceConstants.OUTPUT_PATH, "Output Path:",
				getFieldEditorParent(), getWorkbench().getAdapter(IWorkspace.class).getRoot()) {

			@Override
			public void setEnabled(boolean enabled, Composite parent) {
				super.setEnabled(enabled, parent);
				setEmptyStringAllowed(!enabled);
				checkState();
			}

			@Override
			protected String getDirectory() {
				var dir = super.getDirectory();
				return dir == null ? null : dir.substring(dir.indexOf('/', 1) + 1);
			}
		});

		assemblerMode = new RadioGroupFieldEditor(
				AsmPreferenceConstants.ASSEMBLER_MODE,
				"Assembler:",
				1,
				new String[][] {
						{ "Use built-in assembler (experimental)", AsmPreferenceConstants.ASSEMBLER_MODE_BUILTIN },
						{ "Use external command", AsmPreferenceConstants.ASSEMBLER_MODE_EXTERNAL }
				},
				getFieldEditorParent(),
				true);
		addField(assemblerMode);

		externalCommand = new FileFieldEditor(
				AsmPreferenceConstants.EXTERNAL_COMMAND,
				"External assembler command:",
				getFieldEditorParent()) {
			@Override
			protected boolean checkState() {
				clearErrorMessage();
				return true;
			}
		};
		externalCommand.setEmptyStringAllowed(true);
		addField(externalCommand);

		updateExternalCommandEnablement();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		if (AsmPreferenceConstants.ASSEMBLER_MODE.equals(event.getProperty())) {
			updateExternalCommandEnablement();
		}
	}

	private void updateExternalCommandEnablement() {
		if (externalCommand != null && assemblerMode != null) {
			var store = getPreferenceStore();
			var mode = store.getString(AsmPreferenceConstants.ASSEMBLER_MODE);
			boolean isExternal = AsmPreferenceConstants.ASSEMBLER_MODE_EXTERNAL.equals(mode);
			externalCommand.setEnabled(isExternal, getFieldEditorParent());
		}
	}
}
