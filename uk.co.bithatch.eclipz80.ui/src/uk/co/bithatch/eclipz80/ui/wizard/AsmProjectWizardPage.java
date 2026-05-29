package uk.co.bithatch.eclipz80.ui.wizard;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.fieldassist.ControlDecoration;
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

public class AsmProjectWizardPage extends WizardPage {

	private Text projectNameText;
	private ControlDecoration decoration;
	private Button useDefaultLocationButton;
	private Text locationText;
	private Button browseButton;
	private Label locationLabel;
	private Button createExampleProgramButton;

	public AsmProjectWizardPage() {
		super("Z80 Assembly Project");
		setTitle("Z80 Assembly Project");
		setDescription("Create a new Z80 assembly project.");
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

		createExampleProgramButton = new Button(container, SWT.CHECK);
		createExampleProgramButton.setText("Create example program");
		createExampleProgramButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		createExampleProgramButton.setSelection(true);

		setControl(container);
		setPageComplete(false);
	}

	private void updateLocationState() {
		boolean useDefault = useDefaultLocationButton.getSelection();
		locationLabel.setEnabled(!useDefault);
		locationText.setEnabled(!useDefault);
		browseButton.setEnabled(!useDefault);
	}

	public String getProjectName() {
		return projectNameText.getText().trim();
	}

	public URI getLocationURI() {
		if (useDefaultLocationButton.getSelection()) {
			return null;
		}
		return new File(locationText.getText().trim()).toURI();
	}

	public boolean isCreateExampleProgram() {
		return createExampleProgramButton.getSelection();
	}

	private void checkForEmptyProject() {
		if (!useDefaultLocationButton.getSelection()) {
			String name = projectNameText.getText().trim();
			String loc = locationText.getText().trim();
			if (name.isEmpty() && (loc.contains("/") || loc.contains("\\"))) {
				projectNameText.setText(new File(loc).getName());
			}
		}
	}

	private void dialogChanged() {
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
		return workspace.getRoot().getProject(projectName).exists();
	}

	private void updateCreateExampleDefault() {
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
