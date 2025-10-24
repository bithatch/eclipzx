package uk.co.bithatch.emuzx;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import uk.co.bithatch.bitzx.FileNames;

public abstract class AbstractEmulatorLaunchShortcut implements ILaunchShortcut2 {

	@Override
	public final void launch(ISelection selection, String mode) {
		if (selection instanceof ITreeSelection ts) {
			var firstElement = ts.getFirstElement(); // This Object is a File
			if (firstElement instanceof IAdaptable adapter) {
				var file = adapter.getAdapter(IFile.class);
				try {
					doLaunch(file, mode);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		var file = (IFile) editor.getEditorInput().getAdapter(IFile.class);
		try {
			doLaunch(file, mode);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
    public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
        return null; // Not needed unless showing a dialog
    }

    @Override
    public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
        return null;
    }

    @Override
    public IResource getLaunchableResource(ISelection selection) {
        if (selection instanceof IStructuredSelection structured) {
            Object element = structured.getFirstElement();
            if (element instanceof IFile file && isSupported(file)) {
                return file;
            }
        }
        return null;
    }
	
    @Override
    public IResource getLaunchableResource(IEditorPart editorPart) {
        IEditorInput input = editorPart.getEditorInput();
        if (input instanceof IFileEditorInput fileInput) {
            IFile file = fileInput.getFile();
            if(isSupported(file)) {
                return file;
            }
        }
        return null;
    }

	protected abstract String[] getSupportedExtensions();

	protected abstract void doLaunch(IFile file, String mode) throws CoreException;

	private boolean isSupported(IFile file) {
		String[] exts = getSupportedExtensions();
		return exts.length == 0 || FileNames.hasExtensions(file.getName(), exts);
	}
}