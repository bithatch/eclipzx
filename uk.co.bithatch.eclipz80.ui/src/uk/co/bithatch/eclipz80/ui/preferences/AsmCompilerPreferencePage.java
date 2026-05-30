package uk.co.bithatch.eclipz80.ui.preferences;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;

import uk.co.bithatch.widgetzx.ContainerFieldEditor;
import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;

public class AsmCompilerPreferencePage extends AbstractProjectSpecificPreferencePage {

	private RadioGroupFieldEditor assemblerMode;
	private FileFieldEditor externalCommand;
	private BooleanFieldEditor alwaysGenerateMap;

	public AsmCompilerPreferencePage() {
		super(AsmPreferencesAccess.get(), AsmPreferenceConstants.COMPILER, GRID);
	}

	@Override
	protected void createFieldEditors() {
		super.createFieldEditors();

		addField(new ContainerFieldEditor(AsmPreferenceConstants.OUTPUT_PATH, "Output Path:", getFieldEditorParent(),
				getWorkbench().getAdapter(IWorkspace.class).getRoot()) {

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

		assemblerMode = new RadioGroupFieldEditor(AsmPreferenceConstants.ASSEMBLER_MODE, "Assembler:", 1,
				new String[][] {
						{ "Use built-in assembler (experimental)", AsmPreferenceConstants.ASSEMBLER_MODE_BUILTIN },
						{ "Use external command", AsmPreferenceConstants.ASSEMBLER_MODE_EXTERNAL } },
				getFieldEditorParent(), true);
		addField(assemblerMode);

		externalCommand = new FileFieldEditor(AsmPreferenceConstants.EXTERNAL_COMMAND, "External assembler command:",
				getFieldEditorParent()) {
			@Override
			protected boolean checkState() {
				clearErrorMessage();
				return true;
			}
		};
		externalCommand.setEmptyStringAllowed(true);
		addField(externalCommand);

		alwaysGenerateMap = new BooleanFieldEditor(AsmPreferenceConstants.GENERATE_MAP, "Always generate map file",
				getFieldEditorParent()) {
			private boolean setValues;

			@Override
			protected void valueChanged(boolean oldValue, boolean newValue) {
				if (newValue && !setValues) {
					setValues = true;
					copyWorkspaceSettingsToProject();
				}
				updateAvailableState();
			}
		};
		addField(alwaysGenerateMap);
	}

	@Override
	protected void updateApplyButton() {
		updateExternalCommandEnablement(AsmPreferencesAccess.get().getAssemblerMode(project));
		super.updateApplyButton();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		if (event.getSource() instanceof RadioGroupFieldEditor) {
			updateExternalCommandEnablement((String)event.getNewValue());
		}
	}

	private boolean isExternal(String mode) {;
		return AsmPreferenceConstants.ASSEMBLER_MODE_EXTERNAL.equals(mode);
	}

	private void updateExternalCommandEnablement(String mode) {
		var ext = isExternal(mode);
		if (externalCommand != null && assemblerMode != null) {
			externalCommand.setEnabled(ext, getFieldEditorParent());
		}
		if (alwaysGenerateMap != null) {
			alwaysGenerateMap.setEnabled(!ext, getFieldEditorParent());
		}
	}
}
