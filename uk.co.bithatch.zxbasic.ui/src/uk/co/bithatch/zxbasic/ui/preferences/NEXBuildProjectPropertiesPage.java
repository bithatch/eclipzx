package uk.co.bithatch.zxbasic.ui.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPropertyPage;

import uk.co.bithatch.zxbasic.tools.NexConverter;

public class NEXBuildProjectPropertiesPage extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {

	private IProject project;
	private BooleanFieldEditor includeSysVar;
	private FileFieldEditor location;
	private StringFieldEditor core;

	{
		setPreferenceStore(ZXBasicPreferencesAccess.get().getPreferenceStore());
	}

	public NEXBuildProjectPropertiesPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		core = new StringFieldEditor(ZXBasicPreferenceConstants.NEX_CORE, "Core", getFieldEditorParent());
		core.setEmptyStringAllowed(false);
		addField(core);

		includeSysVar = new BooleanFieldEditor(ZXBasicPreferenceConstants.NEX_INCLUDE_SYSVAR, "Include sysvar.bin",
				getFieldEditorParent()) {
			@Override
			protected void valueChanged(boolean oldValue, boolean newValue) {
				super.valueChanged(oldValue, newValue);
				updateAvailableState();
			}
		};
		addField(includeSysVar);

		location = new FileFieldEditor(ZXBasicPreferenceConstants.NEX_SYSVAR_LOCATION, "Alternative Location:",
				getFieldEditorParent()) {

			@Override
			public void setEnabled(boolean enabled, Composite parent) {
				super.setEnabled(enabled, getTextControl().getParent());
//				checkState();
			}
		};
		location.setEmptyStringAllowed(true);
		addField(location);
		updateApplyButton();
	}

    @Override
	protected void performDefaults() {
		super.performDefaults();
		core.setStringValue(NexConverter.DEFAULT_CORE_STR);
	}

	@Override
	protected void adjustGridLayout() {
    	super.adjustGridLayout();

		var par = super.getFieldEditorParent();
		var layout = par == null ? null : par.getLayout();
		if(layout != null) {
//			((GridLayout)layout).marginBottom = 24;
			((GridLayout)layout).verticalSpacing = 16;
		}
	}

	@Override
	public IAdaptable getElement() {
		return project;
	}

	@Override
	public void setElement(IAdaptable selected) {
		project = selected.getAdapter(IProject.class);
		setPreferenceStore(ZXBasicPreferencesAccess.get().getPreferenceStore(project));
	}

	private void updateAvailableState() {
		location.setEnabled(includeSysVar.getBooleanValue(), null); // this seems too weird
	}

}
