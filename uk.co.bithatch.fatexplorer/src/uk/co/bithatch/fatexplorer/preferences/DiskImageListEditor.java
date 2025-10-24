package uk.co.bithatch.fatexplorer.preferences;

import java.io.File;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;

public class DiskImageListEditor extends ListEditor {

    private final IWorkspaceRoot root;

    public DiskImageListEditor(String name, String labelText, Composite parent, IWorkspaceRoot root) {
        super(name, labelText, parent);
        this.root = root;
    }

	@Override
	public void setEnabled(boolean enabled, Composite parent) {
		getList().setEnabled(enabled);
		getAddButton().setEnabled(enabled);
		getRemoveButton().setEnabled(enabled);
	}

    @Override
    protected String createList(String[] items) {
        return String.join(File.pathSeparator, items);
    }

    @Override
    protected String[] parseString(String stringList) {
        return stringList.isEmpty() ? new String[0] : stringList.split(Pattern.quote(File.pathSeparator));
    }

    @Override
    protected String getNewInputObject() {
        return newDiskImageDialog(getShell(), root);
    }

	public static String newDiskImageDialog(Shell shell, IWorkspaceRoot root) {
		// Offer a choice: project-relative or external
        MessageDialog dialog = new MessageDialog(shell,
                "Add Disk Image", null,
                "Do you want to add a disk image from your project or an external file?",
                MessageDialog.QUESTION,
                new String[] { "Project File", "External File", "Cancel" }, 0);
        int result = dialog.open();
        
        if (result == 0) {
            ResourceSelectionDialog projDialog = new ResourceSelectionDialog(
                    shell,
                    root,
                    "Select a disk image from your workspace");
            if (projDialog.open() == Window.OK) {
                Object[] selected = projDialog.getResult();
                if (selected.length > 0) {
                	if(selected[0] instanceof IFile ifile) {
                		return ifile.getFullPath().toPortableString().substring(1);
                	}
                	else {
	                    IPath path = (IPath) selected[0];
	                    return path.toPortableString();
                	}
                }
            }
        } else if (result == 1) {
            FileDialog fileDialog = new FileDialog(shell);
            fileDialog.setText("Select File");
            fileDialog.setFilterExtensions(new String[] {"*.img", "*.*"});
            return fileDialog.open();
        }

        return null;
	}
}
