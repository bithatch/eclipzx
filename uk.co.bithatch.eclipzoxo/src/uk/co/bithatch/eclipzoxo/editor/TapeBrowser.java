package uk.co.bithatch.eclipzoxo.editor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
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

import uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferencesAccess;
import uk.co.bithatch.eclipzoxo.views.EmulatorInstance;
import uk.co.bithatch.eclipzoxo.views.EmulatorView;
import uk.co.bithatch.zoxo.system.Tape;
import uk.co.bithatch.zoxo.system.TapeBase;
import uk.co.bithatch.zoxo.system.TapeBlockListener;
import uk.co.bithatch.zoxo.system.TapeSettings;
import uk.co.bithatch.zoxo.system.TapeState;
import uk.co.bithatch.zoxo.system.TapeStateListener;

public class TapeBrowser extends EditorPart implements TapeBlockListener, TapeStateListener {

	private record TapeBlock(int number, String type, String info) {
	}

	private final static ILog LOG = ILog.of(TapeBrowser.class);

	public static final String ID = "uk.co.bithatch.eclipzoxo.editor.TapeBrowser";

	protected Composite container;
	private Optional<EmulatorInstance> emulator = Optional.empty();
	private Path nativeFile;
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
				LOG.error("Failed to open tape.", e1);
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
					for (var emulator : eview.getEmulators()) {
						if (nativeFile.equals(emulator.tape().map(Tape::getTapeFilename).orElse(null))) {
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
		setEmulator(null);
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
		// Optional
	}

	public Path getNativeFile() {
		return nativeFile;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {

		setSite(site);
		setInput(input);
		setPartName(input.getName());

		var file = getFile();
		var loc = file.getLocation();

		this.nativeFile = loc.toPath();

		/*
		 * This is not the same tape object as in the emulator, it is just used for
		 * display. We send events to and listen for events from the tape in the
		 * emulator, not this one.
		 */
		tape = new TapeBase(ZoxoPreferencesAccess.get().settings(TapeSettings.class));
		try {
			tape.insert(this.nativeFile);
		} catch (IOException e) {
			throw new PartInitException(Status.error("Failed to insert tape.", e));
		}

		onInit();

	}

	@Override
	public boolean isDirty() {
		return false;
	}

	public boolean isInserted() {
		return emulator.flatMap(EmulatorInstance::tape).map(Tape::isTapeInserted).orElse(false);
	}

	public boolean isPlaying() {
		return emulator.flatMap(EmulatorInstance::tape).map(Tape::isTapePlaying).orElse(false);
	}

	public boolean isReady() {
		return emulator.flatMap(EmulatorInstance::tape).map(Tape::isTapeReady).orElse(false);
	}

	public boolean isRecording() {
		return emulator.flatMap(EmulatorInstance::tape).map(Tape::isTapeRecording).orElse(false);
	}

	public boolean isRunning() {
		return emulator.flatMap(EmulatorInstance::tape).map(Tape::isTapeRunning).orElse(false);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void play() {
		emulator.flatMap(EmulatorInstance::tape).ifPresent(t -> {
			try {
				t.play(true);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});

	}

	public void rewind() {
		emulator.flatMap(EmulatorInstance::tape).ifPresent(t -> {
			try {
				t.rewind();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});

	}

	public void setEmulator(EmulatorInstance emulator) {
		if (!Objects.equals(emulator, this.emulator.orElse(null))) {
			this.emulator.flatMap(EmulatorInstance::tape).ifPresent(tp -> {
				tp.removeTapeBlockListener(this);
				tp.removeTapeChangedListener(this);
			});
			this.emulator = Optional.ofNullable(emulator);
			this.emulator.flatMap(EmulatorInstance::tape).ifPresent(tp -> {
				tp.addTapeBlockListener(this);
				tp.addTapeChangedListener(this);
			});
			updateState();
		}
	}

	@Override
	public void setFocus() {
		container.setFocus();
	}

	public void startRecord() {
		emulator.flatMap(EmulatorInstance::tape).ifPresent(t -> {
			try {
				t.startRecording();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});

	}

	@Override
	public void stateChanged(TapeState arg0) {
		System.out.println("TapeState: " + arg0);
		updateState();

	}

	public void stop() {
		emulator.flatMap(EmulatorInstance::tape).ifPresent(tape -> {
			try {
				if (tape.isTapeRecording()) {
					tape.stopRecording();
				} else {
					tape.stop();
				}
			} catch (IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		});
	}

	protected void createAccessory(Composite root) {
	}

	protected void doSave(IFile file) throws IOException {
	}

	protected IFile getFile() {
		return getEditorInput().getAdapter(IFile.class);
	}

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
			emulator.flatMap(EmulatorInstance::tape).ifPresent(tp -> {
				var idx = viewer.getTable().getSelectionIndex();
				if (idx > -1)
					tp.setSelectedBlock(idx);
			});
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
		if(status.isDisposed())
			return;
		statusLayoutData.exclude = emulator.isPresent();
		status.setVisible(emulator.isEmpty());
		container.layout();
		progress.setMaximum(emulator.flatMap(EmulatorInstance::tape).map(Tape::getNumBlocks).orElse(100));
		progress.setSelection(emulator.flatMap(EmulatorInstance::tape).map(Tape::getSelectedBlock).orElse(100));
		emulator.flatMap(EmulatorInstance::tape).ifPresentOrElse(
				tape -> viewer.getTable().setSelection(tape.getSelectedBlock()), () -> viewer.getTable().deselectAll());
	}

	public EmulatorInstance getEmulator() {
		return emulator.orElse(null);
	}

}
