package uk.co.bithatch.tnfs.eclipse;

import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Property page for managing TNFS client mounts on a project.
 * Shows only for IProject resources.
 */
public class TNFSMountPropertiesPage extends PropertyPage {

	private TableViewer tableViewer;
	private List<TNFSClientMount> mounts;
	private IProject project;
	private final Set<String> pendingMounts = new HashSet<>();

	@Override
	protected Control createContents(Composite parent) {
		var resource = (IResource) getElement().getAdapter(IResource.class);
		if (!(resource instanceof IProject)) {
			var label = new Label(parent, SWT.WRAP);
			label.setText("TNFS client mounts are only available for projects.");
			return label;
		}

		project = (IProject) resource;
		mounts = new ArrayList<>(TNFSMountManager.getMounts(project));

		var composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		var infoLabel = new Label(composite, SWT.WRAP);
		infoLabel.setText("Mount remote TNFS servers as folders in this project. "
				+ "Mounts appear under \"TNFS Mounts\" in the project. "
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
				var name = ((TNFSClientMount) element).getName();
				if (pendingMounts.contains(name)) return "\u23F3";
				return TNFSMountManager.isMounted(project, (TNFSClientMount) element) ? "\u2713" : "";
			}
		});

		var nameCol = new TableViewerColumn(tableViewer, SWT.NONE);
		nameCol.getColumn().setText("Name");
		nameCol.getColumn().setWidth(100);
		nameCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((TNFSClientMount) element).getName();
			}
		});

		var hostCol = new TableViewerColumn(tableViewer, SWT.NONE);
		hostCol.getColumn().setText("Host");
		hostCol.getColumn().setWidth(120);
		hostCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				var m = (TNFSClientMount) element;
				return m.getHost() + ":" + m.getPort();
			}
		});

		var pathCol = new TableViewerColumn(tableViewer, SWT.NONE);
		pathCol.getColumn().setText("Remote Path");
		pathCol.getColumn().setWidth(100);
		pathCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((TNFSClientMount) element).getRemotePath();
			}
		});

		var autoCol = new TableViewerColumn(tableViewer, SWT.CENTER);
		autoCol.getColumn().setText("Auto");
		autoCol.getColumn().setWidth(50);
		autoCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((TNFSClientMount) element).isAutomount() ? "\u2713" : "";
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
				var dlg = new MountDialog(getShell(), null);
				if (dlg.open() == IDialogConstants.OK_ID) {
					var m = dlg.getMount();
					mounts.add(m);
					if (dlg.getPassword() != null && !dlg.getPassword().isEmpty()) {
						TNFSMountManager.setPassword(m, dlg.getPassword());
					}
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
				var selected = (TNFSClientMount) sel.getFirstElement();
				var dlg = new MountDialog(getShell(), selected);
				if (dlg.open() == IDialogConstants.OK_ID) {
					var idx = mounts.indexOf(selected);
					mounts.set(idx, dlg.getMount());
					if (dlg.getPassword() != null) {
						TNFSMountManager.setPassword(dlg.getMount(), dlg.getPassword());
					}
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
				var selected = (TNFSClientMount) sel.getFirstElement();
				TNFSMountManager.setPassword(selected, null);
				mounts.remove(selected);
				TNFSMountManager.saveMounts(project, mounts);
				tableViewer.refresh();
				// Unmount in background
				var job = Job.create("Unmounting TNFS: " + selected.getName(), monitor -> {
					try {
						TNFSMountManager.unmount(project, selected);
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
				var selected = (TNFSClientMount) sel.getFirstElement();
				TNFSMountManager.saveMounts(project, mounts);
				pendingMounts.add(selected.getName());
				tableViewer.refresh();
				var job = Job.create("Mounting TNFS: " + selected.getName(), monitor -> {
					try {
						TNFSMountManager.mount(project, selected);
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
				var selected = (TNFSClientMount) sel.getFirstElement();
				pendingMounts.add(selected.getName());
				tableViewer.refresh();
				var job = Job.create("Unmounting TNFS: " + selected.getName(), monitor -> {
					try {
						TNFSMountManager.unmount(project, selected);
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
			// Save config synchronously (fast), but refresh linked folders in background
			TNFSMountManager.saveMounts(project, mounts);
			var mountsCopy = List.copyOf(mounts);
			var job = Job.create("Refreshing TNFS mounts", monitor -> {
				try {
					TNFSMountManager.refreshLinkedFolders(project, mountsCopy);
				} catch (CoreException e) {
					return Status.error("Failed to refresh TNFS linked folders", e);
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
	 * Dialog for adding/editing a TNFS mount.
	 */
	private static class MountDialog extends TitleAreaDialog {

		private TNFSClientMount mount;
		private String password;

		private Text nameText;
		private Text hostText;
		private Text portText;
		private Text pathText;
		private Text usernameText;
		private Text passwordText;
		private Button automountCheck;

		public MountDialog(Shell parentShell, TNFSClientMount existing) {
			super(parentShell);
			this.mount = existing != null
					? new TNFSClientMount(existing.getName(), existing.getHost(), existing.getPort(),
							existing.getRemotePath(), existing.getUsername(), existing.isAutomount())
					: new TNFSClientMount();
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("TNFS Mount");
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			setTitle("TNFS Mount Configuration");
			setMessage("Enter the TNFS server details for this mount.");

			var area = (Composite) super.createDialogArea(parent);
			var container = new Composite(area, SWT.NONE);
			container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			container.setLayout(new GridLayout(2, false));

			new Label(container, SWT.NONE).setText("Name:");
			nameText = new Text(container, SWT.BORDER);
			nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			nameText.setText(mount.getName());

			new Label(container, SWT.NONE).setText("Host:");
			hostText = new Text(container, SWT.BORDER);
			hostText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			hostText.setText(mount.getHost());

			new Label(container, SWT.NONE).setText("Port:");
			portText = new Text(container, SWT.BORDER);
			portText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			portText.setText(String.valueOf(mount.getPort()));

			new Label(container, SWT.NONE).setText("Remote Path:");
			pathText = new Text(container, SWT.BORDER);
			pathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			pathText.setText(mount.getRemotePath());

			new Label(container, SWT.NONE).setText("Username:");
			usernameText = new Text(container, SWT.BORDER);
			usernameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			usernameText.setText(mount.getUsername());

			new Label(container, SWT.NONE).setText("Password:");
			passwordText = new Text(container, SWT.BORDER | SWT.PASSWORD);
			passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			// Don't pre-fill password for security

			new Label(container, SWT.NONE); // spacer
			automountCheck = new Button(container, SWT.CHECK);
			automountCheck.setText("Automount when project opens");
			automountCheck.setSelection(mount.isAutomount());
			automountCheck.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			return area;
		}

		@Override
		protected void okPressed() {
			var name = nameText.getText().trim();
			var host = hostText.getText().trim();
			if (name.isEmpty() || host.isEmpty()) {
				setErrorMessage("Name and Host are required.");
				return;
			}
			int port;
			try {
				port = Integer.parseInt(portText.getText().trim());
			} catch (NumberFormatException e) {
				setErrorMessage("Port must be a valid number.");
				return;
			}

			mount.setName(name);
			mount.setHost(host);
			mount.setPort(port);
			mount.setRemotePath(pathText.getText().trim());
			mount.setUsername(usernameText.getText().trim());
			mount.setAutomount(automountCheck.getSelection());
			password = passwordText.getText();

			super.okPressed();
		}

		public TNFSClientMount getMount() {
			return mount;
		}

		public String getPassword() {
			return password;
		}
	}
}
