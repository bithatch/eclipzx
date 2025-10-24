package uk.co.bithatch.zxbasic.ui.wizard;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import uk.co.bithatch.widgetzx.util.FileCopyUtil;
import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;
import uk.co.bithatch.zxbasic.ui.library.ContributedSDKRegistry;

public class ZXBasicExamplesWizard extends AbstractBasicProjectWizard<ZXBasicExamplesWizardPage> {

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle("New ZX BASIC Examples Project");
	}

	@Override
	protected ZXBasicExamplesWizardPage createMainPage() {
		return new ZXBasicExamplesWizardPage();
	}

	@Override
	protected boolean onProjectCreated(IProject project) throws CoreException {
		var sdk = ContributedSDKRegistry.getAllSDKs().get(page.sdk.getSelectionIndex());
		WorkspaceModifyOperation operation = new WorkspaceModifyOperation() {
			@Override
			protected void execute(IProgressMonitor monitor) throws CoreException {
				try {
					FileCopyUtil.copyDirectoryToProject(new File(sdk.location(), "examples"), project, monitor);
				} catch (IOException e) {
					throw new CoreException(new Status(IStatus.ERROR, ZXBasicUiActivator.PLUGIN_ID, "Failed to copy templates", e));
				}
			}
		};

		try {
			getContainer().run(true, true, operation);
		} catch (InvocationTargetException | InterruptedException e) {
			// Handle exception or cancel
			return false;
		}

		return true;
	}

}
