package uk.co.bithatch.widgetzx;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import uk.co.bithatch.bitzx.ILanguageSystemProvider;
import uk.co.bithatch.bitzx.LanguageSystem;

public abstract class AbstracLaunchProgramConfigurationTab extends AbstractLaunchConfigurationTab {

	final static DialogSettings settings = new DialogSettings(AbstracLaunchProgramConfigurationTab.class.getName());;

	protected Text projectText;
	protected Text programText;

	private final String projectKey;
	private final String programKey;
	private final String languageKey;

	private Combo languageCombo;

	protected AbstracLaunchProgramConfigurationTab(String projectKey, String programKey, String languageKey) {
		this.programKey = programKey;
		this.projectKey = projectKey;
		this.languageKey = languageKey;
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		var layout = new GridLayout(3, false);
		layout.horizontalSpacing = layout.verticalSpacing = 8;
		comp.setLayout(layout);

		createProjectSelector(comp);
		createLanguageSelector(comp);
		createProgramSelector(comp);

		createAdditionalControls(comp);
	}

	protected void createLanguageSelector(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText("Language:");

		languageCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		languageCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		updateLanguages();
		languageCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				languageChanged();
				updateLaunchConfigurationDialog();
			}
		});
	}

	protected void createProjectSelector(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText("Project:");

		projectText = new Text(parent, SWT.BORDER);
		projectText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		projectText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				projectChanged();
				updateLaunchConfigurationDialog();
			}
		});

		var browse = new Button(parent, SWT.PUSH);
		browse.setLayoutData(GridDataFactory.defaultsFor(browse).create());
		browse.setText("Browse...");
		browse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				var dialog = new ProjectSelectionDialog(getShell(),
						Set.of(ResourcesPlugin.getWorkspace().getRoot().getProjects()).stream().filter(LanguageSystem::isCompatible).collect(Collectors.toSet()), settings, "Compatible Projects Only");

				dialog.setTitle("Select Project");
				if (dialog.open() == ElementListSelectionDialog.OK) {
					IProject project = (IProject) dialog.getFirstResult();
					projectText.setText(project.getName());
					projectChanged();
				}
			}
		});
	}

	protected void projectChanged() {
		updateLanguages();
	}

	protected void createProgramSelector(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText("Program:");

		programText = new Text(parent, SWT.BORDER);
		programText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		programText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				programChanged();
				updateLaunchConfigurationDialog();
			}
		});

		var browse = new Button(parent, SWT.PUSH);
		browse.setText("Browse...");
		browse.setLayoutData(GridDataFactory.defaultsFor(browse).create());
		browse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				var dialog = new ElementTreeSelectionDialog(parent.getShell(), new WorkbenchLabelProvider(),
						new WorkbenchContentProvider());

				var project = resolveProject();
//				var outputFolder = preferenceAccess.getOutputFolder(project);

				dialog.setTitle("Select Source File");
				dialog.setMessage("Choose a .bas or .asm file:");
				dialog.setInput(project); // Root of the selection
				
				try {
					dialog.setInitialSelection(project.getFile(IPath.fromPortableString(programText.getText())));
				}
				catch(IllegalArgumentException iae) {
				}

				// Accept files only
				dialog.addFilter(new ViewerFilter() {
					@Override
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						if(element instanceof IResource res)
							return LanguageSystem.languageSystem(project).isLaunchable(res); 
						else
							return false;
					}
				});

				if (dialog.open() == Window.OK) {
					var selectedFile = (IFile) dialog.getFirstResult();
					var programPath = selectedFile.getFullPath().removeFirstSegments(1).makeRelative();
					programText.setText(programPath.toPortableString());
					programChanged();
				}

			}
		});
	}

	protected void languageChanged() {
	}

	protected void programChanged() {
	}

	protected IPath resolveProjectPath() {
		var path = IPath.fromPortableString(projectText.getText()).makeRelative().addTrailingSeparator();
		var root = ResourcesPlugin.getWorkspace().getRoot();
		return root.getLocation().append(path).removeTrailingSeparator();
	}
	
	protected ILanguageSystemProvider resolveLanguage() {
		return LanguageSystem.languageSystemByName(languageCombo.getText()).orElse(null);
	}

	protected IProject resolveProject() {
		try {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(projectText.getText());
		} catch (IllegalArgumentException iae) {
			return null;
		}
	}

	protected IFile resolveProgram() {
		try {
			var prj = resolveProject();
			if(prj != null) {
				var res = prj.findMember(programText.getText());
				if(res instanceof IFile ifile) {
					return ifile;
				}
			}
		} catch (IllegalArgumentException iae) {
		}
		return null;
	}

	protected abstract void createAdditionalControls(Composite parent);

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(projectKey, "");
		configuration.setAttribute(programKey, "");
		configuration.setAttribute(languageKey, "");
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			projectText.setText(configuration.getAttribute(projectKey, ""));
			programText.setText(configuration.getAttribute(programKey, ""));
			languageCombo.setText(configuration.getAttribute(languageKey, ""));
		} catch (CoreException e) {
			setErrorMessage("Error loading configuration");
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(projectKey, projectText.getText());
		configuration.setAttribute(programKey, programText.getText());
		configuration.setAttribute(languageKey, languageCombo.getText());
	}

	@Override
	public String getName() {
		return "Main";
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		if (projectText.getText().isEmpty()) {
			setErrorMessage("Project must be selected.");
			return false;
		}
		if (languageCombo.getText().isEmpty()) {
			setErrorMessage("Language must be selected.");
			return false;
		}
		if (programText.getText().isEmpty()) {
			setErrorMessage("Program must be selected.");
			return false;
		}
		return super.isValid(launchConfig);
	}

	protected void updateLanguages() {
		var proj = resolveProject();
		var wasSel = languageCombo.getText();
		var newItems = LanguageSystemUI.languageSystemNamesFor(proj);
		languageCombo.setItems(newItems.toArray(new String[0]));
		if(wasSel != null && newItems.contains(wasSel)) {
			languageCombo.select(newItems.indexOf(wasSel));
		}
		else if(newItems.size() > 0) {
			languageCombo.select(0);
		}
		languageCombo.setEnabled(newItems.size() > 1);
	}
}
