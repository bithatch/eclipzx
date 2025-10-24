package uk.co.bithatch.zxbasic.ui.preparation;

import static org.eclipse.jface.layout.GridDataFactory.defaultsFor;
import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_CLEAR_BEFORE_USE;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_TARGET_LOCATION;

import java.util.function.Consumer;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import uk.co.bithatch.zxbasic.ui.api.ILaunchPreparationUI;
import uk.co.bithatch.zxbasic.ui.api.IPreparationTargetUI;

public class LocalPreparationTargetUI implements IPreparationTargetUI {

	private Text targetLocation;
	private Button clearBeforeUse;
	private boolean available;
	private Button fileSystem;
	private Label pathLabel;
	private Button workspace;

	@Override
	public void createControls(Composite parent, Runnable onUpdate) {

		var grid = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = 24;
		layout.verticalSpacing = 8;
		grid.setLayout(layout);

		pathLabel = new Label(grid, SWT.NONE);
		pathLabel.setLayoutData(swtDefaults().create());
		pathLabel.setText("Location:");

		targetLocation = new Text(grid, SWT.DROP_DOWN | SWT.READ_ONLY);
		targetLocation.setLayoutData(fillDefaults().grab(true, false).create());
		targetLocation.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				onUpdate.run();
			}
		});

		fileSystem = new Button(grid, SWT.PUSH);
		fileSystem.setText("File System");
		fileSystem.setLayoutData(GridDataFactory.defaultsFor(fileSystem).create());
		fileSystem.addSelectionListener(widgetSelectedAdapter(e -> {
			var fileDialog = new DirectoryDialog(parent.getShell(), SWT.OPEN);
			fileDialog.setText("Select Directory");
			var path = fileDialog.open();
			if (path != null) {
				targetLocation.setText(path);
			}
		}));

		workspace = new Button(grid, SWT.PUSH);
		workspace.setText("Workspace");
		workspace.setLayoutData(defaultsFor(fileSystem).create());
		workspace.addSelectionListener(widgetSelectedAdapter(e -> {
			var dialog = new ContainerSelectionDialog(parent.getShell(),
					PlatformUI.getWorkbench().getAdapter(IWorkspace.class).getRoot(), true,
					"Select a workspace relative working directory");
			if (dialog.open() == Window.OK) {
				var result = dialog.getResult();
				if (result.length > 0 && result[0] instanceof Path path) {
					targetLocation.setText(String.format("${workspace_loc:%s}", path.makeRelative()));
				}
			}
		}));

		clearBeforeUse = new Button(grid, SWT.CHECK);
		clearBeforeUse.setText("Clear Before Use");
		clearBeforeUse.setLayoutData(swtDefaults().create());
		clearBeforeUse.addSelectionListener(widgetSelectedAdapter(e -> {
			onUpdate.run();
		}));
		updateState();
	}

	@Override
	public void init(ILaunchPreparationUI prepUi) {
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		targetLocation.setText(
				config.getAttribute(PREPARATION_TARGET_LOCATION, LocalPreparationTarget.defaultLocalPreparation()));
		clearBeforeUse.setSelection(config.getAttribute(PREPARATION_CLEAR_BEFORE_USE, false));
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(PREPARATION_TARGET_LOCATION, targetLocation.getText());
		config.setAttribute(PREPARATION_CLEAR_BEFORE_USE, clearBeforeUse.getSelection());
	}

	@Override
	public boolean isValid(ILaunchConfiguration config, Consumer<String> messageAcceptor) {
		if (available && targetLocation.getText().equals("")) {
			messageAcceptor
					.accept("Choose a target location to copy files to, either absoluate or relative to workspace.");
			return false;
		}
		return true;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		targetLocation.setText(LocalPreparationTarget.defaultLocalPreparation());
		clearBeforeUse.setSelection(false);
	}

	@Override
	public void setAvailable(boolean available) {
		this.available = available;
		updateState();
	}

	private void updateState() {
		targetLocation.setEnabled(available);
		fileSystem.setEnabled(available);
		pathLabel.setEnabled(available);
		clearBeforeUse.setEnabled(available);
		workspace.setEnabled(available);
	}

}
