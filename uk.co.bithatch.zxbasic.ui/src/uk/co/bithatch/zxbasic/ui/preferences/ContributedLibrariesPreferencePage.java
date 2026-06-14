package uk.co.bithatch.zxbasic.ui.preferences;

import static uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferenceConstants.ARCHITECTURE;
import static uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferenceConstants.DEPENDENCIES;
import static uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferenceConstants.SDK;
import static uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferenceConstants.SDK_PATHS;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;

public class ContributedLibrariesPreferencePage extends AbstractProjectSpecificPreferencePage
		implements IPreferenceChangeListener {

	private ContributedLibrariesFieldEditor editor;
	private IEclipsePreferences prefs;
	private BuiltInLibrariesField builtIns;

	public ContributedLibrariesPreferencePage() {
		super(ZXBasicPreferencesAccess.get(), ZXBasicPreferenceConstants.CONTRIBUTED, GRID);
		setDescription("Contributed Libraries are provided by the IDE, or through one of its extensions. "
				+ "These are added to your User Libraries to make up the final set of libraries available "
				+ "to your project. You can enable or disable contributed libraries, but built-ins are "
				+ "provided by the current SDK.");
	}

	@Override
	protected void createFieldEditors() {
		super.createFieldEditors();
		
		prefs = ZXBasicPreferencesAccess.get().getPreferences(project);

		var builtInsParent = getFieldEditorParent();
		builtInsParent.setLayout(new GridLayout(1, true));
		builtInsParent.setLayoutData(new GridData(GridData.FILL_BOTH));
		builtIns = new BuiltInLibrariesField(DEPENDENCIES, "Built-Ins", builtInsParent, project);
		addField(builtIns);

		var contributedParent = getFieldEditorParent();
		contributedParent.setLayout(new GridLayout(1, true));
		contributedParent.setLayoutData(new GridData(GridData.FILL_BOTH));
		editor = new ContributedLibrariesFieldEditor(DEPENDENCIES, "Contributed", contributedParent, project);
		addField(editor);

		prefs.addPreferenceChangeListener(this);

	}

	@Override
	public void dispose() {
		prefs.removePreferenceChangeListener(this);
		super.dispose();
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (SDK.equals(event.getKey()) || SDK_PATHS.equals(event.getKey()) || SDK.equals(event.getKey())
				|| ARCHITECTURE.equals(event.getKey())) {
			editor.refresh();
			builtIns.refresh();
		}
	}
}
