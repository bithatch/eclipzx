package uk.co.bithatch.fatexplorer.views;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import de.waldheinz.fs.ReadOnlyException;
import uk.co.bithatch.fatexplorer.Activator;
import uk.co.bithatch.fatexplorer.preferences.DiskImageListEditor;
import uk.co.bithatch.fatexplorer.preferences.FATPreferencesAccess;
import uk.co.bithatch.fatexplorer.preferences.PreferenceConstants;
import uk.co.bithatch.fatexplorer.util.FileNames;
import uk.co.bithatch.fatexplorer.vfs.FATImageFileStore;
import uk.co.bithatch.fatexplorer.vfs.FileStoreCopyUtil;
import uk.co.bithatch.fatexplorer.vfs.FileStoreTransfer;
import uk.co.bithatch.fatexplorer.vfs.UIOverwritePolicyWithApplyToAll;
import uk.co.bithatch.zyxy.lib.MemoryUnit;

public class FATExplorerView extends ViewPart implements ISelectionChangedListener, IPreferenceChangeListener {

	private static class FatFileContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getChildren(Object parentElement) {
			try {
				if (parentElement instanceof URI uri) {
					IFileStore store = EFS.getStore(uri);
					/* TODO where to get a better progress monitor */
					return store.childStores(EFS.NONE, new NullProgressMonitor());
				} else if (parentElement instanceof IFileStore fs) {
					/* TODO where to get a better progress monitor */
					return fs.childStores(EFS.NONE, new NullProgressMonitor());
				} else {
					throw new UnsupportedOperationException();
				}
			} catch (RuntimeException re) {
				throw re;
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof List<?>) {
				return ((List<?>) inputElement).toArray();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof IFileStore) {
				return ((IFileStore) element).getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof IFileStore) {
				try {
					/* TODO where to get a better progress monitor */
					return ((IFileStore) element).childStores(EFS.NONE, new NullProgressMonitor()).length > 0;
				} catch (Exception e) {
					return false;
				}
			}
			return false;
		}
	}

	private static class FatFileLabelProvider extends LabelProvider implements IStyledLabelProvider {
		@Override
		public Image getImage(Object element) {
			if (element instanceof FATImageFileStore fs) {
				var info = fs.fetchInfo();
				if (fs.getParent() == null) {
					return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
				} else if (info.isDirectory())
					return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
				else
					return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
			} else {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_ERROR);
			}
		}

		@Override
		public StyledString getStyledText(Object element) {
			if (element instanceof FATImageFileStore str) {
				if(str.getParent() == null) {
			        var s = new StyledString(getFsText(str));
					s.setStyle(0, s.length(), StyledString.DECORATIONS_STYLER);
					return s;
				}
			}
		    return new StyledString(element.toString());
		}

		@Override
		public String getText(Object element) {
			if (element instanceof IFileStore) {
				if (element instanceof FATImageFileStore fs && fs.getParent() == null) {
					//return FileNames.getPathFileName(FATPreferencesAccess.getPathForUUID(fs.getUuid()));
					return getFsText(fs);
				} else {
					return ((IFileStore) element).getName();
				}
			}
			return super.getText(element);
		}

		protected String getFsText(FATImageFileStore str) {
			var fs = str.nativeFileSystem();
			return String.format("%s (%d of %d MiB)", 
					fs.getVolumeLabel() == null || fs.getVolumeLabel().equals("") 
						? FileNames.getPathFileName(FATPreferencesAccess.getPathForUUID(str.getUuid()))
						: fs.getVolumeLabel(), 
					MemoryUnit.MEBIBYTE.fromBytes(fs.getUsableSpace() - fs.getFreeSpace()),
					MemoryUnit.MEBIBYTE.fromBytes(fs.getUsableSpace()));
		}
	}

	public static final String ID = "uk.co.bithatch.fatexplorer.views.fatExplorerView";

	private Clipboard clipboard;
	private Action copyAction = makeCopyAction();
	private Action createFolderAction = makeCreateFolderAction();
	private Action cutAction = makeCutAction();
	private Action deleteAction = makeDeleteAction();
	private Display display;
	private boolean isCutOperation = false;
	private Action pasteAction = makePasteAction();
	private Action renameAction = makeRenameAction();
	private Action saveToComputerAction = makeSaveToComputerAction();
	private Action saveToWorkspaceAction = makeSaveToWorkspaceAction();
	private Action propertiesAction = makePropertiesAction();

	private TreeViewer viewer;

	@Override
	public void createPartControl(Composite parent) {
		display = parent.getDisplay();
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new FatFileContentProvider());
		viewer.setLabelProvider(new FatFileLabelProvider());

		resetInput();
		viewer.addSelectionChangedListener(this);

		clipboard = new Clipboard(parent.getDisplay());

		makeViewMenu();
		configureDragAndDrop();
		hookDoubleClickAction();
		hookContextMenu();
		
		FATPreferencesAccess.getPreferences().addPreferenceChangeListener(this);
	}

	protected void resetInput() {
		viewer.setInput(FATPreferencesAccess.getConfiguredImageURIs().stream().map(uri -> {
			try {
				return openUri(uri);
			}
			catch(Exception e) {
				e.printStackTrace();
				return null;
			}
		}).filter(e -> e != null).toList());
	}

	@Override
	public void dispose() {
		FATPreferencesAccess.getPreferences().removePreferenceChangeListener(this);
		clipboard.dispose();
		super.dispose();
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if(event.getKey().equals(PreferenceConstants.DISK_IMAGES)) {
			display.execute(() -> resetInput());
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		updateState();
	}

	@Override
	public void setFocus() {
		viewer.getTree().setFocus();
	}

	protected IFileStore openUri(URI uri) {
		try {
			return EFS.getStore(uri);
		} catch (CoreException e) {
			return null;
		}
	}
	
	private void updateState() {
		var sel = getSelectedFileStores();
		var hasSel = sel.size() > 0;
		var selIsRoot = sel.stream().filter(s -> s.getParent() == null).findFirst().isPresent();
		var singleSelection = sel.size() == 1;
		
		propertiesAction.setEnabled(singleSelection && !selIsRoot);
		saveToComputerAction.setEnabled(hasSel && !selIsRoot);
		saveToWorkspaceAction.setEnabled(hasSel && !selIsRoot);
		cutAction.setEnabled(hasSel && !selIsRoot);
		copyAction.setEnabled(hasSel && !selIsRoot);
		pasteAction.setEnabled(hasSel);
		deleteAction.setEnabled(hasSel && (!selIsRoot || (selIsRoot && singleSelection)));
	}

	private void configureDragAndDrop() {
		var fileTransfer = FileTransfer.getInstance();
		var fileStoreTransfer = FileStoreTransfer.getInstance();
		viewer.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT,
				new Transfer[] { fileTransfer, fileStoreTransfer }, new DropTargetListener() {

					@Override
					public void dragEnter(DropTargetEvent event) {
						if (event.detail == DND.DROP_DEFAULT) {
							event.detail = DND.DROP_COPY;
						}
					}

					@Override
					public void dragLeave(DropTargetEvent event) {
					}

					@Override
					public void dragOperationChanged(DropTargetEvent event) {
						if (event.detail == DND.DROP_DEFAULT) {
							if ((event.operations & DND.DROP_MOVE) != 0) {
								event.detail = DND.DROP_MOVE;
							} else if ((event.operations & DND.DROP_COPY) != 0) {
								event.detail = DND.DROP_COPY;
							} else {
								event.detail = DND.DROP_NONE;
							}
						}
					}

					@Override
					public void dragOver(DropTargetEvent event) {
						event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
					}

					@Override
					public void drop(DropTargetEvent event) {
						if (fileTransfer.isSupportedType(event.currentDataType)) {
							var dropItem = (TreeItem) event.item;
							var itemData = dropItem.getData();
							if (itemData instanceof FATImageFileStore file && event.data instanceof String[] files) {
								if (event.detail == DND.DROP_COPY)
									copyFilesToTarget(file, files);
								else if (event.detail == DND.DROP_MOVE)
									moveFilesToTarget(file, files);
								return;
							}
						} else if (fileStoreTransfer.isSupportedType(event.currentDataType)) {
							var dropItem = (TreeItem) event.item;
							var itemData = dropItem.getData();
							if (itemData instanceof FATImageFileStore file
									&& event.data instanceof IFileStore[] files) {
								if (event.detail == DND.DROP_COPY)
									copyFileStoresToTarget(file, files);
								else if (event.detail == DND.DROP_MOVE)
									moveFileStoresToTarget(file, files);
								return;
							}
						}
					}

					@Override
					public void dropAccept(DropTargetEvent event) {
					}

				});

		viewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY /* | DND.DROP_DEFAULT */,
				new Transfer[] { fileStoreTransfer }, new DragSourceListener() {

					private IFileStore draggingStore;

					@Override
					public void dragFinished(DragSourceEvent event) {
						draggingStore = null;
					}

					@Override
					public void dragSetData(DragSourceEvent event) {
						if (fileStoreTransfer.isSupportedType(event.dataType) && draggingStore != null) {
							event.data = new IFileStore[] { draggingStore };
						}
					}

					@Override
					public void dragStart(DragSourceEvent event) {
						draggingStore = (IFileStore) viewer.getStructuredSelection().getFirstElement();
					}
				});

	}

	private void copyFileStoresToTarget(FATImageFileStore targetDir, IFileStore[] fileList) {
		var policy = new UIOverwritePolicyWithApplyToAll();
		fileJob("Copy Files To FAT16/FAT32 Filesystem", monitor -> {
			monitor.beginTask("Copying files", fileList.length);
			try {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				
				for (var file : fileList) {
					FileStoreCopyUtil.copyStoreToStore(file, targetDir, true, policy, monitor);
					monitor.worked(1);
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
				}

				display.execute(() -> viewer.refresh(targetDir));
				return Status.OK_STATUS;

			} catch (Exception e) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to copy files.", e);
			} finally {
				monitor.done();
			}
		});
	}

	private void copyFileStoresToTarget(File target, IFileStore[] fileList, Runnable onDone) {
		var policy = new UIOverwritePolicyWithApplyToAll();
		fileJob("Copy Files To FAT16/FAT32 Filesystem", monitor -> {
			monitor.beginTask("Copying files", fileList.length);
			try {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				for (var file : fileList) {
					FileStoreCopyUtil.copyStoreToFile(file, target, true, policy, monitor);
					
					monitor.worked(1);
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
				}

				if (onDone != null) {
					display.execute(() -> {
						onDone.run();
					});
				}
				return Status.OK_STATUS;
			} catch (CoreException e) {
				return e.getStatus();
			} catch (Exception e) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to copy files.", e);
			} finally {
				monitor.done();
			}
		});
	}

	private void copyFilesToTarget(FATImageFileStore targetDir, String[] files) {
		var policy = new UIOverwritePolicyWithApplyToAll();
		var fileList = Arrays.asList(files).stream().map(File::new).toList();
		fileJob("Copy Files To FAT16/FAT32 Filesystem", monitor -> {
			monitor.beginTask("Copying files", fileList.size());
			try {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				for (var file : fileList) {
					FileStoreCopyUtil.copyFileToStore(file, targetDir, true, policy, monitor);
					monitor.worked(1);
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
				}

				display.execute(() -> viewer.refresh(targetDir));
				return Status.OK_STATUS;

			} catch (Exception e) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to copy files.", e);
			} finally {
				monitor.done();
			}
		});
	}

	private void copySelectionToClipboard() {
		var fileStoreTransfer = FileStoreTransfer.getInstance();
		clipboard.setContents(new Object[] { getSelectedFileStores().toArray(new IFileStore[0]) }, new Transfer[] { fileStoreTransfer });
	}

	private void fileJob(String jobName, Function<IProgressMonitor, IStatus> task) {
		/* TODO make sure only ever one of these jobs running per disk image */
		var job = new Job(jobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return task.apply(monitor);
			}
		};

		job.setUser(true);
		job.schedule();
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(saveToWorkspaceAction);
		manager.add(saveToComputerAction);
		manager.add(new Separator());
		manager.add(createFolderAction);
		manager.add(new Separator());
		manager.add(cutAction);
		manager.add(copyAction);
		manager.add(pasteAction);
		manager.add(new Separator());
		manager.add(renameAction);
		manager.add(deleteAction);
		manager.add(new Separator());
		manager.add(propertiesAction);
	}

	private List<FATImageFileStore> getSelectedFileStores() {
		return viewer.getStructuredSelection().stream().filter(obj -> obj instanceof FATImageFileStore)
				.map(obj -> ((FATImageFileStore) obj)).toList();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				var selection = viewer.getStructuredSelection();
				var element = selection.getFirstElement();
				if (element instanceof FATImageFileStore fs && !fs.fetchInfo().isDirectory()) {
					try {
						IDE.openEditorOnFileStore(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
								fs);
					} catch (PartInitException e) {
						throw new IllegalStateException(e);
					}
				} else if (viewer.getExpandedState(element)) {
					viewer.collapseToLevel(element, TreeViewer.ALL_LEVELS);
				} else {
					viewer.expandToLevel(element, 1);
				}
			}
		});
	}

	private Action makeCopyAction() {
		var action = new Action("Copy") {
			@Override
			public void run() {
				copySelectionToClipboard();
				isCutOperation = false;
			}
		};
		action.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		return action;
	}

	private Action makeCreateFolderAction() {
		var action = new Action("New Folder") {
			@Override
			public void run() {
				var obj = viewer.getStructuredSelection().getFirstElement();
				if (obj instanceof FATImageFileStore parent) {
					var dialog = new InputDialog(getSite().getShell(), "Create Folder", "Folder name:", "New Folder",
							null);
					if (dialog.open() == Window.OK) {
						var folderName = dialog.getValue();
						fileJob("Create Folder", monitor -> {
							try {
								monitor.beginTask("Creating Folder", 1);
								parent.getChild(folderName).mkdir(EFS.NONE, monitor);
								monitor.worked(1);
								return Status.OK_STATUS;
							} catch (CoreException e) {
								return e.getStatus();
							} finally {
								display.execute(() -> viewer.refresh(parent));
								monitor.done();
							}
						});

					}
				}
			}
		};
		action.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));
		return action;
	}

	private Action makeCutAction() {
		var action = new Action("Cut") {
			@Override
			public void run() {
				copySelectionToClipboard();
				isCutOperation = true;
			}
		};
		action.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		return action;
	}

	private Action makeDeleteAction() {
		var action = new Action("Delete") {
			@Override
			public void run() {
				var selection = viewer.getStructuredSelection();
				var selectedFiles = selection.stream().filter(s -> s instanceof FATImageFileStore)
						.map(s -> (FATImageFileStore) s).toList();
				
				var selIsRoot = selectedFiles.stream().filter(s -> s.getParent() == null).findFirst().isPresent();
				
				if(selIsRoot) {
					if (MessageDialog.openQuestion(
			                Display.getDefault().getActiveShell(),
			                "Delete Disk Image Reference",
			                "Are you sure you wish to delete this disk image reference? The actual image will not be removed."
			            )) {
						selectedFiles.forEach(s -> {
							try {
								var nfs = s.nativeFileSystem();
								nfs.flush();
								nfs.close();
							} catch (IOException e) {
							}
						});
						FATPreferencesAccess.removeImagePath(FATPreferencesAccess.getPathForURI(selectedFiles.get(0).toURI()));
		            }	
				}
				else {				
					if (MessageDialog.openQuestion(
			                Display.getDefault().getActiveShell(),
			                "Delete files",
			                "Are you sure you wish to delete these " + selectedFiles.size() + " files(s) and folders()?"
			            )) {
						fileJob("Delete files", monitor -> {
							try {
								monitor.beginTask("Deleting files", selectedFiles.size());
								for (var file : selectedFiles) {
									file.delete(EFS.NONE, monitor);
									monitor.worked(1);
								}
								return Status.OK_STATUS;
							} catch (CoreException e) {
								return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to delete files.", e);
							} finally {
								display.execute(() -> viewer.refresh());
								monitor.done();
							}
						});
		            }
				}

				
				
				
			}
		};
		action.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		return action;
	}

	private Action makePasteAction() {
		var action = new Action("Paste") {
			@Override
			public void run() {
				if (viewer.getStructuredSelection().getFirstElement() instanceof FATImageFileStore targetDir) {

					var fileTransfer = FileTransfer.getInstance();
					var fileContents = clipboard.getContents(fileTransfer);
					if (fileContents == null) {
						var fsTransfer = FileStoreTransfer.getInstance();
						var fsContents = clipboard.getContents(fsTransfer);
						if (fsContents instanceof IFileStore[] stores) {
							copyFileStoresToTarget(targetDir, stores);
						} else {
							throw new IllegalStateException("Unexpected URL contents");
						}
					} else {
						if (fileContents instanceof String[] files) {
							if(isCutOperation)
								moveFilesToTarget(targetDir, files);
							else
								copyFilesToTarget(targetDir, files);
						} else {
							throw new IllegalStateException("Unexpected file contents");
						}
					}

					viewer.refresh(targetDir);
				}
			}
		};
		action.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		return action;
	}

	private Action makeRenameAction() {
		return new Action("Rename") {
			@Override
			public void run() {
				var obj = viewer.getStructuredSelection().getFirstElement();
				if (obj instanceof FATImageFileStore file) {
					if(file.getParent() == null) {

						var lbl = file.nativeFileSystem().getVolumeLabel();
						if(lbl == null) 
							lbl = "";
						
						var dialog = new InputDialog(getSite().getShell(), "Rename Volume", "New name:", lbl, null);
						if (dialog.open() == Window.OK) {
							var newName = dialog.getValue();
							fileJob("Rename Volume", monitor -> {
								try {
									monitor.beginTask("Renaming volume", 1);
									file.nativeFileSystem().setVolumeLabel(newName); // You implement this
									monitor.worked(1);
									return Status.OK_STATUS;
								} catch (ReadOnlyException e) {
									return Status.error("Read only.", e);
								} catch (IOException e) {
									return Status.error("Failed to set volume label.", e);
								} finally {
									display.execute(() -> viewer.refresh());
									monitor.done();
								}
							});
						}
					}
					else {
						var dialog = new InputDialog(getSite().getShell(), "Rename File Or Folder", "New name:", file.getName(), null);
						if (dialog.open() == Window.OK) {
							var newName = dialog.getValue();
							fileJob("Rename File", monitor -> {
								try {
									monitor.beginTask("Renaming file", 1);
									file.move(file.getParent().getChild(newName), EFS.NONE, monitor); // You implement this
									monitor.worked(1);
									return Status.OK_STATUS;
								} catch (CoreException e) {
									return e.getStatus();
								} finally {
									display.execute(() -> viewer.refresh());
									monitor.done();
								}
							});
						}
					}
				}
			}
		};
	}

	private Action makeSaveToComputerAction() {
		var action = new Action("Save To Computer") {
			@Override
			public void run() {
				var selected = getSelectedFileStores();
				if (selected.size() > 1 || selected.get(0).fetchInfo().isDirectory()) {
					var fileDialog = new DirectoryDialog(getViewSite().getShell(), SWT.SAVE);
					fileDialog.setText("Select Directory");
					var path = fileDialog.open();
					if (path != null) {
						copyFileStoresToTarget(new File(path), selected.toArray(new IFileStore[0]), null);
					}
				} else {
					var fileDialog = new FileDialog(getViewSite().getShell(), SWT.SAVE);
					fileDialog.setOverwrite(true);
					fileDialog.setFileName(selected.get(0).getName());
					fileDialog.setText("Select File");
					var path = fileDialog.open();
					if (path != null) {
						copyFileStoresToTarget(new File(path), selected.toArray(new IFileStore[0]), null);
					}
				}
			}
		};
		return action;
	}

	private Action makePropertiesAction() {
		var action = new Action("Properties") {
			@Override
			public void run() {
				var dialog = new FATFilePropertiesDialog(getViewSite().getShell(), getSelectedFileStores().get(0));
				if(dialog.open() == SWT.OK) {
				}
			}
		};
		return action;
	}

	private Action makeSaveToWorkspaceAction() {
		var action = new Action("Save To Workspace") {
			@Override
			public void run() {
				var selected = getSelectedFileStores();
				var ws = PlatformUI.getWorkbench().getAdapter(IWorkspace.class);
				var rootDir = ws.getRoot();
				var projDialog = new ContainerSelectionDialog(getViewSite().getShell(), rootDir, true,
						"Select a folder from your workspace");
				if (projDialog.open() == Window.OK) {
					var selectedTarget = projDialog.getResult();
					if (selectedTarget.length > 0) {
						var path = (IPath) selectedTarget[0];
						var targetDir = rootDir.getRawLocation().append(path).toFile();
						copyFileStoresToTarget(targetDir,
								selected.toArray(new IFileStore[0]), () -> {
									try {
										var file = rootDir.getRawLocation().append(path.addTrailingSeparator());
										rootDir.getContainerForLocation(file).refreshLocal(IResource.DEPTH_INFINITE,
												new NullProgressMonitor());
									} catch (CoreException e) {
									}
								});
					}
				}
			}
		};
		return action;
	}

	private void makeViewMenu() {
		var contextMenu = new Menu(viewer.getTree());
		viewer.getTree().setMenu(contextMenu);

		var refreshAction = new Action("Refresh") {
			public void run() {
				resetInput();
			}
		};
		refreshAction.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
		getViewSite().getActionBars().getToolBarManager().add(refreshAction);

		var addImageAction = new Action("Add Image") {
			public void run() {
				Shell shell = getSite().getShell();
				String uriStr = DiskImageListEditor.newDiskImageDialog(shell, null);
				if (uriStr != null) {
					FATPreferencesAccess.addImagePath(uriStr);
					viewer.setInput(FATPreferencesAccess.getConfiguredImageURIs());
				}
			}
		};
		addImageAction.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ADD));
		getViewSite().getActionBars().getToolBarManager().add(addImageAction);
	}

	private void moveFileStoresToTarget(FATImageFileStore targetDir, IFileStore[] fileList) {
		var policy = new UIOverwritePolicyWithApplyToAll();
		fileJob("Move Files To FAT16/FAT32 Filesystem", monitor -> {
			monitor.beginTask("Moving files", fileList.length);
			try {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				for (var file : fileList) {
					FileStoreCopyUtil.moveStoreToStore(file, targetDir, true, policy, monitor);
					monitor.worked(1);
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
				}

				display.execute(() -> {
					viewer.refresh(targetDir);
					for (var f : fileList) {
						if (f.getParent() != null)
							viewer.refresh(f.getParent());
						else
							viewer.refresh(f);

					}
				});
				return Status.OK_STATUS;

			} catch (Exception e) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to move files.", e);
			} finally {
				monitor.done();
			}
		});
	}

	private void moveFilesToTarget(FATImageFileStore targetDir, String[] files) {
		var policy = new UIOverwritePolicyWithApplyToAll();
		var fileList = Arrays.asList(files).stream().map(File::new).toList();
		fileJob("Moving Files To FAT16/FAT32 Filesystem", monitor -> {
			monitor.beginTask("Moving files", fileList.size());
			try {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				for (var file : fileList) {
					FileStoreCopyUtil.moveFileToStore(file, targetDir, true, policy, monitor);
					monitor.worked(1);
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
				}

				display.execute(() -> viewer.refresh(targetDir));
				return Status.OK_STATUS;

			} catch (Exception e) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to move files.", e);
			} finally {
				monitor.done();
			}
		});
	}
}