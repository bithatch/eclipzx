package uk.co.bithatch.nextzxos;


import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


public class NextZXOSPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private boolean ok;

	public NextZXOSPreferencePage() {
		super(FLAT);
		setDescription("""
				For ZX Next Emulation you are going to need a disk image for the operating
				system. Choose here how the emulator should find it. If this is blank, the
				default built-in image will be used. If it is an HTTP URL, the emulator will
				try to download it from there. If it is a file path, the emulator will try
				to load it from there. The location may either be ZIP file, in which the image
				will be extracted from the zip (the first .img file found will be used), or 
				it may be the image file itself (again must  have a .img extension). 
				""");
	}

	@Override
	protected void createFieldEditors() {
		setPreferenceStore(PreferencesAccess.get().getPreferenceStore());
		addField(new FileFieldEditor(PreferenceConstants.NEXT_ZXOS, "Next ZXOS Location", getFieldEditorParent()));
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
//		if(ok) {
//			try {
//				PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, false, new UpdateSettingsOperation());
//			} catch (InvocationTargetException  |  InterruptedException e) {
//				throw new IllegalStateException(e);
//			}
//		}
		super.dispose();
	}

}
