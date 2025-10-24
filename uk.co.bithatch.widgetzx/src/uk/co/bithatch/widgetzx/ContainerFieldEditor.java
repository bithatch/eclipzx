package uk.co.bithatch.widgetzx;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

public class ContainerFieldEditor extends StringButtonFieldEditor {
	private IWorkspaceRoot root;

	protected ContainerFieldEditor() {
	}

	public ContainerFieldEditor(String name, String labelText, Composite parent, IWorkspaceRoot root) {
		init(name, labelText);
		this.root = root;
		setEmptyStringAllowed(false);
		setErrorMessage("Empty output path not allowed.");
		setChangeButtonText(JFaceResources.getString("openBrowse"));//$NON-NLS-1$
		setValidateStrategy(VALIDATE_ON_KEY_STROKE);
		createControl(parent);
	}

	@Override
	protected String changePressed() {
		return getDirectory();
	}

	@Override
	protected boolean doCheckState() {
		String fileName = getTextControl().getText();
		fileName = fileName.trim();
		return !fileName.isEmpty() ||  ( fileName.isEmpty() && isEmptyStringAllowed());
	}

	protected String getDirectory() {
		

        // Project folder
        ContainerSelectionDialog projDialog = new ContainerSelectionDialog(
                getShell(),
                root,
                true,
                "Select a folder from your workspace");
        if (projDialog.open() == Window.OK) {
            Object[] selected = projDialog.getResult();
            if (selected.length > 0) {
                IPath path = (IPath) selected[0];
                return path.toPortableString();
            }
        }
		
		return null;
	}


}
