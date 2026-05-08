package uk.co.bithatch.eclipzx.ui.glue;

import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_BASE_ON_NEXT_ZXOS;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_RESET_IMAGE_STATE;

import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import uk.co.bithatch.zxbasic.ui.api.ILaunchPreparationUI;
import uk.co.bithatch.zxbasic.ui.api.IPreparationTargetUI;

public class AutomaticFATPreparationTargetUI implements IPreparationTargetUI {

	private Button baseOnNextZXOS;
	private Button resetImageState;
	private boolean available;

	@Override
	public void createControls(Composite parent, Runnable onUpdate) {

		var grid = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(1, false);
		layout.horizontalSpacing = 24;
		layout.verticalSpacing = 2;
		grid.setLayout(layout);

		baseOnNextZXOS = new Button(grid, SWT.CHECK);
		baseOnNextZXOS.setText("Base on Next ZXOS FAT image (see Preferences -> Emulator Resources for location of image)");
		baseOnNextZXOS.setLayoutData(swtDefaults().create());
		baseOnNextZXOS.addSelectionListener(widgetSelectedAdapter(e -> {
			onUpdate.run();
		}));

		resetImageState = new Button(grid, SWT.CHECK);
		resetImageState.setText("Reset FAT image to original state before each use");
		resetImageState.setLayoutData(swtDefaults().create());
		resetImageState.addSelectionListener(widgetSelectedAdapter(e -> {
			onUpdate.run();
		}));
		updateState();
	}

	@Override
	public void init(ILaunchPreparationUI prepUi) {
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		baseOnNextZXOS.setSelection(config.getAttribute(PREPARATION_BASE_ON_NEXT_ZXOS, true));
		resetImageState.setSelection(config.getAttribute(PREPARATION_RESET_IMAGE_STATE, true));
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(PREPARATION_BASE_ON_NEXT_ZXOS, baseOnNextZXOS.getSelection());
		config.setAttribute(PREPARATION_RESET_IMAGE_STATE, resetImageState.getSelection());
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		baseOnNextZXOS.setSelection(true);
		resetImageState.setSelection(false);
	}

	@Override
	public void setAvailable(boolean available) {
		this.available = available;
		updateState();
	}

	private void updateState() {
		baseOnNextZXOS.setEnabled(available);
		resetImageState.setEnabled(available);
	}

	@Override
	public boolean isValid(ILaunchConfiguration config, Consumer<String> messageAcceptor) {
		return true;
	}

}
