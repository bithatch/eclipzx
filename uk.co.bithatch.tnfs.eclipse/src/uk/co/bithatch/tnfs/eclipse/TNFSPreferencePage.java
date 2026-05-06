package uk.co.bithatch.tnfs.eclipse;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.tnfs.lib.TNFS;
import uk.co.bithatch.widgetzx.SpinnerFieldEditor;

public class TNFSPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private boolean ok;

	public TNFSPreferencePage() {
		super(FLAT);
		setDescription("""
				A built-in TNFS server can share your project folders with
				devices capable of talking this protocol, e.g. Spectranet.
				
				When enabaled, the TNFS server will be activated as soon as 
				you Share a folder or project.
				""");
	}

	@Override
	protected void createFieldEditors() {
		setPreferenceStore(TNFSPreferencesAccess.get().getPreferenceStore());
		addField(new BooleanFieldEditor(TNFSPreferenceConstants.ENABLE, "Enabled", getFieldEditorParent()));
		addField(new StringFieldEditor(TNFSPreferenceConstants.ADDRESS, "Bind Address", 20, StringFieldEditor.VALIDATE_ON_FOCUS_LOST, getFieldEditorParent()));
		addField(new SpinnerFieldEditor(TNFSPreferenceConstants.PORT, "Port", getFieldEditorParent(), 0, 65535, 1));
		addField(new BooleanFieldEditor(TNFSPreferenceConstants.TCP, "TCP", getFieldEditorParent()));
		addField(new BooleanFieldEditor(TNFSPreferenceConstants.UDP, "UDP", getFieldEditorParent()));
	}

	@Override
	protected void checkState() {
		setMessage(null);
		super.checkState();
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		ok = super.performOk();
		return ok;
	}

	@Override
	public void dispose() {
		if(ok) {
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, false, new UpdateSettingsOperation());
			} catch (InvocationTargetException  |  InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}
		super.dispose();
	}

}
