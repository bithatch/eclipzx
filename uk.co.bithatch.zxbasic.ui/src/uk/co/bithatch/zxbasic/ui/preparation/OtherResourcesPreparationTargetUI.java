package uk.co.bithatch.zxbasic.ui.preparation;

import static org.eclipse.jface.layout.GridDataFactory.defaultsFor;
import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_OTHER_FILES;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;

import uk.co.bithatch.zxbasic.ui.api.ILaunchPreparationUI;
import uk.co.bithatch.zxbasic.ui.api.IPreparationSourceUI;

public class OtherResourcesPreparationTargetUI implements IPreparationSourceUI {

	private boolean available;
	private Text otherFiles;
	private Button variablesButton;
	private Button selectOtherFiles;
	private ILaunchPreparationUI prepUi;

	@Override
	public void init(ILaunchPreparationUI prepUi) {
		this.prepUi = prepUi;
	}

	@Override
	public void createControls(Composite parent, Runnable onUpdate) {

		var grid = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = 24;
		layout.verticalSpacing = 8;
		grid.setLayout(layout);
		
		otherFiles = new Text(grid, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		otherFiles.setLayoutData(fillDefaults().span(2, 3).grab(true, true).create());
		otherFiles.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				prepUi.updateLaunchConfigurationDialog();
			}
		});

		var otherFilesButtons = new Composite(grid, SWT.NONE);
		otherFilesButtons.setLayout(new GridLayout(1, true));
		otherFilesButtons.setLayoutData(fillDefaults().align(SWT.LEFT, SWT.TOP).grab(false, true).create());

		variablesButton = new Button(otherFilesButtons, SWT.PUSH);
		variablesButton.setText("Variables");
		variablesButton.addListener(SWT.Selection, e -> {
			var dialog = new StringVariableSelectionDialog(parent.getShell());
			if (dialog.open() == Window.OK) {
				var variable = dialog.getVariableExpression();
				if (variable != null) {
					otherFiles.insert(variable);
				}
			}
		});
		variablesButton.setLayoutData(fillDefaults().grab(true, true).create());

		selectOtherFiles = new Button(otherFilesButtons, SWT.PUSH);
		selectOtherFiles.setText("Select");
		selectOtherFiles.setLayoutData(defaultsFor(selectOtherFiles).create());
		selectOtherFiles.addListener(SWT.Selection, e -> {
			var elementDialog = new ResourceSelectionDialog(parent.getShell(), prepUi.resolveProject(),
					"Select resources to copy to prepare emulator launch.");
			if (elementDialog.open() == Window.OK) {
				var result = elementDialog.getResult();
				if (result != null) {
					addOtherFiles(Arrays.asList((Object[]) result).stream().map(o -> ((IResource)o)).toList());
				}
			}
		});
		
		updateState();
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		otherFiles.setText(String.join(System.lineSeparator(), config.getAttribute(PREPARATION_OTHER_FILES, Collections.emptyList())));
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(PREPARATION_OTHER_FILES, 
				Arrays.asList(otherFiles.getText().equals("") ? new String[0] : otherFiles.getText().split(System.lineSeparator())));
	}

	@Override
	public boolean isValid(ILaunchConfiguration config, Consumer<String> messageAcceptor) {

		if(otherFiles.getText().trim().equals("")) {
			messageAcceptor.accept("No other files selected to copy.");
            return false;
		}

		return true;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(PREPARATION_OTHER_FILES, "");
	}

	@Override
	public void setAvailable(boolean available) {
		this.available = available;
		updateState();
	}

	protected void addOtherFiles(List<IResource> resources) {
		otherFiles.setText(String.join(System.lineSeparator(),
				resources.stream().map(res -> res.getRawLocation().toString()).toList()));
	}

	private void updateState() {
		otherFiles.setEnabled(available);
		variablesButton.setEnabled(available);
		selectOtherFiles.setEnabled(available);
	}

}
