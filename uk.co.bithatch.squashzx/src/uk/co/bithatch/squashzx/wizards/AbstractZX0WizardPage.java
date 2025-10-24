package uk.co.bithatch.squashzx.wizards;

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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.internal.ExpandableNode;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public abstract class AbstractZX0WizardPage extends WizardPage {

	protected static void setVisible(ControlDecoration ctrl, boolean vis) {
		if (vis)
			ctrl.show();
		else
			ctrl.hide();
	}

	protected static String stripExtension(String p) {
		var i = p.lastIndexOf('.');
		return i == -1 ? p : p.substring(0, i);
	}

	protected Text destText;
	protected ControlDecoration nameDecoration;
	protected Text nameText;
	protected Button removeSource;

	private Button backwardsMode;
	private Button classicMode;
	private ControlDecoration destDecoration;
	private IFile file;
	private Text fileText;
	private Button overrite;

	private ControlDecoration srcDecoration;

	private final String typeName;
	private String sourceTypeName;

	protected AbstractZX0WizardPage(String pageName, String typeName, String sourceTypeName) {
		super(pageName);
		this.typeName = typeName;
		this.sourceTypeName = sourceTypeName;
	}

	@Override
	public final void createControl(Composite parent) {
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

		var fileLabel = new Label(container, SWT.NONE);
		fileLabel.setText("Source File:");
		fileText = new Text(container, SWT.BORDER);
		fileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		srcDecoration = new ControlDecoration(fileText, SWT.LEFT | SWT.TOP);
		srcDecoration.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		srcDecoration.setDescriptionText("Source file does not exist.");
		srcDecoration.hide();

		var browseButton = new Button(container, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, e -> {

			var dialog = new ElementTreeSelectionDialog(parent.getShell(), new WorkbenchLabelProvider(),
					new WorkbenchContentProvider());

			dialog.setTitle("Select File To " + typeName);
			dialog.setMessage("Select any file to " + typeName.toLowerCase() + " with ZX0:");
			dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
			dialog.setAllowMultiple(false);

			dialog.addFilter(new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (element instanceof IFile file) {
						String ext = file.getFileExtension();
						return ext == null || !(ext.equalsIgnoreCase("zx0"));
					} else if (element instanceof IContainer) {
						return true; // allow folders
					}
					return false;
				}
			});

			if (dialog.open() == Window.OK) {
				fileText.setText(((IFile) dialog.getFirstResult()).getLocation().toString());
			}
		});

		Label destLabel = new Label(container, SWT.NONE);
		destLabel.setText("Destination folder:");
		destText = new Text(container, SWT.BORDER);
		destText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

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
		nameLabel.setText("Filename:");
		nameText = new Text(container, SWT.BORDER);
		nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		if (file != null) {
			fileText.setText(file.getFullPath().toPortableString());
			updateDefaultName(file);
		}

		nameDecoration = new ControlDecoration(nameText, SWT.LEFT | SWT.TOP);
		targetExists();
		nameDecoration.hide();
		new Label(container, SWT.NONE);

		overrite = new Button(container, SWT.CHECK);
		overrite.setText("Overwrite existing file");
		overrite.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());

		removeSource = new Button(container, SWT.CHECK);
		removeSource.setText("Remove original " + sourceTypeName.toLowerCase() + " file");
		removeSource.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());

		new Label(container, SWT.NONE).setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());

		var advanced = new Group(container, SWT.TITLE);
		advanced.setText("Advanced");
		advanced.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
		var advancedLayout = new GridLayout(3, false);
		advancedLayout.verticalSpacing = 8;
		advancedLayout.horizontalSpacing = 16;
		advanced.setLayout(advancedLayout);

		classicMode = new Button(advanced, SWT.CHECK);
		classicMode.setText("Classic Mode");
		classicMode.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());

		backwardsMode = new Button(advanced, SWT.CHECK);
		backwardsMode.setText("Backwards Mode");
		backwardsMode.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());

		// Project folder

		onCreateControl(advanced, stateUpdate);

		setControl(container);
		updateState();

		overrite.addSelectionListener(stateUpdate);
		classicMode.addSelectionListener(stateUpdate);
		backwardsMode.addSelectionListener(stateUpdate);
		nameText.addModifyListener(e -> updateState());
		destText.addModifyListener(e -> updateState());
		fileText.addModifyListener(e -> {
			updateState();
		});
	}

	public int getSkip() {
		return 0;
	}

	public int getThreads() {
		return 0;
	}

	public final IFile getSource() {
		return file;
	}

	public final IFolder getTargetFolder() {
		return (IFolder) getTargetResource();
	}

	public final IFile getTargetPath() {
		var fname = nameText.getText();
		try {
			var fullPath = getTargetResource();
			if (fullPath instanceof IProject iprj) {
				return iprj.getFile(fname);
			} else if (fullPath instanceof IFolder ifldr) {
				return ifldr.getFile(fname);
			}
		} catch (Exception e) {
		}
		return null;
	}

	public final boolean isBackwards() {
		return backwardsMode.getSelection();
	}

	public final boolean isClassic() {
		return classicMode.getSelection();
	}

	public final boolean isOverwrite() {
		return overrite.getSelection();
	}

	public abstract boolean isQuick();

	public final boolean isRemoveSource() {
		return removeSource.getSelection();
	}

	public final void setSource(IFile file) {
		this.file = file;
		if (fileText != null) {
			fileText.setText(file.getFullPath().toPortableString());
			updateDefaultName(file);
			updateState();
		}
	}

	protected abstract void checkTargetExtension(IFile targetFile);

	protected void onCreateControl(Composite parent, SelectionAdapter onSelection) {
	}

	protected abstract void updateDefaultName(IFile file);

	private final IResource getTargetResource() {
		return ResourcesPlugin.getWorkspace().getRoot().findMember(destText.getText().trim());
	}

	private void targetExists() {
		destDecoration.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		destDecoration.setDescriptionText("Target file already exists.");
	}

	private void updateState() {
		var targetFile = getTargetPath();
		var source = getSource();
		var sourceExists = source != null && source.exists();
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
			} else if (!sourceExists) {
				setErrorMessage("The source file does not exist.");
			} else {
				setErrorMessage(null);
			}
		} else {
			overrite.setEnabled(targetFile.exists());
			if (targetFile.exists() && !overwriteTarget) {
				targetExists();
				setVisible(nameDecoration, true);
				setPageComplete(false);
				setErrorMessage("Target file exists, overwrite or chose a different name");
			} else {
				checkTargetExtension(targetFile);
				setPageComplete(sourceExists && targetFolderExists && (!targetFile.exists() || overwriteTarget));
				if (sourceExists) {
					if (targetFolderExists)
						setErrorMessage(null);
					else
						setErrorMessage("The destination folder does not exist.");
				} else {
					setErrorMessage("The source file does not exist.");
				}
			}
		}
	}

}
