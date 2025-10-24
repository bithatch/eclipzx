package uk.co.bithatch.zxbasic.ui.preferences;

import java.io.File;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

public class LibraryFolderListEditor extends ListEditor {

    private final IWorkspaceRoot root;

    public LibraryFolderListEditor(String name, String labelText, Composite parent, IWorkspaceRoot root) {
        super(name, labelText, parent);
        this.root = root;
    }

	@Override
	public void setEnabled(boolean enabled, Composite parent) {
		getList().setEnabled(enabled);
		getAddButton().setEnabled(enabled);
		getRemoveButton().setEnabled(enabled);
	}
	
	public int getNumberOfItems() {
		return getList().getItemCount();
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
        Shell shell = getShell();

        // Offer a choice: project-relative or external
        MessageDialog dialog = new MessageDialog(shell,
                "Add Library Folder", null,
                "Do you want to add a folder from your project or an external folder?",
                MessageDialog.QUESTION,
                new String[] { "Project Folder", "External Folder", "Cancel" }, 0);
        int result = dialog.open();
        
        if (result == 0) {
            // Project folder
            ContainerSelectionDialog projDialog = new ContainerSelectionDialog(
                    shell,
                    root,
                    false,
                    "Select a folder from your workspace");
            if (projDialog.open() == Window.OK) {
                Object[] selected = projDialog.getResult();
                if (selected.length > 0) {
                    IPath path = (IPath) selected[0];
                    return path.toPortableString();
                }
            }
        } else if (result == 1) {
            // External folder
            DirectoryDialog dirDialog = new DirectoryDialog(shell);
            dirDialog.setText("Select Folder");
            String selected = dirDialog.open();
            if (selected != null) {
                return selected;
            }
        }

        return null;
    }

	@Override
	protected void adjustForNumColumns(int numColumns) {
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, numColumns, 1);
		getList().setLayoutData(gd);
		super.adjustForNumColumns(numColumns);
	}
}
