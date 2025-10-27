package uk.co.bithatch.jspeccy.wizards;

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

import uk.co.bithatch.jspeccy.views.EmulatorInstance;

public class ExportScreenshotWizardPage extends WizardPage {

	protected Text destText;
	protected ControlDecoration nameDecoration;
	protected Text nameText;

	private ControlDecoration destDecoration;
	private Button overrite;

	private final IContainer container;
	private final EmulatorInstance emulator;

	public ExportScreenshotWizardPage(EmulatorInstance emulator, IContainer container) {
		super("Export Screenshot");
		this.emulator = emulator;
		this.container = container;
		setTitle("Export Screenshot");
		setDescription("Export the emulator screen as an image file.");
	}

	@Override
	public final void createControl(Composite parent) {

			var stateUpdate = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateState();
				}
			};
			
			var composite = new Composite(parent, SWT.NONE);
			var layout = new GridLayout(3, false);
			layout.verticalSpacing = 8;
			layout.horizontalSpacing = 16;
			composite.setLayout(layout);

			Label destLabel = new Label(composite, SWT.NONE);
			destLabel.setText("Destination folder:");
			destText = new Text(composite, SWT.BORDER);
			destText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			destText.setText(container == null ? "" : container.getFullPath().toString());

			destDecoration = new ControlDecoration(destText, SWT.LEFT | SWT.TOP);
			destDecoration
					.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
			destDecoration.setDescriptionText("Destination does not exist.");
			destDecoration.hide();

			var browseDestButton = new Button(composite, SWT.PUSH);
			browseDestButton.setText("Browse...");
			browseDestButton.addListener(SWT.Selection, e -> {

				var projDialog = new ContainerSelectionDialog(getShell(),
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

			Label nameLabel = new Label(composite, SWT.NONE);
			nameLabel.setText("Filename:");
			nameText = new Text(composite, SWT.BORDER);
			nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			nameText.setText(emulator.getFile() == null ? "" : stripExtension(emulator.getFile().getName().toString()) + ".png");

			nameDecoration = new ControlDecoration(nameText, SWT.LEFT | SWT.TOP);
			targetExists();
			nameDecoration.hide();
			new Label(composite, SWT.NONE);

			overrite = new Button(composite, SWT.CHECK);
			overrite.setText("Overwrite existing file");
			overrite.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());

			new Label(composite, SWT.NONE).setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());

			setControl(composite);
			updateState();

			overrite.addSelectionListener(stateUpdate);
			nameText.addModifyListener(e -> updateState());
			destText.addModifyListener(e -> updateState());

	}

	public int getSkip() {
		return 0;
	}

	public int getThreads() {
		return 0;
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

	public final boolean isOverwrite() {
		return overrite.getSelection();
	}

	private final IResource getTargetResource() {
		return ResourcesPlugin.getWorkspace().getRoot().findMember(destText.getText().trim());
	}

	private void targetExists() {
		destDecoration.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		destDecoration.setDescriptionText("Target file already exists.");
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
				targetExists();
				setVisible(nameDecoration, true);
				setPageComplete(false);
				setErrorMessage("Target file exists, overwrite or chose a different name");
			} else {
				setPageComplete(checkTargetExtension(targetFile) && targetFolderExists
						&& (!targetFile.exists() || overwriteTarget));
				if (targetFolderExists)
					setErrorMessage(null);
				else
					setErrorMessage("The destination folder does not exist.");
			}
		}
	}

	protected boolean checkTargetExtension(IFile targetFile) {
		if (targetFile.getName().toLowerCase().endsWith(".png")
				|| targetFile.getName().toLowerCase().endsWith(".scr")) {
			setVisible(nameDecoration, false);
			return true;
		} else {
			nameDecoration
					.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
			nameDecoration.setDescriptionText("Image file name should end with .png or .scr.");
			setVisible(nameDecoration, true);
			return false;
		}
	}

	protected static String stripExtension(String p) {
		var i = p.lastIndexOf('.');
		return i == -1 ? p : p.substring(0, i);
	}

	protected static void setVisible(ControlDecoration ctrl, boolean vis) {
		if (vis)
			ctrl.show();
		else
			ctrl.hide();
	}
}
