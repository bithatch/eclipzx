package uk.co.bithatch.zxbasic.ui.preferences;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;

import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;
import uk.co.bithatch.zxbasic.ui.library.ContributedSDKRegistry;
import uk.co.bithatch.zxbasic.ui.tools.Python;

public class SDKPreferencePage extends AbstractProjectSpecificPreferencePage {

	private FileFieldEditor python;
	private LibraryFolderListEditor sdks;

	public SDKPreferencePage() {
		super(ZXBasicPreferencesAccess.get(), ZXBasicPreferenceConstants.SDKS, GRID);
		setDescription("""
				The ZX Basic SDK provides the compilers, runtime libraries and other
				tools. EclipZX has one built-in, but if you want to use your own
				particular version, you can choose one here. Each project can select
				the SDK it uses. This SDK also requires that Python is available, install
				it or manually select the location if it is not on your PATH.
				""");
	}

	@Override
	protected void createFieldEditors() {
		python = new FileFieldEditor(ZXBasicPreferenceConstants.SDK_PYTHON_LOCATION, "Python Interpreter", true,
				StringButtonFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()

		) {
			@Override
			protected String changePressed() {
				var c = super.changePressed();
				SDKPreferencePage.this.checkState();
				return c;
			}
		};
		python.setErrorMessage("Python interpreter path specified does not exist.");
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			python.setFileExtensions(new String[] { "*.exe" });
		}
		addField(python);

		sdks = new LibraryFolderListEditor(ZXBasicPreferenceConstants.SDK_PATHS, "User SDK Folders:",
				getFieldEditorParent(), getWorkbench().getAdapter(IWorkspace.class).getRoot()) {
			
		};
		addField(sdks);
		

	}

	@Override
	protected void checkState() {
		setMessage(null);
		super.checkState();
		if (isValid()) {
			var custom = python.getStringValue();
			if (custom.equals("")) {
				if (!Python.get().hasSystemInterpreter()) {
					setMessage("No Python interpreter path specified and no system interpreter.", ERROR);
					setValid(false);
				}
			}
			
			if(isValid() && sdks.getNumberOfItems() == 0 && ContributedSDKRegistry.getAllSDKs().isEmpty()) {
				setMessage("The default ZX Basic SDK is not installed, you must add one.", ERROR);
				setValid(false);
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
	}

	@Override
	protected void updateApplyButton() {
		// TODO Auto-generated method stub
		super.updateApplyButton();
	}
}
