package uk.co.bithatch.emuzx.ui;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CONFIGURATION_CONTENT;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CONFIGURATION_FILE;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class EmulatorConfigurationFileTab extends AbstractLaunchConfigurationTab {

	private Text configurationFileContent;
	private Button variablesButton;
	protected Text configurationLocation;


	@Override
	public void createControl(Composite parent) {
		var emulatorParent = new Composite(parent, SWT.NONE);
		setControl(emulatorParent);
		var layout = new GridLayout(3, false);
		layout.horizontalSpacing = layout.verticalSpacing = 8;
		emulatorParent.setLayout(layout);

		createEmulatorSelector(emulatorParent);
		;

		var argsLabel = new Label(emulatorParent, SWT.NONE);
		argsLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.TOP).grab(false, true).create());
		argsLabel.setText("Configuration File:");

		configurationFileContent = new Text(emulatorParent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		configurationFileContent.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).span(1, 1).create());
		configurationFileContent.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});

		variablesButton = new Button(emulatorParent, SWT.PUSH);
		variablesButton.setText("Variables");
		variablesButton.setLayoutData(
				GridDataFactory.defaultsFor(variablesButton).align(SWT.LEFT, SWT.TOP).grab(false, true).create());
		variablesButton.addListener(SWT.Selection, e -> {
			var dialog = new StringVariableSelectionDialog(configurationFileContent.getShell());
			if (dialog.open() == Window.OK) {
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					configurationFileContent.insert(variable);
				}
			}
		});
	}

	@Override
	public Image getImage() {
		return Activator.getDefault().getImageRegistry().get(Activator.CONFIG_FILE_PATH);
	}

	protected void createEmulatorSelector(Composite parent) {
		var label = new Label(parent, SWT.NONE);
		label.setText("Configuration Content:");

		configurationLocation = new Text(parent, SWT.BORDER);
		configurationLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		configurationLocation.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});

		var browse = new Button(parent, SWT.PUSH);
		browse.setText("Browse...");
		browse.setLayoutData(GridDataFactory.defaultsFor(browse).create());
		browse.addSelectionListener(widgetSelectedAdapter(e -> {
			var dialog = new FileDialog(getShell(), SWT.OPEN);
			var result = dialog.open();
			if (result != null) {
				configurationLocation.setText(result);
			}
		}));
	}

	@Override
	public String getName() {
		return "Config. File";
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CONFIGURATION_FILE, "");
		configuration.setAttribute(CONFIGURATION_CONTENT, "");
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			configurationLocation.setText(configuration.getAttribute(CONFIGURATION_FILE, ""));
			configurationFileContent.setText(configuration.getAttribute(CONFIGURATION_CONTENT, ""));

		} catch (Exception e) {
			setErrorMessage("Could not initialize fields: " + e.getMessage());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CONFIGURATION_FILE, configurationLocation.getText());
		configuration.setAttribute(CONFIGURATION_CONTENT, configurationFileContent.getText());

	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		if ((!configurationLocation.getText().isEmpty() && !configurationFileContent.getText().isEmpty())) {
			setErrorMessage("You can either have configuration content or a configuration file, not both.");
			return false;
		}
		return super.isValid(launchConfig);
	}


}