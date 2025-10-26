package uk.co.bithatch.zxbasic.ui.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.dialogs.WelcomeEditor;
import org.osgi.service.prefs.BackingStoreException;

import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicNature;
import uk.co.bithatch.zxbasic.ui.perspective.ZXBasicPerspective;

public abstract class AbstractBasicProjectWizard<PAGE extends AbstractBasicProjectWizardPage> extends Wizard implements INewWizard {

    protected PAGE page;

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("New ZX BASIC Project");
    }

    @Override
    public void addPages() {
        page = createMainPage();
        addPage(page);
    }

	protected abstract PAGE createMainPage();

    @Override
    public boolean performFinish() {
        String projectName = page.getProjectName();
        if (projectName.isEmpty()) return false;

        try {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            if (!project.exists()) {
                project.create(null);
            }
            project.open(null);

            IProjectDescription desc = project.getDescription();
            desc.setNatureIds(new String[] {
                "org.eclipse.xtext.ui.shared.xtextNature",
                ZXBasicNature.NATURE_ID
            });
            project.setDescription(desc, null);

            if(onProjectCreated(project)) {
            	final String PREFERENCE_KEY = "zxbasic.switch.perspective";

                var prefs = InstanceScope.INSTANCE.getNode(ZXBasicUiActivator.PLUGIN_ID);
                var pref = prefs.get(PREFERENCE_KEY, "prompt");

                var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                var page = window.getActivePage();
                var currentPerspective = page.getPerspective();

                if (!ZXBasicPerspective.ID.equals(currentPerspective.getId())) {
                    if ("prompt".equals(pref)) {
                        var dialog = MessageDialogWithToggle.openYesNoQuestion(
                            window.getShell(),
                            "Switch to ZX Basic Perspective?",
                            "This project works best in the ZX Basic perspective.\n" +
                            "Would you like to switch now?",
                            "Remember my decision and do not ask again",
                            false, // toggle default
                            null,
                            null
                        );

                        var switchPerspective = (dialog.getReturnCode() == IDialogConstants.YES_ID);
                        var toggleState = dialog.getToggleState() ? (switchPerspective ? "always" : "never") : "prompt";

                        prefs.put(PREFERENCE_KEY, toggleState);
                        try {
                            prefs.flush();
                        } catch (BackingStoreException e) {
                            e.printStackTrace();
                        }

                        if (switchPerspective) {
                            switchToPerspective(ZXBasicPerspective.ID, window);
                        }

                    } else if ("always".equals(pref)) {
                        switchToPerspective(ZXBasicPerspective.ID, window);
                    }
                }
                
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private void switchToPerspective(String perspectiveId, IWorkbenchWindow window) {
        IPerspectiveRegistry registry = PlatformUI.getWorkbench().getPerspectiveRegistry();
        IPerspectiveDescriptor desc = registry.findPerspectiveWithId(perspectiveId);
        if (desc != null) {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage().setPerspective(desc);
        }
    }


	protected abstract boolean onProjectCreated(IProject project) throws CoreException;
}
