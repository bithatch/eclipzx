package uk.co.bithatch.widgetzx.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.bitzx.AbstractPreferencesAccess;
import uk.co.bithatch.bitzx.IPreferenceConstants;

public abstract class AbstractProjectSpecificPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {

    protected IProject project;
    private IWorkbench workbench;
    private List<FieldEditor> prjFields = new ArrayList<>();
	private BooleanFieldEditor showProjectSpecific;
	private final String pssCategory;
	private final  AbstractPreferencesAccess pax;

	public AbstractProjectSpecificPreferencePage(AbstractPreferencesAccess pax, String pssCategory) {
		super();
		this.pax = pax;
		this.pssCategory = pssCategory;
        setPreferenceStore(pax.getPreferenceStore());
	}

	public AbstractProjectSpecificPreferencePage(AbstractPreferencesAccess pax, String pssCategory, int style) {
		super(style);
		this.pax = pax;
		this.pssCategory = pssCategory;
        setPreferenceStore(pax.getPreferenceStore());
	}

	public AbstractProjectSpecificPreferencePage(AbstractPreferencesAccess pax, String pssCategory, String title, ImageDescriptor image, int style) {
		super(title, image, style);
		this.pax = pax;
		this.pssCategory = pssCategory;
        setPreferenceStore(pax.getPreferenceStore());
	}

	public AbstractProjectSpecificPreferencePage(AbstractPreferencesAccess pax, String pssCategory, String title, int style) {
		super(title, style);
		this.pax = pax;
		this.pssCategory = pssCategory;
        setPreferenceStore(pax.getPreferenceStore());
	}


    @Override
	protected void createFieldEditors() {


    	if(project != null) {
    		var parent = getFieldEditorParent();

	        showProjectSpecific = new BooleanFieldEditor(
	            pssCategory + "." + IPreferenceConstants.PROJECT_SPECIFIC_SETTINGS,
	            "Enable project specific settings",
	            parent
	        ) {
	        	private boolean setValues;

	        	@Override
	        	protected void valueChanged(boolean oldValue, boolean newValue) {
	        		if(newValue && !setValues) {
	        			setValues = true;
	        			copyWorkspaceSettingsToProject();
	        		}
	        		updateAvailableState();
	        	}
	        };
			addField(showProjectSpecific);
    	}

    	
	}

    @Override
	protected final Composite getFieldEditorParent() {
		var par = super.getFieldEditorParent();
		var layout = par == null ? null : par.getLayout();
		if(layout != null) {
//			((GridLayout)layout).marginBottom = 24;
			((GridLayout)layout).verticalSpacing = 8;
		}
		return par;
	}

    protected final void copyWorkspaceSettingsToProject() {
		var wsprefs = pax.getPreferenceStore();
		var thisPrefs = getPreferenceStore();
		prjFields.forEach(fld -> {
			if(fld != showProjectSpecific) {
				var ws =  fld.getPreferenceName();
				if(!thisPrefs.contains(ws)) {
					thisPrefs.setValue(ws, wsprefs.getString(ws));
				}
			}
		});
	}

	@Override
	protected final void initialize() {
		super.initialize();
		updateAvailableState();
	}

	protected final void updateAvailableState() {
    	updateProjectEnablement();
		checkState();
		updateApplyButton();
    }

	@Override
    public final void init(IWorkbench workbench) {
    	this.workbench = workbench;
    }

	@Override
	public final IAdaptable getElement() {
		return project;
	}

	@Override
	public final void setElement(IAdaptable selected) {
		project = selected.getAdapter(IProject.class);
		if(workbench == null) {
			workbench = selected.getAdapter(IWorkbench.class);
		}
		setPreferenceStore(pax.getPreferenceStore(project));
		updateProjectEnablement();
	}

	public IWorkbench getWorkbench() {
		if(workbench == null) {
			return PlatformUI.getWorkbench();
		} else {
			return workbench;
		}
	}

	@Override
	protected final void addField(FieldEditor editor) {
		super.addField(editor);
		prjFields.add(editor);
		 
		if( showProjectSpecific != null && 
			!showProjectSpecific.getBooleanValue() && 
			!editor.getPreferenceName().endsWith(IPreferenceConstants.PROJECT_SPECIFIC_SETTINGS) && 
			!pax.isProjectSpecificFor(project, editor.getPreferenceName())) {
			/* Reset project specific values if not project specific */
			/* TODO: not greate, ideally we wouldn't actually set these */
			getPreferenceStore().setValue(editor.getPreferenceName(), pax.getPreferenceStore().getString(editor.getPreferenceName()));
		}
		updateProjectEnablement();
	}

	protected final void updateProjectEnablement() {
		prjFields.forEach(fld -> {
			if(fld != showProjectSpecific) {
				var val = isValidForProjectState(fld);
				setEnabled(fld, val);
			}
		});
	}

	protected final void setEnabled(FieldEditor fld, boolean val) {
		fld.setEnabled(val, getFieldEditorParent());
	}

	protected final boolean isValidForProjectState(FieldEditor fld) {
		return showProjectSpecific == null || showProjectSpecific.getBooleanValue();
	}
}
