package uk.co.bithatch.eclipz88dk.wizard;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public abstract class AbstractZ88DKProjectWizardPage extends WizardPage {

	private Text projectNameText;
	private ControlDecoration decoration;
	private Button useDefaultLocationButton;
	private Text locationText;
	private Button browseButton;
	private Label locationLabel;
	private Button createExampleProgramButton;

	protected AbstractZ88DKProjectWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	protected AbstractZ88DKProjectWizardPage(String pageName) {
		super(pageName);
	}

	@Override
    public void createControl(Composite parent) {
        var container = new Composite(parent, SWT.NONE);
        var layout = new GridLayout(3, false);
        layout.verticalSpacing = 8;
        layout.horizontalSpacing = 16;
		container.setLayout(layout);

        var label = new Label(container, SWT.NONE);
        label.setText("Project name:");

        projectNameText = new Text(container, SWT.BORDER);
        projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        projectNameText.addModifyListener(e -> dialogChanged());

		decoration = new ControlDecoration(projectNameText, SWT.LEFT | SWT.TOP);
		decoration.setImage(PlatformUI.getWorkbench().getSharedImages()
		        .getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		decoration.setDescriptionText("Project name already exists.");
		decoration.hide();

        useDefaultLocationButton = new Button(container, SWT.CHECK);
        useDefaultLocationButton.setText("Use default location");
        useDefaultLocationButton.setSelection(true);
        useDefaultLocationButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        useDefaultLocationButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateLocationState();
                dialogChanged();
            }
        });

        locationLabel = new Label(container, SWT.NONE);
        locationLabel.setText("Location:");

        locationText = new Text(container, SWT.BORDER);
        locationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        locationText.addModifyListener(e -> {
        	checkForEmptyProject();
        	dialogChanged(); 
        });

        browseButton = new Button(container, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                var dialog = new DirectoryDialog(getShell());
                dialog.setMessage("Select the project location");
                var path = dialog.open();
                if (path != null) {
                    locationText.setText(path);
                }
            }
        });

        updateLocationState();
        
        createFields(container);

        createExampleProgramButton = new Button(container, SWT.CHECK);
        createExampleProgramButton.setText("Create example program");
        createExampleProgramButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        createExampleProgramButton.setSelection(true);
        createExampleProgramButton.setVisible(showCreateExampleProgram());

        setControl(container);
        
        setPageComplete(false);
    }

	private void checkForEmptyProject() {
		if(!useDefaultLocationButton.getSelection()) {
			String name = projectNameText.getText().trim();
			String loc = locationText.getText().trim();
			if (name.isEmpty() && (loc.contains("/") || loc.contains("\\"))) {
				projectNameText.setText(new File(loc).getName());
			}
		}
	}

	private void updateLocationState() {
		boolean useDefault = useDefaultLocationButton.getSelection();
		locationLabel.setEnabled(!useDefault);
		locationText.setEnabled(!useDefault);
		browseButton.setEnabled(!useDefault);
	}

	public void setProject(String project) {
		projectNameText.setText(project);
	}

    protected void createFields(Composite container) {
	}

	/**
	 * Override to return {@code false} in wizard pages that should not show
	 * the "Create example program" checkbox (e.g. examples wizards).
	 */
	protected boolean showCreateExampleProgram() {
		return true;
	}

	public boolean isCreateExampleProgram() {
		return createExampleProgramButton.getSelection();
	}

	public String getProjectName() {
        return projectNameText.getText().trim();
    }
	
	/**
	 * Returns the location URI for the project, or {@code null} if the default
	 * workspace location should be used.
	 */
	public URI getLocationURI() {
		if (useDefaultLocationButton.getSelection()) {
			return null;
		}
		return new File(locationText.getText().trim()).toURI();
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
        if (!useDefaultLocationButton.getSelection()) {
            String loc = locationText.getText().trim();
            if (loc.isEmpty()) {
                updateStatus("Location must not be empty.");
                return;
            }
            var dir = new File(loc);
            if (!dir.isDirectory()) {
                updateStatus("Location does not exist or is not a directory.");
                return;
            }
        }

    	decoration.hide();
        updateStatus(null);
        updateCreateExampleDefault();
    }

	private boolean projectExists(String projectName) {
	    if (projectName == null || projectName.trim().isEmpty()) {
	        return false;
	    }
	    IWorkspace workspace = ResourcesPlugin.getWorkspace();
	    IProject project = workspace.getRoot().getProject(projectName);
	    return project.exists();
	}

	/**
	 * Update the "Create example program" checkbox default based on whether the
	 * resolved project directory already exists and contains files.  If it does,
	 * the user is likely importing existing source so we default to unchecked.
	 */
	protected void updateCreateExampleDefault() {
		File projectDir;
		if (useDefaultLocationButton.getSelection()) {
			var wsRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
			projectDir = new File(wsRoot, projectNameText.getText().trim());
		} else {
			projectDir = new File(locationText.getText().trim());
		}
		boolean hasContent = projectDir.isDirectory() && projectDir.list() != null && projectDir.list().length > 0;
		createExampleProgramButton.setSelection(!hasContent);
	}


	private void updateStatus(String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }
}
