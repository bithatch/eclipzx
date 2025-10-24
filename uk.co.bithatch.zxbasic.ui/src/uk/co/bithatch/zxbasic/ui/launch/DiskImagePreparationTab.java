package uk.co.bithatch.zxbasic.ui.launch;

import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_SOURCE_IDS;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_TARGET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;
import uk.co.bithatch.zxbasic.ui.api.ILaunchPreparationUI;
import uk.co.bithatch.zxbasic.ui.api.IPreparationSourceUI;
import uk.co.bithatch.zxbasic.ui.api.IPreparationTargetUI;
import uk.co.bithatch.zxbasic.ui.preparation.PreparationSourceDescriptor;
import uk.co.bithatch.zxbasic.ui.preparation.PreparationSourceRegistry;
import uk.co.bithatch.zxbasic.ui.preparation.PreparationTargetDescriptor;
import uk.co.bithatch.zxbasic.ui.preparation.PreparationTargetRegistry;

public class DiskImagePreparationTab extends AbstractLaunchConfigurationTab implements ILaunchPreparationUI {
	
	private final static ILog LOG = ILog.of(DiskImagePreparationTab.class);

	protected Text emulatorLocation;
	private ExternalEmulatorLaunchConfigurationTab launchTab;
	private Button noPreparation;
	private List<Button> sourceSelectors = new ArrayList<>();
	private Map<Button, IPreparationSourceUI> sourceUIs = new HashMap<>();
	private List<Button> targetSelectors = new ArrayList<>();
	private Map<Button, IPreparationTargetUI> targetUIs = new HashMap<>();

