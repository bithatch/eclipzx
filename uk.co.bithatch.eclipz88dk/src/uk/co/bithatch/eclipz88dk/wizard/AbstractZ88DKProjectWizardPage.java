package uk.co.bithatch.eclipz88dk.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public abstract class AbstractZ88DKProjectWizardPage extends WizardPage {

	private Text projectNameText;
	private ControlDecoration decoration;

	protected AbstractZ88DKProjectWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	protected AbstractZ88DKProjectWizardPage(String pageName) {
		super(pageName);
	}

	@Override
    public void createControl(Composite parent) {
        var container = new Composite(parent, SWT.NONE);
        var layout = new GridLayout(2, false);
        layout.verticalSpacing = 8;
        layout.horizontalSpacing = 16;
		container.setLayout(layout);

        var label = new Label(container, SWT.NONE);
        label.setText("Project name:");

        projectNameText = new Text(container, SWT.BORDER);
        projectNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        projectNameText.addModifyListener(e -> dialogChanged());
        

		decoration = new ControlDecoration(projectNameText, SWT.LEFT | SWT.TOP);
		decoration.setImage(PlatformUI.getWorkbench().getSharedImages()
		        .getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		decoration.setDescriptionText("Project name already exists.");
		decoration.hide();
        
        createFields(container);

        setControl(container);
        
        setPageComplete(false);
    }
	
	public void setProject(String project) {
		projectNameText.setText(project);
	}

    protected void createFields(Composite container) {
	}

	public String getProjectName() {
        return projectNameText.getText().trim();
    }
	
	protected void dialogChanged() {
        String name = projectNameText.getText().trim();

        if (name.isEmpty()) {
        	decoration.show();
            updateStatus("Project name must not be empty.");
            return;
        }
        if (projectExists(name)) {
        	decoration.show();
            updateStatus("A project with that name already exists.");
            return;
        }

    	decoration.hide();
        updateStatus(null);
    }

	private boolean projectExists(String projectName) {
	    if (projectName == null || projectName.trim().isEmpty()) {
	        return false;
	    }
	    IWorkspace workspace = ResourcesPlugin.getWorkspace();
	    IProject project = workspace.getRoot().getProject(projectName);
	    return project.exists();
	}


	private void updateStatus(String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }
}
