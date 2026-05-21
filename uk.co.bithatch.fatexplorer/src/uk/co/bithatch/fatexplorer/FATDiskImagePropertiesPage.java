package uk.co.bithatch.fatexplorer;

import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Property page for managing FAT disk image mounts on a project.
 * Shows only for IProject resources.
 */
public class FATDiskImagePropertiesPage extends PropertyPage {

	private TableViewer tableViewer;
	private List<FATDiskImageMount> mounts;
	private IProject project;
	private final Set<String> pendingMounts = new HashSet<>();

	@Override
	protected Control createContents(Composite parent) {
		var resource = (IResource) getElement().getAdapter(IResource.class);
		if (!(resource instanceof IProject)) {
			var label = new Label(parent, SWT.WRAP);
			label.setText("FAT disk image mounts are only available for projects.");
			return label;
		}

		project = (IProject) resource;
		mounts = new ArrayList<>(FATDiskImageManager.getMounts(project));

		var composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		var infoLabel = new Label(composite, SWT.WRAP);
		infoLabel.setText("Mount FAT16/FAT32 disk images as folders in this project. "
				+ "Mounts appear under \"FAT Disk Images\" in the project explorer. "
				+ "Use Mount/Unmount to connect or disconnect immediately.");
		infoLabel.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false)
				.span(2, 1).hint(400, SWT.DEFAULT).create());

		// Table
		tableViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		var table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 7));

		var mountedCol = new TableViewerColumn(tableViewer, SWT.CENTER);
		mountedCol.getColumn().setText("Mounted");
		mountedCol.getColumn().setWidth(70);
		mountedCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				var name = ((FATDiskImageMount) element).getName();
				if (pendingMounts.contains(name)) return "\u23F3";
				return FATDiskImageManager.isMounted(project, (FATDiskImageMount) element) ? "\u2713" : "";
			}
		});

		var nameCol = new TableViewerColumn(tableViewer, SWT.NONE);
		nameCol.getColumn().setText("Name");
		nameCol.getColumn().setWidth(120);
		nameCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((FATDiskImageMount) element).getName();
			}
		});

		var pathCol = new TableViewerColumn(tableViewer, SWT.NONE);
		pathCol.getColumn().setText("Image Path");
		pathCol.getColumn().setWidth(250);
		pathCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((FATDiskImageMount) element).getImagePath();
			}
		});

		var autoCol = new TableViewerColumn(tableViewer, SWT.CENTER);
		autoCol.getColumn().setText("Auto");
		autoCol.getColumn().setWidth(50);
		autoCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((FATDiskImageMount) element).isAutomount() ? "\u2713" : "";
			}
		});

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setInput(mounts);

		// Buttons
		var addButton = new Button(composite, SWT.PUSH);
		addButton.setText("Add...");
		addButton.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).create());
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				var dlg = new DiskImageDialog(getShell(), project, null);
				if (dlg.open() == IDialogConstants.OK_ID) {
					mounts.add(dlg.getMount());
					tableViewer.refresh();
				}
			}
		});

		var editButton = new Button(composite, SWT.PUSH);
		editButton.setText("Edit...");
		editButton.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).create());
		editButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				var sel = (IStructuredSelection) tableViewer.getSelection();
				if (sel.isEmpty()) return;
				var selected = (FATDiskImageMount) sel.getFirstElement();
				var dlg = new DiskImageDialog(getShell(), project, selected);
				if (dlg.open() == IDialogConstants.OK_ID) {
					var idx = mounts.indexOf(selected);
					mounts.set(idx, dlg.getMount());
					tableViewer.refresh();
				}
			}
		});

		var deleteButton = new Button(composite, SWT.PUSH);
		deleteButton.setText("Delete");
		deleteButton.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).create());
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				var sel = (IStructuredSelection) tableViewer.getSelection();
				if (sel.isEmpty()) return;
				var selected = (FATDiskImageMount) sel.getFirstElement();
				mounts.remove(selected);
				FATDiskImageManager.saveMounts(project, mounts);
				tableViewer.refresh();
				var job = Job.create("Unmounting disk image: " + selected.getName(), monitor -> {
					try {
						FATDiskImageManager.unmount(project, selected);
					} catch (CoreException ex) {
						return Status.error("Failed to unmount '" + selected.getName() + "'", ex);
					}
					return Status.OK_STATUS;
				});
				job.schedule();
			}
		});

		// Separator
		var sep = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).create());

		var mountButton = new Button(composite, SWT.PUSH);
		mountButton.setText("Mount");
		mountButton.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).create());
		mountButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				var sel = (IStructuredSelection) tableViewer.getSelection();
				if (sel.isEmpty()) return;
				var selected = (FATDiskImageMount) sel.getFirstElement();
				FATDiskImageManager.saveMounts(project, mounts);
				pendingMounts.add(selected.getName());
				tableViewer.refresh();
				var job = Job.create("Mounting disk image: " + selected.getName(), monitor -> {
					try {
						FATDiskImageManager.mount(project, selected);
					} catch (CoreException ex) {
						return Status.error("Failed to mount '" + selected.getName() + "'", ex);
					} finally {
						pendingMounts.remove(selected.getName());
					}
					if (tableViewer != null && !tableViewer.getControl().isDisposed()) {
						tableViewer.getControl().getDisplay().asyncExec(() -> {
							if (!tableViewer.getControl().isDisposed()) tableViewer.refresh();
						});
					}
					return Status.OK_STATUS;
				});
				job.setUser(true);
				job.schedule();
			}
		});

		var unmountButton = new Button(composite, SWT.PUSH);
		unmountButton.setText("Unmount");
		unmountButton.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).create());
		unmountButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				var sel = (IStructuredSelection) tableViewer.getSelection();
				if (sel.isEmpty()) return;
				var selected = (FATDiskImageMount) sel.getFirstElement();
				pendingMounts.add(selected.getName());
				tableViewer.refresh();
				var job = Job.create("Unmounting disk image: " + selected.getName(), monitor -> {
					try {
						FATDiskImageManager.unmount(project, selected);
					} catch (CoreException ex) {
						return Status.error("Failed to unmount '" + selected.getName() + "'", ex);
					} finally {
						pendingMounts.remove(selected.getName());
					}
					if (tableViewer != null && !tableViewer.getControl().isDisposed()) {
						tableViewer.getControl().getDisplay().asyncExec(() -> {
							if (!tableViewer.getControl().isDisposed()) tableViewer.refresh();
						});
					}
					return Status.OK_STATUS;
				});
				job.setUser(true);
				job.schedule();
			}
		});

		return composite;
	}

	@Override
	public boolean performOk() {
		if (project != null) {
			FATDiskImageManager.saveMounts(project, mounts);
			var mountsCopy = List.copyOf(mounts);
			var job = Job.create("Refreshing FAT disk image mounts", monitor -> {
				try {
					FATDiskImageManager.refreshLinkedFolders(project, mountsCopy);
					for (var m : mountsCopy) {
						if (m.isAutomount() && !FATDiskImageManager.isMounted(project, m)) {
							FATDiskImageManager.mount(project, m);
						}
					}
				} catch (CoreException e) {
					return Status.error("Failed to refresh FAT disk image linked folders", e);
				}
				return Status.OK_STATUS;
			});
			job.setUser(true);
			job.schedule();
		}
		return true;
	}

	@Override
	protected void performDefaults() {
		if (mounts != null) {
			mounts.clear();
			tableViewer.refresh();
		}
		super.performDefaults();
	}

	/**
	 * Dialog for adding/editing a FAT disk image mount.
	 */
	private static class DiskImageDialog extends TitleAreaDialog {

		private FATDiskImageMount mount;
		private final IProject project;
		private Text nameText;
		private Text pathText;
		private Button automountCheck;

		public DiskImageDialog(Shell parentShell, IProject project, FATDiskImageMount existing) {
			super(parentShell);
			this.project = project;
			this.mount = existing != null
					? new FATDiskImageMount(existing.getName(), existing.getImagePath(), existing.isAutomount())
					: new FATDiskImageMount();
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("FAT Disk Image");
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			setTitle("FAT Disk Image Configuration");
			setMessage("Enter a name and select the disk image file.\n"
					+ "Project-relative paths (e.g. Debug/test.img) or absolute paths are supported.");

			var area = (Composite) super.createDialogArea(parent);
			var container = new Composite(area, SWT.NONE);
			container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			container.setLayout(new GridLayout(4, false));

			new Label(container, SWT.NONE).setText("Name:");
			nameText = new Text(container, SWT.BORDER);
			nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			nameText.setText(mount.getName());

			new Label(container, SWT.NONE).setText("Image Path:");
			pathText = new Text(container, SWT.BORDER);
			pathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			pathText.setText(mount.getImagePath());

			var browseProjectButton = new Button(container, SWT.PUSH);
			browseProjectButton.setText("Project...");
			browseProjectButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					var dlg = new ElementTreeSelectionDialog(
							getShell(),
							new WorkbenchLabelProvider(),
							new WorkbenchContentProvider());
					dlg.setTitle("Select Disk Image");
					dlg.setMessage("Select a disk image file from the project.");
					dlg.setInput(project);
					dlg.setAllowMultiple(false);
					if (dlg.open() == Window.OK) {
						var result = dlg.getFirstResult();
						if (result instanceof IFile ifile) {
							var projRelPath = ifile.getProjectRelativePath().toPortableString();
							pathText.setText(projRelPath);
							autoFillName(projRelPath);
						} else if (result instanceof IResource res) {
							var projRelPath = res.getProjectRelativePath().toPortableString();
							pathText.setText(projRelPath);
							autoFillName(projRelPath);
						}
					}
				}
			});

			var browseExternalButton = new Button(container, SWT.PUSH);
			browseExternalButton.setText("External...");
			browseExternalButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					var dlg = new FileDialog(getShell(), SWT.OPEN);
					dlg.setFilterExtensions(new String[] { "*.img", "*.*" });
					dlg.setFilterNames(new String[] { "Disk Images (*.img)", "All Files (*.*)" });
					var result = dlg.open();
					if (result != null) {
						pathText.setText(result);
						autoFillName(result);
					}
				}
			});

			new Label(container, SWT.NONE); // spacer
			automountCheck = new Button(container, SWT.CHECK);
			automountCheck.setText("Automount when project opens");
			automountCheck.setSelection(mount.isAutomount());
			automountCheck.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

			return area;
		}

		private void autoFillName(String path) {
			if (nameText.getText().isBlank()) {
				var fname = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
				if (fname.contains("\\")) fname = fname.substring(fname.lastIndexOf('\\') + 1);
				var dot = fname.lastIndexOf('.');
				nameText.setText(dot > 0 ? fname.substring(0, dot) : fname);
			}
		}

		@Override
		protected void okPressed() {
			var name = nameText.getText().trim();
			var path = pathText.getText().trim();
			if (name.isEmpty() || path.isEmpty()) {
				setErrorMessage("Name and Image Path are required.");
				return;
			}

			mount.setName(name);
			mount.setImagePath(path);
			mount.setAutomount(automountCheck.getSelection());

			super.okPressed();
		}

		public FATDiskImageMount getMount() {
			return mount;
		}
	}
}
