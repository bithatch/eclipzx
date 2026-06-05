package uk.co.bithatch.zxbasic.ui.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.widgetzx.ZXPerspectivesUI;
import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicNature;

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
                var locationURI = page.getLocationURI();
                if (locationURI != null) {
                    IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
                    desc.setLocationURI(locationURI);
                    project.create(desc, null);
                } else {
                    project.create(null);
                }
            }
            project.open(null);

            IProjectDescription desc = project.getDescription();
            desc.setNatureIds(new String[] {
                "org.eclipse.xtext.ui.shared.xtextNature",
                ZXBasicNature.NATURE_ID
            });
            project.setDescription(desc, null);

            if(onProjectCreated(project)) {
            	ZXPerspectivesUI.zxCodingPerspective(ZXBasicUiActivator.PLUGIN_ID);
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


	protected abstract boolean onProjectCreated(IProject project) throws CoreException;
}
