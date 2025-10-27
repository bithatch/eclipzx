package uk.co.bithatch.emuzx.emulator.jspeccy;

import static uk.co.bithatch.emuzx.emulator.jspeccy.EmulatorLaunchConfigurationAttributes.OUTPUT_FORMAT;

import java.util.List;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.WellKnownOutputFormat;
import uk.co.bithatch.widgetzx.AbstracLaunchProgramConfigurationTab;
import uk.co.bithatch.widgetzx.LanguageSystemUI;

public class EmulatorLaunchConfigurationTab extends AbstracLaunchProgramConfigurationTab {

	private Combo outputFormatCombo;

	public EmulatorLaunchConfigurationTab() {
		super(EmulatorLaunchConfigurationAttributes.PROJECT, EmulatorLaunchConfigurationAttributes.PROGRAM, EmulatorLaunchConfigurationAttributes.LANGUAGE);
	}

	@Override
	protected void createAdditionalControls(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText("Output Format:");

		outputFormatCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		outputFormatCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		updateOutputFormats();
		outputFormatCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		updateOutputFormats();
		configuration.setAttribute(OUTPUT_FORMAT, WellKnownOutputFormat.SNA.name());
	}

	@Override
	public void initializeFrom(org.eclipse.debug.core.ILaunchConfiguration configuration) {
		super.initializeFrom(configuration);
		try {
			updateOutputFormats();
			String format = configuration.getAttribute(OUTPUT_FORMAT, WellKnownOutputFormat.SNA.name());
			outputFormatCombo.setText(format);
		} catch (Exception e) {
			setErrorMessage("Could not initialize output format: " + e.getMessage());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		super.performApply(configuration);
		configuration.setAttribute(OUTPUT_FORMAT, outputFormatCombo.getText());
	}

	@Override
	protected void languageChanged() {
		super.languageChanged();
		updateOutputFormats();
	}

	@Override
	protected void projectChanged() {
		super.projectChanged();
		updateOutputFormats();
	}

	@Override
	protected void programChanged() {
		updateOutputFormats();
	}

	protected void updateOutputFormats() {
		if(outputFormatCombo == null)
			return;
		
		var  lang = resolveLanguage();
		if(lang == null)
			outputFormatCombo.setItems();
		else {
			var formats = lang.outputFormats(resolveProject()).stream()
				.filter(f -> Activator.JSPECCY_RUNNABLE_FORMATS.contains(f.extension())).toList();
			outputFormatCombo.setItems(LanguageSystemUI.describedNames(formats));
			if(outputFormatCombo.getSelectionIndex() == -1 && formats.size() > 0) {
				outputFormatCombo.select(0);
			}
		}
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		var val = super.isValid(launchConfig);
		if(val) {
			if (outputFormatCombo.getText().isEmpty()) {
				setErrorMessage("Output format must be selected.");
				return false;
			}
		}
		return val;
	}
}
