package uk.co.bithatch.zxbasic.ui.launch;

import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.DEBUGGER_EMULATOR_ARGS;
import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.PORT;
import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.START_SUSPENDED;

import java.util.Collections;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import uk.co.bithatch.bitzx.Strings;
import uk.co.bithatch.zyxy.dezog.DezogClient;

public class DebugConfigurationTab extends AbstractLaunchConfigurationTab {

	private Text argsText;
	private Button variablesButton;
	private Spinner port;
	private Button startSuspended;


	@Override
	public void createControl(Composite parent) {
		var debugContainer = new Composite(parent, SWT.NONE);
		setControl(debugContainer);
		var layout = new GridLayout(1, false);
		debugContainer.setLayout(layout);

		var emulatorParent = new Group(debugContainer, SWT.TITLE);
		emulatorParent.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		emulatorParent.setText("Emulator");
		
		var emulatorLayout = new GridLayout(3, false);
		emulatorLayout.horizontalSpacing = emulatorLayout.verticalSpacing = 8;
		emulatorParent.setLayout(emulatorLayout);

		var argsLabel = new Label(emulatorParent, SWT.NONE);
		argsLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.TOP).grab(false, true).create());
		argsLabel.setText("Emulator Arguments:");

		argsText = new Text(emulatorParent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		argsText.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		argsText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});

		variablesButton = new Button(emulatorParent, SWT.PUSH);
		variablesButton.setText("Variables");
		variablesButton
				.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.TOP).grab(false, true).create());
		variablesButton.addListener(SWT.Selection, e -> {
			StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(argsText.getShell());
			if (dialog.open() == Window.OK) {
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					argsText.insert(variable);
				}
			}
		});
		
		var debuggerParent = new Group(debugContainer, SWT.TITLE);
		debuggerParent.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		debuggerParent.setText("Debugger");
		
		var debuggerLayout = new GridLayout(3, false);
		debuggerLayout.horizontalSpacing = emulatorLayout.verticalSpacing = 8;
		debuggerParent.setLayout(debuggerLayout);
		
		var portLabel = new Label(debuggerParent, SWT.NONE);
		portLabel.setText("Port:");
		
		port = new Spinner(debuggerParent, SWT.NONE);
		port.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
		port.setValues(0, 0, 65535, 0, 1, 256);
		
		startSuspended = new Button(debuggerParent, SWT.CHECK);
		startSuspended.setText("Start Suspended");
		startSuspended.setToolTipText("When selected, the program will be suspended on lanch. When deselected, the program will run until the first breakpoint or watchpoint.");
		startSuspended.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(3, 1).create());
		
		updateLaunchConfigurationDialog();
	}

	@Override
	public Image getImage() {
		return DebugUITools.getImage(IDebugUIConstants.IMG_ACT_DEBUG);
	}

	@Override
	protected void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
//		super.setDefaults(configuration);
		configuration.setAttribute(PORT, DezogClient.DEFAULT_PORT);
		configuration.setAttribute(START_SUSPENDED, false);
		configuration.setAttribute(DEBUGGER_EMULATOR_ARGS, Collections.emptyList());
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			startSuspended.setSelection(configuration.getAttribute(START_SUSPENDED, false));
			port.setSelection(configuration.getAttribute(PORT, DezogClient.DEFAULT_PORT));
			argsText.setText(String.join(System.lineSeparator(),
					configuration.getAttribute(DEBUGGER_EMULATOR_ARGS, Collections.emptyList())));
		} catch (Exception e) {
			setErrorMessage("Could not initialize fields: " + e.getMessage());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(START_SUSPENDED, startSuspended.getSelection());
		configuration.setAttribute(PORT, port.getSelection());
		configuration.setAttribute(DEBUGGER_EMULATOR_ARGS, 
				Strings.separatedList(argsText.getText().trim(), System.lineSeparator()));
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
//		setErrorMessage(null);
//		if (outputFormatCombo.getText().isEmpty()) {
//			setErrorMessage("Output format must be selected.");
//			return false;
//		}
		return super.isValid(launchConfig);
	}

	@Override
	public String getName() {
		return "Debugger";
	}
}