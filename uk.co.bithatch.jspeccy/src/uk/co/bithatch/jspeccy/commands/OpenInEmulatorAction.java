package uk.co.bithatch.jspeccy.commands;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.jspeccy.views.EmulatorView;

public class OpenInEmulatorAction implements IObjectActionDelegate {

	private IFile selectedFile;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection sel) {
			Object obj = sel.getFirstElement();
			if (obj instanceof IFile file) {
				selectedFile = file;
			}
		}
	}

	@Override
	public void run(IAction action) {
		if (selectedFile != null) {
			try {
				var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				var	 view = (EmulatorView) page.showView(EmulatorView.ID);
				view.getEmulator().load(selectedFile.getLocation().toFile());
			} catch (PartInitException e) {
				throw new IllegalStateException(e);
			} catch(IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

}
