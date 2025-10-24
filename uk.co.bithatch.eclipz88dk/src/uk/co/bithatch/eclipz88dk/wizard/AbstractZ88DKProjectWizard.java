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

public abstract class AbstractZ88DKProjectWizard<PAGE extends AbstractZ88DKProjectWizardPage> extends Wizard
		implements INewWizard {

	public interface CreateTask {
		void call(IProgressMonitor monitor) throws Exception;
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
		var operation = new WorkspaceModifyOperation() {
			@Override
			protected void execute(IProgressMonitor monitor) throws CoreException {
				try {
					delegate.call(monitor);
					checkPerspective();
				} catch (CoreException ce) {
					throw ce;
				} catch (Exception e) {
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

		return true;
	}

	protected abstract CreateTask doProjectCreation(String projectName);

	private void checkPerspective() {
		final String PREFERENCE_KEY = "eclipz88dk.switch.perspective";

		var prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		var pref = prefs.get(PREFERENCE_KEY, "prompt");

		var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		var page = window.getActivePage();
		var currentPerspective = page.getPerspective();

		if (!C_PERSPECTIVE.equals(currentPerspective.getId())) {
			if ("prompt".equals(pref)) {
				var dialog = MessageDialogWithToggle.openYesNoQuestion(window.getShell(), "Switch to C Perspective?",
						"This project works best in the C perspective.\n" + "Would you like to switch now?",
						"Remember my decision and do not ask again", false, // toggle default
						null, null);

				var switchPerspective = (dialog.getReturnCode() == IDialogConstants.YES_ID);
				var toggleState = dialog.getToggleState() ? (switchPerspective ? "always" : "never") : "prompt";

				prefs.put(PREFERENCE_KEY, toggleState);
				try {
					prefs.flush();
				} catch (BackingStoreException e) {
				}

				if (switchPerspective) {
					switchToPerspective(C_PERSPECTIVE, window);
				}

			} else if ("always".equals(pref)) {
				switchToPerspective(C_PERSPECTIVE, window);
			}
		}
	}

	private void switchToPerspective(String perspectiveId, IWorkbenchWindow window) {
		IPerspectiveRegistry registry = PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor desc = registry.findPerspectiveWithId(perspectiveId);
		if (desc != null) {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().setPerspective(desc);
		}
	}
}