	@Override
	public void createControl(Composite parent) {
		var preparationParent = new Composite(parent, SWT.NONE);
		setControl(preparationParent);
		var layout = new GridLayout(1, false);
		layout.horizontalSpacing = layout.verticalSpacing = 8;
		preparationParent.setLayout(layout);

		var targetParent = new Group(preparationParent, SWT.TITLE);
		targetParent.setLayoutData(fillDefaults().grab(true, true).create());
		targetParent.setText("Target");
		var targetLayout = new GridLayout(3, false);
		targetLayout.horizontalSpacing = targetLayout.verticalSpacing = 8;
		targetParent.setLayout(targetLayout);

		noPreparation = new Button(targetParent, SWT.RADIO);
		noPreparation.setText("No preparation");
		noPreparation.setToolTipText("Do not perform any additional preparation before the launch");
		noPreparation.setLayoutData(fillDefaults().span(3, 1).grab(true, false).create());
		noPreparation.addSelectionListener(widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
		
		for(var targetDescriptor : PreparationTargetRegistry.descriptors()) {
			var customTarget = new Button(targetParent, SWT.RADIO);
			customTarget.setText(targetDescriptor.name());
			customTarget.setData(targetDescriptor);
			customTarget.setLayoutData(fillDefaults().span(3, 1).grab(true, false).create());
			customTarget.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
			targetSelectors.add(customTarget);
			
			try {
				targetDescriptor.createTargetUI().ifPresent(ui -> {
					var wrapper = new Composite(targetParent, SWT.NONE);
					wrapper.setLayout(new FillLayout());
					wrapper.setLayoutData(fillDefaults().span(3, 1).grab(true, false).indent(16, 0).create());
					ui.createControls(wrapper, () -> updateLaunchConfigurationDialog());
					targetUIs.put(customTarget, ui);
				});
			} catch (CoreException e1) {
				LOG.error("Failed to create preparation target UI.", e1);
			}
		}

		var sourceParent = new Group(preparationParent, SWT.TITLE);
		sourceParent.setLayoutData(fillDefaults().grab(true, true).create());
		sourceParent.setText("Source");
		var sourceLayout = new GridLayout(3, false);
		sourceLayout.horizontalSpacing = sourceLayout.verticalSpacing = 8;
		sourceParent.setLayout(sourceLayout);
		
		for(var sourceDescriptor : PreparationSourceRegistry.descriptors()) {
			var customSource = new Button(sourceParent, SWT.CHECK);
			customSource.setText(sourceDescriptor.name());
			customSource.setData(sourceDescriptor);
			if(!sourceDescriptor.description().equals("")) {
				customSource.setToolTipText(sourceDescriptor.description());
			}
			customSource.setLayoutData(fillDefaults().span(3, 1).grab(true, false).create());
			customSource.addSelectionListener(widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
			sourceSelectors.add(customSource);
			
			try {
				sourceDescriptor.createSourceUI().ifPresent(ui -> {
					ui.init(this);
					
					var wrapper = new Composite(sourceParent, SWT.NONE);
					wrapper.setLayout(new FillLayout());
					wrapper.setLayoutData(fillDefaults().span(3, 1).grab(true, false).indent(16, 0).create());
					ui.createControls(wrapper, () -> updateLaunchConfigurationDialog());
					sourceUIs.put(customSource, ui);
				});
			} catch (CoreException e1) {
				LOG.error("Failed to create preparation source UI.", e1);
			}
		}
		
		updateLaunchConfigurationDialog();
	}

	@Override
	public Image getImage() {
		return ZXBasicUiActivator.getInstance().getImageRegistry().get(ZXBasicUiActivator.PREPARE_PATH);
	}

	@Override
	public String getName() {
		return "Preparation";
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
        try 
        {
        	for(var ui : targetUIs.values())
    			ui.initializeFrom(configuration);
        	
        	for(var ui : sourceUIs.values())
    			ui.initializeFrom(configuration);
    		
    		var target  = configuration.getAttribute(PREPARATION_TARGET, "");
    		if(target.equals("")) {
    			noPreparation.setSelection(true);
    			for(var btn : targetSelectors) {
    				btn.setSelection(false);
    			}
    		}
    		else {
    			noPreparation.setSelection(false);
    			for(var btn : targetSelectors) {
    				var descriptor = (PreparationTargetDescriptor)btn.getData();
   					btn.setSelection(target.equals(descriptor.id()));
    			}
    		}

    		var sourceIds = Arrays.asList(configuration.getAttribute(PREPARATION_SOURCE_IDS, defaultDescriptors()).split(";")).
    				stream().
    				filter(s -> !s.equals("")).
    				toList();
    				
    		for(var btn :sourceSelectors) {
				var descriptor = (PreparationSourceDescriptor)btn.getData();
				btn.setSelection(sourceIds.contains(descriptor.id()));
    		}
    		
    		
        } catch (Exception e) {
            setErrorMessage("Could not initialize fields: " + e.getMessage());
        }
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		var ui = getSelectedUI();
		if(ui != null && !ui.isValid(launchConfig, this::setErrorMessage)) {
			return false;
		}
		return super.isValid(launchConfig);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		var ui = getSelectedUI();
		if(ui != null) {
			ui.performApply(configuration);
		}
		
		if(noPreparation.getSelection()) {
			configuration.setAttribute(PREPARATION_TARGET, "");
		}
		else {
			for(var btn : targetSelectors) {
				if(btn.getSelection()) {
					configuration.setAttribute(PREPARATION_TARGET, ((PreparationTargetDescriptor)btn.getData()).id());
				}
			}
		}
		
		configuration.setAttribute(PREPARATION_SOURCE_IDS, 
				String.join(";", sourceSelectors.stream().
						filter(Button::getSelection).
						map(b -> (PreparationSourceDescriptor)((Button)b).getData()).
						map(b -> b.id()).
						toList()));
	}

	@Override
	public IProject resolveProject() {
		return launchTab.resolveProject();
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		targetUIs.values().forEach(ui -> ui.setDefaults(configuration));
		sourceUIs.values().forEach(ui -> ui.setDefaults(configuration));
        configuration.setAttribute(PREPARATION_TARGET, "");
        configuration.setAttribute(PREPARATION_SOURCE_IDS, 
        		defaultDescriptors());
	}

	@Override
	public void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
		
		var sources = isSourcesSet();

		var selUi = getSelectedUI();
		for(var button : targetSelectors) {
			button.setEnabled(sources);
			var ui = targetUIs.get(button);
			if(ui != null)
				ui.setAvailable(ui == selUi && sources);
		}
		

		for(var button : sourceSelectors) {
			var ui = sourceUIs.get(button);
			if(ui != null)
				ui.setAvailable(button.getSelection());
		}
	}

	protected String defaultDescriptors() {
		return String.join(";", PreparationSourceRegistry.descriptors().
				stream().
				filter(d -> d.selected()).
				map(d -> d.id()).
				toList());
	}

	protected boolean isSourcesSet() {
		return sourceSelectors.stream().filter(Button::getSelection).findFirst().isPresent();
	}

	
	void setLaunchTab(ExternalEmulatorLaunchConfigurationTab launchTab) {
		this.launchTab = launchTab;
	}
	
	private IPreparationTargetUI getSelectedUI() {

		for(var button : targetSelectors) {
			if(button.getSelection()) {
				var ui = targetUIs.get(button);
				if(ui != null)
					return ui;
			}
		}
		return null;
	}
}