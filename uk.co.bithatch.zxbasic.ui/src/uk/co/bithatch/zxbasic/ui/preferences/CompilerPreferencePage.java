package uk.co.bithatch.zxbasic.ui.preferences;

import static uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferenceConstants.SDK_PATHS;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;

import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.widgetzx.ContainerFieldEditor;
import uk.co.bithatch.widgetzx.DynamicComboFieldEditor;
import uk.co.bithatch.widgetzx.LanguageSystemUI;
import uk.co.bithatch.widgetzx.SpinnerFieldEditor;
import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicLanguageSystemProvider;
import uk.co.bithatch.zxbasic.ui.library.ContributedSDKRegistry;

public class CompilerPreferencePage extends AbstractProjectSpecificPreferencePage {

	private DynamicComboFieldEditor arch;
	private DynamicComboFieldEditor ofmt;
	private DynamicComboFieldEditor sdks;

	public CompilerPreferencePage() {
		super(ZXBasicPreferencesAccess.get(), ZXBasicPreferenceConstants.COMPILER, GRID);
	}

	@Override
	public void dispose() {
		getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (SDK_PATHS.equals(event.getProperty())) {
			sdks.updateEntries(getSDKChoices());
		} else if (ZXBasicPreferenceConstants.ARCHITECTURE.equals(event.getProperty())) {
			updateOutputFormats((String) event.getNewValue());
		}

	}

	@Override
	protected void createFieldEditors() {
		super.createFieldEditors();

		addField(new ContainerFieldEditor(ZXBasicPreferenceConstants.OUTPUT_PATH, "Output Path:",
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

		addField(new SpinnerFieldEditor(ZXBasicPreferenceConstants.OPTIMIZATION_LEVEL, "Optimization Level",
				getFieldEditorParent(), 0, 5, 1));

		sdks = new DynamicComboFieldEditor(ZXBasicPreferenceConstants.SDK, "SDK:", getSDKChoices(),
				getFieldEditorParent());
		addField(sdks);

		arch = new DynamicComboFieldEditor(ZXBasicPreferenceConstants.ARCHITECTURE, "Architecture:",
				LanguageSystemUI.describedOptions(LanguageSystem
						.languageSystem(BorielZXBasicLanguageSystemProvider.class).architectures(project)),
				getFieldEditorParent());
		addField(arch);

		ofmt = new DynamicComboFieldEditor(ZXBasicPreferenceConstants.OUTPUT_FORMAT, "Output Format:",
				LanguageSystemUI.describedOptions(LanguageSystem
						.languageSystem(BorielZXBasicLanguageSystemProvider.class).outputFormats(project)),
				getFieldEditorParent());
		addField(ofmt);

		updateOutputFormats(getPreferenceStore().getString(ZXBasicPreferenceConstants.ARCHITECTURE));

		getPreferenceStore().addPropertyChangeListener(this);
	}

	private String[][] getSDKChoices() {
		return ContributedSDKRegistry.getAllSDKsAsOptions();
	}

	private void updateOutputFormats(String archName) {
		ofmt.updateEntries(LanguageSystemUI.describedOptions(
				LanguageSystem.languageSystem(BorielZXBasicLanguageSystemProvider.class).outputFormats(project)));
	}
}
