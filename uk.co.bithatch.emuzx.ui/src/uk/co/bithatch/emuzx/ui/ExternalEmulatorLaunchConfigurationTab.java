package uk.co.bithatch.emuzx.ui;

import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.OUTPUT_FORMAT;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.emuzx.ExternallyLaunchableRegistry;
import uk.co.bithatch.widgetzx.AbstractLaunchProgramConfigurationTab;

public class ExternalEmulatorLaunchConfigurationTab extends AbstractLaunchProgramConfigurationTab {

	private record OutputFormatWrapper(IOutputFormat format, IOutputFormat projectFormat) {
		String description() {
			return format == null ? "Same as project or workspace (" + projectFormat.description() + ")" : format.fullDescription();
		}

		String value() {
			return format == null ? "" : format.name();
		}
	}

	private Combo outputFormatCombo;
	private ExternalEmulatorConfigurationTab emulatorTab;
	private ArrayList<OutputFormatWrapper> wrappers;

	public ExternalEmulatorLaunchConfigurationTab() {
		super(ExternalEmulatorLaunchConfigurationAttributes.PROJECT,
				ExternalEmulatorLaunchConfigurationAttributes.PROGRAM,
				ExternalEmulatorLaunchConfigurationAttributes.LANGUAGE);
	}

	void setEmulatorTab(ExternalEmulatorConfigurationTab emulatorTab) {
		this.emulatorTab = emulatorTab;
	}

	@Override
	protected void createAdditionalControls(Composite parent) {
		createOutputFormatControl(parent);

	}

	@Override
	protected void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
		emulatorTab.getWorkingDir().defaultDirContainer(resolveProjectPath());
		rebuildOutputFormats();
	}

	@Override
	protected void architectureChanged() {
		rebuildOutputFormats();
	}

	protected void createOutputFormatControl(Composite parent) {
		Label formatLabel = new Label(parent, SWT.NONE);
		formatLabel.setText("Output Format:");

		outputFormatCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		outputFormatCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		outputFormatCombo
				.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
		rebuildOutputFormats();
	}

	private void rebuildOutputFormats() {
		wrappers = new ArrayList<OutputFormatWrapper>();
		var project = resolveProject();
		var selIdx = outputFormatCombo.getSelectionIndex();
		var selText = selIdx == -1 ? null : outputFormatCombo.getItem(selIdx);
		
		try {
			var program = resolveProgram();
			if(program != null) {
				var externallyLaunchable = ExternallyLaunchableRegistry.externallyLaunchableFor(program);
				var system = resolveArchitecture();
				
				outputFormatCombo.setEnabled(true);
				wrappers.add(new OutputFormatWrapper(null, externallyLaunchable.getOutputFormat(project)));
				wrappers.addAll(
						system.supportedFormats().stream().
						map(f -> new OutputFormatWrapper(f, null)).
						toList());
		
				var newItems = wrappers.stream().map(OutputFormatWrapper::description).toList();
				outputFormatCombo.setItems(newItems.toArray(new String[0]));
		
				if (newItems.contains(selText)) {
					outputFormatCombo.select(newItems.indexOf(selText));
				} else {
					outputFormatCombo.select(0);
				}
				
				return;
			}
			
		}
		catch(CoreException ce) {
			ce.printStackTrace();
		}
		

		outputFormatCombo.setItems(new String[0]);

	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		configuration.setAttribute(OUTPUT_FORMAT, "");
		if(outputFormatCombo != null) {
			rebuildOutputFormats();
		}
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		super.initializeFrom(configuration);
		try {
			rebuildOutputFormats();
			var fmtName = configuration.getAttribute(OUTPUT_FORMAT, "");
			for (var i = 0; i < wrappers.size(); i++) {
				var fmt = wrappers.get(i);
				if (fmt.value().equals(fmtName)) {
					outputFormatCombo.select(i);
					break;
				}
			}
			if (outputFormatCombo.getSelectionIndex() == -1)
				outputFormatCombo.select(0);
		} catch (Exception e) {
			setErrorMessage("Could not initialize fields: " + e.getMessage());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		super.performApply(configuration);
		configuration.setAttribute(OUTPUT_FORMAT, outputFormatCombo.getSelectionIndex() < 1 ? ""
				: wrappers.get(outputFormatCombo.getSelectionIndex()).value());
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		if (outputFormatCombo.getText().isEmpty()) {
			setErrorMessage("Output format must be selected.");
			return false;
		}
		return super.isValid(launchConfig);
	}
}