package uk.co.bithatch.emuzx.ui;

import static java.lang.System.lineSeparator;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
import static uk.co.bithatch.bitzx.Strings.separatedList;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.AUTOCONFIGURED;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CUSTOM_WORKING_DIRECTORY;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.EMULATOR_ARGS;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.EMULATOR_EXECUTABLE;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.WORKING_DIRECTORY_LOCATION;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import uk.co.bithatch.bitzx.Strings;
import uk.co.bithatch.emuzx.EmulatorRegistry;
import uk.co.bithatch.emuzx.PreferenceConstants;
import uk.co.bithatch.emuzx.PreferencesAccess;

public class ExternalEmulatorConfigurationTab extends AbstractLaunchConfigurationTab {

	private Text argsText;
	private Button variablesButton;
	protected Text emulatorLocation;

	private WorkingDirectorySelector workingDir;
	private boolean adjusting;
	private boolean autoconfigured;
	private String mode;
	private ILaunchConfiguration configuration;
	private LaunchConfigurationContext context;

	public ExternalEmulatorConfigurationTab(String mode, LaunchConfigurationContext context) {
		this.mode = mode;
		this.context = context;
	}

	@Override
	public void createControl(Composite parent) {
		var emulatorParent = new Composite(parent, SWT.NONE);
		setControl(emulatorParent);
		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = layout.verticalSpacing = 8;
		emulatorParent.setLayout(layout);

		createEmulatorSelector(emulatorParent);
		;

		var argsLabel = new Label(emulatorParent, SWT.NONE);
		argsLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.TOP).grab(false, true).create());
		argsLabel.setText("Emulator Arguments:");

		argsText = new Text(emulatorParent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		argsText.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).span(2, 1).create());
		argsText.addModifyListener(new ModifyListener() {
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
			var dialog = new StringVariableSelectionDialog(argsText.getShell());
			if (dialog.open() == Window.OK) {
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					argsText.insert(variable);
				}
			}
		});

		var workingDirGroup = new Group(emulatorParent, SWT.TITLE);
		workingDirGroup.setLayout(new FillLayout());
		workingDirGroup.setText("Working Directory");
		workingDirGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(4, 1).create());
		workingDir = new WorkingDirectorySelector(workingDirGroup, SWT.NONE, context.resolveProjectPath(),
				ResourcesPlugin.getWorkspace().getRoot(), () -> {
					updateLaunchConfigurationDialog();
				});

		adjusting = false;
	}

	@Override
	public Image getImage() {
		return Activator.getDefault().getImageRegistry().get(Activator.CHIP_PATH);
	}

	protected void createEmulatorSelector(Composite parent) {
		var label = new Label(parent, SWT.NONE);
		label.setText("Emulator:");

		emulatorLocation = new Text(parent, SWT.BORDER);
		emulatorLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		emulatorLocation.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
				var path = emulatorPath();
				if (!adjusting && !autoconfigured && path.toFile().exists()) {
					emulatorChosen(path);
				}
			}
		});

		var select = new Button(parent, SWT.PUSH);
		select.setText("Select");
		select.setLayoutData(GridDataFactory.defaultsFor(select).create());
		select.addSelectionListener(widgetSelectedAdapter(e -> selectEmulator()));

		var browse = new Button(parent, SWT.PUSH);
		browse.setText("Browse...");
		browse.setLayoutData(GridDataFactory.defaultsFor(select).create());
		browse.addSelectionListener(widgetSelectedAdapter(e -> {
			var dialog = new FileDialog(getShell(), SWT.OPEN);
			var result = dialog.open();
			if (result != null) {
				emulatorLocation.setText(result);
			}
		}));
	}

	@Override
	public String getName() {
		return "Emulator";
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(EMULATOR_EXECUTABLE, "");
		configuration.setAttribute(EMULATOR_ARGS, Collections.emptyList());
		configuration.setAttribute(CUSTOM_WORKING_DIRECTORY, false);
		configuration.setAttribute(WORKING_DIRECTORY_LOCATION, "");
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			this.configuration = configuration;
			adjusting = true;
			autoconfigured = configuration.getAttribute(AUTOCONFIGURED, false);
			emulatorLocation.setText(configuration.getAttribute(EMULATOR_EXECUTABLE, ""));

			var largs = configuration.getAttribute(EMULATOR_ARGS, Collections.emptyList());
			argsText.setText(String.join(System.lineSeparator(), largs));

			var customWorkingDir = configuration.getAttribute(CUSTOM_WORKING_DIRECTORY, false);
			workingDir.workingDirectory(customWorkingDir
					? configuration.getAttribute(WORKING_DIRECTORY_LOCATION, System.getProperty("user.home"))
					: null);

		} catch (Exception e) {
			setErrorMessage("Could not initialize fields: " + e.getMessage());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(AUTOCONFIGURED, autoconfigured);
		configuration.setAttribute(EMULATOR_EXECUTABLE, emulatorLocation.getText());
		configuration.setAttribute(EMULATOR_ARGS, Strings.separatedList(argsText.getText(), System.lineSeparator()));

		var wd = workingDir.workingDirectory();
		if (wd == null) {
			configuration.setAttribute(CUSTOM_WORKING_DIRECTORY, false);
			configuration.setAttribute(WORKING_DIRECTORY_LOCATION, "");
		} else {
			configuration.setAttribute(CUSTOM_WORKING_DIRECTORY, true);
			configuration.setAttribute(WORKING_DIRECTORY_LOCATION, wd);
		}

	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		if (emulatorLocation.getText().isEmpty()) {
			setErrorMessage("Emulator executor must be selected.");
			return false;
		}
		return super.isValid(launchConfig);
	}

	private void selectEmulator() {
		var ld = new EmulatorSelectorDialog(getShell());
		autoconfigured = true;
		if (ld.open() == Window.OK) {
			configureEmulator(ld);

		}
	}

	private void emulatorChosen(IPath emulatorPath) {
		var matches = EmulatorRegistry.descriptors().stream().filter(em -> em.matches(emulatorPath.toString()))
				.toList();
		if (matches.size() > 0) {

			var ld = new EmulatorSelectorDialog(getShell());
			ld.setInput(matches);
			ld.setInitialSelections(matches.get(0));
			ld.setMessage("You have chosen a supported emulator, do you wish to\n" + "automatically configure it?");
			autoconfigured = true;
			if (ld.open() == Window.OK) {
				configureEmulator(ld);
			}
		}
	}

	private void configureEmulator(EmulatorSelectorDialog ld) {
		var sel = ld.getSelectedEmulator();
		try {
			// TODO
			var cfg = (ILaunchConfigurationWorkingCopy) configuration/* .getWorkingCopy() */;
			cfg.setAttribute(EMULATOR_EXECUTABLE, ld.getEmulatorExecutable().toString());

			sel.createEmulator().ifPresent(em -> {
				em.configure(sel, cfg, context.resolveProgram(), ld.getEmulatorHome(), mode);
			});

			var preconfigured = new ArrayList<>(
					cfg.getAttribute(EMULATOR_ARGS, separatedList(argsText.getText(), lineSeparator())));
			var leading = new ArrayList<>(
					separatedList(
							PreferencesAccess.get().getPreferences()
									.get(sel.getIdOrDefault(Activator.PLUGIN_ID) + "."
											+ PreferenceConstants.EXTERNAL_EMULATOR_LEADING_OPTIONS, ""),
							lineSeparator()));
			var trailing = new ArrayList<>(
					separatedList(
							PreferencesAccess.get().getPreferences()
									.get(sel.getIdOrDefault(Activator.PLUGIN_ID) + "."
											+ PreferenceConstants.EXTERNAL_EMULATOR_TRAILING_OPTIONS, ""),
							lineSeparator()));

			leading.removeAll(preconfigured);
			trailing.removeAll(preconfigured);
			preconfigured.addAll(0, leading);
			preconfigured.addAll(trailing);

			cfg.setAttribute(EMULATOR_ARGS, preconfigured);

			var newCfg = cfg.doSave().getWorkingCopy();
			initializeFrom(newCfg);

			context.initializeFrom(cfg);
//			}
			updateLaunchConfigurationDialog();
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	private IPath emulatorPath() {
		return IPath.fromOSString(emulatorLocation.getText());
	}

	public WorkingDirectorySelector getWorkingDir() {
		return workingDir;
	}

}