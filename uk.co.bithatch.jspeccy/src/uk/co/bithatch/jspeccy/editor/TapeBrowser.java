package uk.co.bithatch.jspeccy.editor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import uk.co.bithatch.jspeccy.Activator;
import uk.co.bithatch.jspeccy.views.EmulatorInstance;
import uk.co.bithatch.jspeccy.views.EmulatorView;
import utilities.Tape;
import utilities.TapeBlockListener;

public class TapeBrowser extends EditorPart /* implements SelectionListener */ implements TapeBlockListener {

	private record TapeBlock(int number, String type, String info) {
	}
	
	public static final String ID = "uk.co.bithatch.jspeccy.editor.TapeBrowser";

	protected Composite container;
	private EmulatorInstance emulator;
	private File nativeFile;
	private ProgressBar progress;
	private Composite status;
	private GridData statusLayoutData;
	private Tape tape;

	private TableViewer viewer;

	@Override
	public void blockChanged(int block) {
		container.getDisplay().asyncExec(() -> {
			viewer.getTable().setSelection(block);
			progress.setSelection(block);
		});
	}

	@Override
	public void createPartControl(Composite parent) {

		container = new Composite(parent, SWT.NONE);
		var containerLayout = new GridLayout(1, true);
		containerLayout.verticalSpacing = 8;
		container.setLayout(containerLayout);

		status = new Composite(container, SWT.NONE);
		var statusLayout = new GridLayout(2, false);
		statusLayout.horizontalSpacing = 8;
		status.setLayout(statusLayout);
		statusLayoutData = GridDataFactory.fillDefaults().grab(true, false).create();
		status.setLayoutData(statusLayoutData);

		var info = new Label(status, SWT.WRAP);
		info.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK));
		info.setText("Tape is not attached to emulator.");

		var link = new Link(status, SWT.NONE);
		link.setText("<a>Open In Emulator</a>");
		link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			try {
				EmulatorView.open(nativeFile);
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
		}));
		createAccessory(container);

		createTable();

		progress = new ProgressBar(container, SWT.NONE);
		progress.setLayoutData(statusLayoutData);
		progress.setMaximum(100);
		progress.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 16).create());

		onPartControl();

		/*
		 * Find any emulators that have this tape. Subsequent changes to this are
		 * delivered to this editor via setEmulator()
		 */
		for (var viewRef : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences()) {
			if (viewRef.getId().equals(EmulatorView.ID)) {
				var em = viewRef.getPart(false);
				if (em instanceof EmulatorView eview) {
					for(var emulator : eview.getEmulators()) {
						if(nativeFile.equals(emulator.getTapeFile())) {
							setEmulator(emulator);
						}
					}
					break;
				}
			}
		}

		updateState();
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
		// Optional
	}

	public EmulatorInstance getEmulator() {
		return emulator;
	}

	public File getNativeFile() {
		return nativeFile;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {

		setSite(site);
		setInput(input);
		setPartName(input.getName());

		var file = getFile();
		var loc = file.getLocation();

		this.nativeFile = loc.toFile();

		/*
		 * This is not the same tape object as in the emulator, it is just used for
		 * display. We send events to and listen for events from the tape in the
		 * emulator, not this one.
		 */
		tape = new Tape(Activator.getDefault().settings().jspeccy().getTapeSettings());
		tape.insert(this.nativeFile);

		onInit();

	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void setEmulator(EmulatorInstance emulator) {
		if (!Objects.equals(emulator, this.emulator)) {
			if (this.emulator != null) {
				this.emulator.getTape().removeTapeBlockListener(this);
			}
			this.emulator = emulator;
			if(emulator != null) {
				emulator.getTape().addTapeBlockListener(this);
			}
			updateState();
		}
	}

	@Override
	public void setFocus() {
		container.setFocus();
	}
	
	public boolean isInserted() {
		return emulator != null && emulator.isTapeInserted();
	}
	
	public boolean isPlaying() {
		return emulator != null && emulator.isTapePlaying();
	}
	
	public boolean isRecording() {
		return emulator != null && emulator.isTapeRecording();
	}
	
	public boolean isReady() {
		return emulator != null && emulator.isTapeReady();
	}
	
	public boolean isRunning() {
		return emulator != null && emulator.isTapeRunning();
	}

	protected void createAccessory(Composite root) {
	}

	protected void doSave(IFile file) throws IOException {
	}

	protected IFile getFile() {
		return getEditorInput().getAdapter(IFile.class);
	}

//	protected AFX load(IPath loc) throws PartInitException {
//		try (var in = Files.newByteChannel(loc.toPath())) {
//			return AFX.load(in);
//		} catch (IOException e) {
//			throw new PartInitException(Status.error("Failed to load AFB nativeFile.", e));
//		}
//	}

	protected void onInit() {
	}

	protected void onPartControl() {
	}

	private void createColumns(TableViewer viewer2) {
		var blockNoCol = new TableViewerColumn(viewer, SWT.NONE);
		blockNoCol.getColumn().setWidth(64);
		blockNoCol.getColumn().setText("Block#");
		blockNoCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				var blk = (TapeBlock) element;
				return String.valueOf(blk.number);
			}
		});

		var blockTypeCol = new TableViewerColumn(viewer, SWT.NONE);
		blockTypeCol.getColumn().setWidth(192);
		blockTypeCol.getColumn().setText("Type");
		blockTypeCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				var blk = (TapeBlock) element;
				return blk.type;
			}
		});

		var blockinfoCol = new TableViewerColumn(viewer, SWT.NONE);
		blockinfoCol.getColumn().setWidth(256);
		blockinfoCol.getColumn().setText("Type");
		blockinfoCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				var blk = (TapeBlock) element;
				return blk.info;
			}
		});

	}

	private void createTable() {
		viewer = new TableViewer(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		viewer.addSelectionChangedListener(evt -> {
			if(emulator != null) {
				var idx = viewer.getTable().getSelectionIndex();
				if(idx > -1)
					emulator.getTape().setSelectedBlock(idx);
			}
		});

		createColumns(viewer);

		var table = viewer.getTable();
		table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		viewer.setContentProvider(ArrayContentProvider.getInstance());
		rebuildTableItems();
	}

	private void rebuildTableItems() {
		var blocks = new ArrayList<TapeBlock>();
		var num = tape.getNumBlocks();
		for (var i = 0; i < num; i++) {
			blocks.add(new TapeBlock(i, tape.getBlockType(i), tape.getBlockInfo(i)));
		}
		viewer.setInput(blocks);
	}

	private void updateState() {
		statusLayoutData.exclude = emulator != null;
		status.setVisible(emulator == null);
		container.layout();
		progress.setMaximum(emulator == null  ? 100 : emulator.getTape().getNumBlocks());
		progress.setSelection(emulator == null  ? 100 : emulator.getTape().getSelectedBlock());
		if(emulator == null)
			viewer.getTable().deselectAll();
		else
			viewer.getTable().setSelection(emulator.getTape().getSelectedBlock());
	}

	public void startRecord() {
		if(emulator != null) {
			emulator.startRecord();
		}
		
	}

	public void rewind() {
		if(emulator != null) {
			emulator.rewindTape();
		}
		
	}

	public void stop() {
		if(emulator != null) {
			if(emulator.isTapeRecording())
				emulator.stopRecord();
			else
				emulator.stopTape();
		}
	}

	public void play() {

		if(emulator != null) {
			emulator.playTape();
		}
		
	}


}
