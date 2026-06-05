package uk.co.bithatch.eclipz88dk.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.service.prefs.BackingStoreException;

import uk.co.bithatch.eclipz88dk.Activator;
import uk.co.bithatch.widgetzx.ZXPerspectivesUI;

public abstract class AbstractZ88DKProjectWizard<PAGE extends AbstractZ88DKProjectWizardPage> extends Wizard
		implements INewWizard {

	public interface CreateTask {
		void call(IProgressMonitor monitor, java.net.URI locationURI) throws Exception;
	}

	private static final String C_PERSPECTIVE = "org.eclipse.cdt.ui.CPerspective";

	protected PAGE page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("New Z88DK Project");
	}

	@Override
	public void addPages() {
		page = createMainPage();
		addPage(page);
	}

	protected abstract PAGE createMainPage();

	@Override
	public final boolean performFinish() {
		String projectName = page.getProjectName();
		if (projectName.isEmpty())
			return false;

		var delegate = doProjectCreation(projectName);
		var locationURI = page.getLocationURI();
		var operation = new WorkspaceModifyOperation() {
			@Override
			protected void execute(IProgressMonitor monitor) throws CoreException {
				try {
					delegate.call(monitor, locationURI);
				} catch (CoreException ce) {
					throw ce;
				} catch (Exception e) {
					e.printStackTrace();
					throw new CoreException(Status.error("Failed to create project.", e));
				}
			}
		};

		try {
			getContainer().run(true, true, operation);
		} catch (InvocationTargetException | InterruptedException e) {
			// Handle exception or cancel
			return false;
		}

		ZXPerspectivesUI.zxCodingPerspective(Activator.PLUGIN_ID);

		return true;
	}

	protected abstract CreateTask doProjectCreation(String projectName);
}
