package uk.co.bithatch.jspeccy.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import configuration.JSpeccySettings;
import machine.Interface1;
import machine.Interface1DriveListener;

public class MicrodriveManager extends Composite implements Interface1DriveListener {
	private final TableViewer viewer;
	private final Button btnOpenFileSystem, btnOpenWorkspace, btnInsertNew, btnEject, btnSave, btnSaveAs;
	private final Interface1 iface1;
	private static String lastPath = System.getProperty("user.dir");

	private MicrodriveManager(Composite parent, int style, Interface1 iface1, JSpeccySettings settings, File fileToMount) {
		super(parent, style);

		this.iface1 = iface1;

		setLayout(new GridLayout(2, false));

		var highlightFont = JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
		var boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);

		// ----- Table -----
		viewer = new TableViewer(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		var table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Content
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		// Drive column
		var colDrive = new TableViewerColumn(viewer, SWT.LEFT);
		colDrive.getColumn().setText("Drive");
		colDrive.getColumn().setWidth(70);
		colDrive.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return Integer.toString(((Cartridge) element).getDrive());
			}

			@Override
			public void update(ViewerCell cell) {
				super.update(cell);
				;
				cell.setFont(boldFont);
			}
		});

		// Filename column
		var colFile = new TableViewerColumn(viewer, SWT.LEFT);
		colFile.getColumn().setText("Filename");
		colFile.getColumn().setWidth(220);
		colFile.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				var cart = (Cartridge) element;
				if (cart.isInserted()) {
					return cart.getFile() == null ? "Unformatted Cartridge" : cart.getFile().getName();
				} else {
					return "Empty Drive";
				}
			}

			@Override
			public void update(ViewerCell cell) {
				super.update(cell);
				Object element = cell.getElement();
				if (!((Cartridge) element).isInserted()) {
					cell.setFont(highlightFont);
				}
			}
		});

		var colRO = new TableViewerColumn(viewer, SWT.CENTER);
		colRO.getColumn().setText("Read-Only");
		colRO.getColumn().setWidth(100);
		colRO.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Cartridge) element).isReadOnly() ? "☑" : "☐";
			}
		});

		var colMod = new TableViewerColumn(viewer, SWT.CENTER);
		colMod.getColumn().setText("Modified");
		colMod.getColumn().setWidth(90);
		colMod.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Cartridge) element).isModified() ? "☑" : "☐";
			}
		});

		// Seed rows
		viewer.setInput(seed(Byte.toUnsignedInt(settings.getInterface1Settings().getMicrodriveUnits())));

		// Selection listener to manage button enablement
		viewer.addSelectionChangedListener(e -> updateButtons());

		// ----- Right button bar -----
		Composite buttons = new Composite(this, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		buttons.setLayout(new GridLayout(1, true));

		btnInsertNew = new Button(buttons, SWT.PUSH);
		btnInsertNew.setText("Insert New");
		btnInsertNew.setLayoutData(buttonData());
		btnInsertNew.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> onInsertNew()));

		btnOpenWorkspace = new Button(buttons, SWT.PUSH);
		btnOpenWorkspace.setText("Workspace");
		btnOpenWorkspace.setLayoutData(buttonData());
		btnOpenWorkspace.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> onOpenWorkspace()));

		btnOpenFileSystem = new Button(buttons, SWT.PUSH);
		btnOpenFileSystem.setText("File System");
		btnOpenFileSystem.setLayoutData(buttonData());
		btnOpenFileSystem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> onOpenFileSystem()));

		btnEject = new Button(buttons, SWT.PUSH);
		btnEject.setText("Eject");
		btnEject.setLayoutData(buttonData());
		btnEject.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> onEject(getSelected())));

		btnSave = new Button(buttons, SWT.PUSH);
		btnSave.setText("Save");
		btnSave.setLayoutData(buttonData());
		btnSave.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> onSave(getSelected())));

		btnSaveAs = new Button(buttons, SWT.PUSH);
		btnSaveAs.setText("Save As");
		btnSaveAs.setLayoutData(buttonData());
		btnSaveAs.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> onSaveAs(getSelected())));

		updateButtons();

		iface1.addInterface1DriveListener(this);
		
		if(fileToMount != null) {
			parent.getDisplay().asyncExec(() -> {

				var nextFree = -1;
				var carts = getCartridges();
				for(int i = 0 ; i < carts.size(); i++) {
					var cart = carts.get(i);
					if(cart.isInserted()) {
						if(fileToMount.getAbsolutePath().equals(cart.getFile().getAbsolutePath())) {
							return;
						}
					}
					else  {
						nextFree = i;
						break;
					}
				}
				
				if(nextFree == -1) {
					MessageDialog.openError(getShell(), "Cartridge Error", "No free cartridge slots.");	
				}
				else {
					mountFileToCartridge(carts.get(nextFree), fileToMount);
				}
				
			});
			
		}
	}

	@Override
	public void dispose() {
		iface1.removeInterface1DriveListener(this);
		super.dispose();
	}

	private static GridData buttonData() {
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 110;
		return gd;
	}

	private List<Cartridge> seed(int n) {
		List<Cartridge> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			var entry = new Cartridge(i + 1);
			updateEntry(entry);
			list.add(entry);
		}
		return list;
	}

	private void updateEntry(Cartridge entry) {
		var drv = entry.getDrive() - 1;
		var drvPath = iface1.getAbsolutePath(drv);
		entry.setInserted(iface1.isCartridge(drv));
		entry.setFile(drvPath == null || drvPath.equals("") ? null : new File(drvPath));
		entry.setModified(iface1.isModified(drv));
		entry.setReadOnly(iface1.isWriteProtected(drv));
	}

	private Cartridge getSelected() {
		ISelection sel = viewer.getSelection();
		if (sel instanceof IStructuredSelection s && !s.isEmpty()) {
			Object o = s.getFirstElement();
			if (o instanceof Cartridge de)
				return de;
		}
		return null;
	}

	private void updateButtons() {
		var sel = getSelected();
		var hasSel = sel != null;
		var mounted = isInserted(sel);
		btnInsertNew.setEnabled(hasSel && !mounted);
		btnEject.setEnabled(hasSel && mounted);
		btnSave.setEnabled(hasSel && mounted && sel.isModified());
		btnSaveAs.setEnabled(hasSel && mounted);
		btnOpenFileSystem.setEnabled(hasSel && !mounted);
		btnOpenWorkspace.setEnabled(hasSel && !mounted);
	}

	private boolean isInserted(Cartridge sel) {
		return sel != null && sel.isInserted();
	}

	// ----- Action stubs -----
	private void onInsertNew() {
		Cartridge drv = getSelected();
		if (iface1.insertNew(drv.getDrive() - 1)) {
			driveChanged(drv);
		} else {
			MessageDialog.openError(getShell(), "Cartridge Error", "Failed to insert tape cartridge.");
		}
	}

	private void onOpenWorkspace() {
		var sel = getSelected();
		var row = sel.getDrive() - 1;

		var dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());

		dialog.setTitle("Select Cartridge File");
		dialog.setMessage("Select a cartridge file (*.mdv, *.mdr) from your workspace");
		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
		dialog.setAllowMultiple(false);

		dialog.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IFile file) {
					String ext = file.getFileExtension();
					return ext == null || ext.equalsIgnoreCase("mdv") || ext.equalsIgnoreCase("mdr");
				} else if (element instanceof IContainer) {
					return true; // allow folders
				}
				return false;
			}
		});

		if (dialog.open() == Window.OK) {
			if (iface1.insertFile(row, ((IFile) dialog.getFirstResult()).getLocation().toFile())) {
				driveChanged(sel);
			} else {
				MessageDialog.openError(getShell(), "Cartridge Error", "Failed to open tape cartridge.");
			}
		}

	}

	private void onOpenFileSystem() {
		var sel = getSelected();
		var row = sel.getDrive() - 1;
		var fileDialog = new FileDialog(getShell());
		fileDialog.setFilterPath(lastPath);
		fileDialog.setText("Select File");
		fileDialog.setFilterExtensions(new String[] { "*.mdr;*.mdv", "*.*" });
		fileDialog.setFilterNames(new String[] { "Microdrive Cartridge File (*.mdr,*.mdv)", "All Files (*.*)" });
		var res = fileDialog.open();
		if (res != null) {
			var resFile = new File(res);
			mountFileToCartridge(sel, resFile);
		}
	}

	private void mountFileToCartridge(Cartridge sel, File resFile) {
		lastPath = resFile.getParentFile().getAbsolutePath();
		if (iface1.insertFile(sel.getDrive() - 1, resFile)) {
			driveChanged(sel);
		} else {
			MessageDialog.openError(getShell(), "Cartridge Error", "Failed to open tape cartridge.");
		}
	}

	private void onEject(Cartridge row) {
		var sel = getSelected();
		if (iface1.isModified(sel.getDrive() - 1)) {
			if (!MessageDialog.openConfirm(getShell(), "Eject Cartridge",
					"The cartridge was modified. Are you sure?")) {
				return;
			}
		}
		iface1.eject(sel.getDrive() - 1);
		driveChanged(sel);
	}

	private void onSaveAs(Cartridge row) {
		var fileDialog = new FileDialog(getShell(), SWT.SAVE);
		fileDialog.setFilterPath(row.getFile() == null ? lastPath : row.getFile().getParentFile().getAbsolutePath());
		fileDialog.setText("Select File");
		fileDialog.setFilterExtensions(new String[] { "*.mdr;*.mdv", "*.*" });
		fileDialog.setFilterNames(new String[] { "Microdrive Cartridge File (*.mdr,*.mdv)", "All Files (*.*)" });
		fileDialog.setFileName(row.getFile() == null ? "Untitled.mdr" : row.getFile().getName());
		var res = fileDialog.open();
		if (res != null) {
			var resFile = new File(res);
			lastPath = resFile.getAbsolutePath();
			if (!iface1.save(row.getDrive() - 1, resFile)) {
				MessageDialog.openError(getShell(), "Cartridge Error", "Failed to save tape cartridge.");
			}
		}
	}

	private void onSave(Cartridge row) {
		if (row.getFile() == null)
			onSaveAs(row);
		else {
			if (iface1.save(row.getDrive() - 1)) {
				driveChanged(row);
			} else {
				MessageDialog.openError(getShell(), "Cartridge Error", "Failed to save tape cartridge.");
			}
		}
	}

	public TableViewer getViewer() {
		return viewer;
	}

	// ---------- Utility dialog launcher ----------
	public static void openDialog(Shell parent, Interface1 iface1, JSpeccySettings settings) {
		openDialog(parent, iface1, settings, null);
	}
	
	public static void openDialog(Shell parent, Interface1 iface1, JSpeccySettings settings, File fileToMount) {
		class DiskDialog extends Dialog {
			private MicrodriveManager comp;

			DiskDialog(Shell shell) {
				super(shell);
			}

			@Override
			public boolean close() {
				if (comp != null)
					comp.dispose();
				return super.close();
			}

			@Override
			protected Control createDialogArea(Composite parent) {
				Composite area = (Composite) super.createDialogArea(parent);
				area.setLayout(new FillLayout());
				comp = new MicrodriveManager(area, SWT.NONE, iface1, settings, fileToMount);
				return area;
			}

			@Override
			protected void createButtonsForButtonBar(Composite parent) {
				createButton(parent, IDialogConstants.OK_ID, "Close", true);
			}

			@Override
			protected Point getInitialSize() {
				return new Point(640, 420);
			}

			@Override
			protected void configureShell(Shell shell) {
				super.configureShell(shell);
				shell.setText("Microdrives");
			}
		}
		new DiskDialog(parent).open();
	}

	@Override
	public void driveModified(int drive) {
		getDisplay().asyncExec(() -> {
			driveChanged(getCartridges().get(drive));
		});
	}

	protected void driveChanged(Cartridge drv) {
		updateEntry(drv);
		viewer.refresh(drv);
		updateButtons();
	}

	@Override
	public void driveSelected(int drive) {
		getDisplay().asyncExec(() -> {
			var drv = getCartridges().get(drive);
			driveChanged(drv);
		});

	}

	@SuppressWarnings("unchecked")
	protected List<Cartridge> getCartridges() {
		return (List<Cartridge>) viewer.getInput();
	}
}
