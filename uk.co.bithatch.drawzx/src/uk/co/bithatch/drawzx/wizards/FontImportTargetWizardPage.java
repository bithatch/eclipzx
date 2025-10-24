package uk.co.bithatch.drawzx.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

public class FontImportTargetWizardPage extends WizardPage {

	private Text destText;
	private Text nameText;
	private Button overrite;
	private ControlDecoration destDecoration;
	private ControlDecoration nameDecoration;
	private FontImportWizardPage selectionPage;
	private IContainer defaultDestination;
	private boolean defaultNameChosen = true;
	private boolean adjustingName;

	protected FontImportTargetWizardPage(FontImportWizardPage selectionPage, String pageName,
			IContainer defaultDestination) {
		super(pageName);
		setTitle("Imported Font");
		setDescription("Choose where you want your font file imported to.");
		this.selectionPage = selectionPage;
		this.defaultDestination = defaultDestination;
	}

	@Override
	public void createControl(Composite parent) {

		var stateUpdate = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateState();
			}
		};
		var container = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(3, false);
		layout.verticalSpacing = 8;
		layout.horizontalSpacing = 16;
		container.setLayout(layout);

		Label destLabel = new Label(container, SWT.NONE);
		destLabel.setText("Destination folder:");
		destText = new Text(container, SWT.BORDER);
		destText.setText(defaultDestination == null ? "" : defaultDestination.getFullPath().toString());
		destText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		destText.addModifyListener(e -> updateState());

		destDecoration = new ControlDecoration(destText, SWT.LEFT | SWT.TOP);
		destDecoration.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		destDecoration.setDescriptionText("Destination does not exist.");
		destDecoration.hide();

		Button browseDestButton = new Button(container, SWT.PUSH);
		browseDestButton.setText("Browse...");
		browseDestButton.addListener(SWT.Selection, e -> {

			ContainerSelectionDialog projDialog = new ContainerSelectionDialog(getShell(),
					PlatformUI.getWorkbench().getAdapter(IWorkspace.class).getRoot(), true,
					"Select a folder from your workspace");
			if (projDialog.open() == Window.OK) {
				Object[] selected = projDialog.getResult();
				if (selected.length > 0) {
					IPath path = (IPath) selected[0];
					destText.setText(path.toPortableString());
					updateState();
				}
			}
		});

		Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setText("Font filename:");
		nameText = new Text(container, SWT.BORDER);
		nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		nameText.addModifyListener(e -> {
			if (!adjustingName) {
				defaultNameChosen = false;
				updateState();
			}
		});

		nameDecoration = new ControlDecoration(nameText, SWT.LEFT | SWT.TOP);
		palExists();
		nameDecoration.hide();
		new Label(container, SWT.NONE);

		overrite = new Button(container, SWT.CHECK);
		overrite.setText("Overwrite existing file(s)");
		overrite.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
		overrite.addSelectionListener(stateUpdate);

		// Project folder

		setControl(container);
		updateState();
	}

	public IFolder getTargetFolder() {
		return (IFolder) getTargetResource();
	}

	private IResource getTargetResource() {
		return ResourcesPlugin.getWorkspace().getRoot().findMember(destText.getText().trim());
	}

	public IFile getTargetPath() {
		var fname = nameText.getText().trim();
		var extension = stripExtension(fname);
		try {
			var fullPath = getTargetResource();
			var tname = extension.equals(fname) ? fname + ".ch8" : fname;
			if (fullPath instanceof IProject iprj) {
				return iprj.getFile(tname);
			} else if (fullPath instanceof IFolder ifldr) {
				return ifldr.getFile(tname);
			}
		} catch (Exception e) {
		}
		return null;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			if (nameText.getText().equals("") || defaultNameChosen) {
				adjustingName = true;
				try {
					defaultNameChosen = true;
					nameText.setText(selectionPage.getSource().getName() + ".ch8");
				} finally {
					adjustingName = false;
				}
			}
		}
	}

	private void updateState() {
		var targetFile = getTargetPath();
		var targetRes = getTargetResource();
		var targetResExists = targetRes != null && targetRes.exists();
		var targetFolderExists = targetResExists && targetRes instanceof IContainer;
		var overwriteTarget = overrite.getSelection();

		setVisible(destDecoration, !targetFolderExists);
		if (targetFile == null) {
			overrite.setEnabled(false);
			setPageComplete(false);
			if (destText.getText().length() > 0) {
				setErrorMessage("The destination folder does not exist.");
			} else {
				setErrorMessage(null);
			}
		} else {
			overrite.setEnabled(targetFile.exists());
			if (targetFile.exists() && !overwriteTarget) {
				palExists();
				setVisible(nameDecoration, true);
				setPageComplete(false);
				setErrorMessage("Target palette file exists, overwrite or chose a different name");
			} else {
				if (targetFile.getName().toLowerCase().endsWith(".ch8")
						|| targetFile.getName().toLowerCase().endsWith(".udg")) {
					setVisible(nameDecoration, false);
				} else {
					nameDecoration.setImage(
							PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
					nameDecoration.setDescriptionText("Imported font file name should ideally end with .ch8 or .udg.");
					setVisible(nameDecoration, true);
				}
				setPageComplete(targetFolderExists && (!targetFile.exists() || overwriteTarget));
				if (targetFolderExists)
					setErrorMessage(null);
				else
					setErrorMessage("The destination folder does not exist.");
			}
		}
	}

	private static void setVisible(ControlDecoration ctrl, boolean vis) {
		if (vis)
			ctrl.show();
		else
			ctrl.hide();
	}

	private static String stripExtension(String p) {
		var i = p.lastIndexOf('.');
		return i == -1 ? p : p.substring(0, i);
	}

	private void palExists() {
		nameDecoration.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		nameDecoration.setDescriptionText("Target palette file already exists.");
	}
}
