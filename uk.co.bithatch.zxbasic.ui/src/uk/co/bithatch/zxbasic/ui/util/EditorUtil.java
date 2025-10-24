package uk.co.bithatch.zxbasic.ui.util;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import uk.co.bithatch.zxbasic.ui.navigator.ILibraryContentsNode;

public class EditorUtil {
	
	public static void openFileInEditor(ILibraryContentsNode file) {
	    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	    IWorkbenchPage page = window.getActivePage();

	    try {
	        FileStorageEditorInput input = new FileStorageEditorInput(file);
	        IDE.openEditor(page, input, "org.eclipse.ui.DefaultTextEditor", true);
	    } catch (PartInitException e) {
	        MessageDialog.openError(window.getShell(), "Error", "Could not open file: " + e.getMessage());
	    }
	}

}