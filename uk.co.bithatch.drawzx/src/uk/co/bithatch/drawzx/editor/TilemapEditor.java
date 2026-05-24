package uk.co.bithatch.drawzx.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.EditorPart;

import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.drawzx.tilemaps.Tilemap;
import uk.co.bithatch.drawzx.tilemaps.Tilemap.TilemapMode;
import uk.co.bithatch.drawzx.tilemaps.TilemapEntry;
import uk.co.bithatch.drawzx.widgets.DrawListener;
import uk.co.bithatch.drawzx.widgets.SpriteSwatch;
import uk.co.bithatch.drawzx.widgets.TilemapEditorGrid;
import uk.co.bithatch.drawzx.wizards.NewTilemapWizard;

/**
 * Eclipse editor for ZX Next tilemap files (.til).
 * 
 * <p>The editor layout consists of:</p>
 * <ul>
 *   <li>Left: tile definition swatch (reusing {@link SpriteSwatch}) for selecting
 *       which tile to place</li>
 *   <li>Center: the tilemap grid where tiles are placed</li>
 *   <li>Right: info panel showing tilemap properties and selected tile info</li>
 * </ul>
 */
public class TilemapEditor extends EditorPart implements IPartListener {

	protected Tilemap tilemap;
	protected TilemapEditorGrid tilemapGrid;
	protected SpriteSwatch tileSwatch;
	protected SpriteSheet tileDefinitions;

	private boolean dirty;
	private final IUndoContext undoContext = new ObjectUndoContext(this);
	private IOperationHistory history;
	private UndoActionHandler undoHandler;
	private RedoActionHandler redoHandler;
	private TilemapEntry[][] pendingSnapshot;

	// Info labels
	private Label tilemapModeLabel;
	private Label tilemapSizeLabel;
	private Label tilemapByteSizeLabel;
	private Label selectedTileLabel;
	private Combo modeCombo;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());

		var file = getFile();
		try (var in = file.getContents()) {
			// Determine tilemap mode from file size
			var bytes = in.readAllBytes();
			var fileSize = bytes.length;
			TilemapMode tilemapMode;
			boolean sixteenBit;

			if (fileSize == 40 * 32) {
				tilemapMode = TilemapMode.STANDARD_40x32;
				sixteenBit = false;
			} else if (fileSize == 40 * 32 * 2) {
				tilemapMode = TilemapMode.STANDARD_40x32;
				sixteenBit = true;
			} else if (fileSize == 80 * 32) {
				tilemapMode = TilemapMode.HIRES_80x32;
				sixteenBit = false;
			} else if (fileSize == 80 * 32 * 2) {
				tilemapMode = TilemapMode.HIRES_80x32;
				sixteenBit = true;
			} else {
				// Default to standard 40x32 16-bit
				tilemapMode = TilemapMode.STANDARD_40x32;
				sixteenBit = fileSize > 40 * 32;
			}

			// Try to load tile definitions from persistent property
			tileDefinitions = loadTileDefinitions(file);

			// Load tilemap
			var bais = new java.io.ByteArrayInputStream(bytes);
			tilemap = Tilemap.load(bais, tilemapMode, sixteenBit, tileDefinitions);

		} catch (IOException | CoreException e) {
			throw new PartInitException(Status.error("Failed to load tilemap.", e));
		}
	}

	/**
	 * Load tile definitions from the associated .TIL file (stored as a persistent
	 * property), or create default blank tile definitions if none is configured.
	 */
	private SpriteSheet loadTileDefinitions(IFile mapFile) {
		try {
			var tilPath = mapFile.getPersistentProperty(NewTilemapWizard.PROP_TIL_PATH);
			var bppStr = mapFile.getPersistentProperty(new QualifiedName("uk.co.bithatch.drawzx", "bpp"));
			int bpp = bppStr != null ? Integer.parseInt(bppStr) : 1;

			if (tilPath != null && !tilPath.isEmpty()) {
				// Try workspace-relative path first
				var wsRoot = ResourcesPlugin.getWorkspace().getRoot();
				var tilFile = wsRoot.getFile(new Path(tilPath));
				if (tilFile != null && tilFile.exists()) {
					try (var tilIn = tilFile.getContents()) {
						return SpriteSheet.load(tilIn, bpp, true);
					}
				}
				// Try as filesystem path
				var fsPath = java.nio.file.Path.of(tilPath);
				if (java.nio.file.Files.exists(fsPath)) {
					return SpriteSheet.load(fsPath, bpp, true);
				}
			}

			// Default: 256 blank 8x8 tiles
			return new SpriteSheet(256, bpp);
		} catch (Exception e) {
			// Fallback to default
			return new SpriteSheet(256, 1);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		setupUndo();

		var root = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(3, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		root.setLayout(layout);

		// Left: tile swatch for selecting tiles to place
		var swatchGroup = new Group(root, SWT.NONE);
		swatchGroup.setText("Tile Definitions");
		swatchGroup.setLayout(new GridLayout(1, false));
		swatchGroup.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).hint(200, SWT.DEFAULT).create());

		var swatchScroll = new ScrolledComposite(swatchGroup, SWT.V_SCROLL | SWT.BORDER);
		swatchScroll.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		swatchScroll.setExpandHorizontal(true);
		swatchScroll.setExpandVertical(true);

		tileSwatch = new SpriteSwatch(swatchScroll, tileDefinitions, 20, 8, SWT.NONE);
		swatchScroll.setContent(tileSwatch);
		swatchScroll.setMinSize(tileSwatch.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		tileSwatch.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var idx = tileSwatch.selected();
			tilemapGrid.selectedTileIndex(idx);
			if (selectedTileLabel != null) {
				selectedTileLabel.setText(String.valueOf(idx));
				selectedTileLabel.getParent().layout(true);
			}
		}));

		// Center: tilemap grid in a scrolled composite
		var gridScroll = new ScrolledComposite(root, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		gridScroll.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		gridScroll.setExpandHorizontal(true);
		gridScroll.setExpandVertical(true);

		tilemapGrid = new TilemapEditorGrid(gridScroll, tilemap, 16, SWT.NONE);
		gridScroll.setContent(tilemapGrid);
		gridScroll.setMinSize(tilemapGrid.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		tilemapGrid.addModifyListener(e -> {
			markDirty();
		});

		tilemapGrid.addDrawListener(new DrawListener() {
			@Override
			public void drawStarted() {
				pendingSnapshot = tilemap.snapshot();
			}

			@Override
			public void drawFinished() {
				if (pendingSnapshot != null) {
					var before = pendingSnapshot;
					var after = tilemap.snapshot();
					pendingSnapshot = null;
					execute(new TilemapDrawOperation(before, after));
				}
			}
		});

		// Right: info panel
		var infoPanel = new Composite(root, SWT.NONE);
		infoPanel.setLayout(new GridLayout(1, false));
		infoPanel.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).hint(180, SWT.DEFAULT).create());

		createInfoPanel(infoPanel);
		updateInfo();

		getSite().getWorkbenchWindow().getPartService().addPartListener(this);
	}

	private void createInfoPanel(Composite parent) {
		// Tilemap info group
		var tilemapInfo = new Group(parent, SWT.NONE);
		tilemapInfo.setText("Tilemap");
		var gl = new GridLayout(2, false);
		gl.marginWidth = 8;
		gl.marginHeight = 8;
		tilemapInfo.setLayout(gl);
		tilemapInfo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		new Label(tilemapInfo, SWT.NONE).setText("Mode:");
		tilemapModeLabel = new Label(tilemapInfo, SWT.BOLD);

		new Label(tilemapInfo, SWT.NONE).setText("Size:");
		tilemapSizeLabel = new Label(tilemapInfo, SWT.BOLD);

		new Label(tilemapInfo, SWT.NONE).setText("Bytes:");
		tilemapByteSizeLabel = new Label(tilemapInfo, SWT.BOLD);

		// Paint mode
		var toolGroup = new Group(parent, SWT.NONE);
		toolGroup.setText("Tool");
		toolGroup.setLayout(new GridLayout(2, false));
		toolGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		new Label(toolGroup, SWT.NONE).setText("Mode:");
		modeCombo = new Combo(toolGroup, SWT.READ_ONLY);
		modeCombo.setItems("Select", "Place Tile", "Fill");
		modeCombo.select(1); // Default to Place
		tilemapGrid.mode(TilemapEditorGrid.TilemapPaintMode.PLACE);
		modeCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			switch (modeCombo.getSelectionIndex()) {
			case 0 -> tilemapGrid.mode(TilemapEditorGrid.TilemapPaintMode.SELECT);
			case 1 -> tilemapGrid.mode(TilemapEditorGrid.TilemapPaintMode.PLACE);
			case 2 -> tilemapGrid.mode(TilemapEditorGrid.TilemapPaintMode.FILL);
			}
		}));

		// Selected tile info
		var tileInfo = new Group(parent, SWT.NONE);
		tileInfo.setText("Selected Tile");
		tileInfo.setLayout(new GridLayout(2, false));
		tileInfo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		new Label(tileInfo, SWT.NONE).setText("Index:");
		selectedTileLabel = new Label(tileInfo, SWT.BOLD);
		selectedTileLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		selectedTileLabel.setText("0");
	}

	private void updateInfo() {
		tilemapModeLabel.setText(tilemap.mode() == TilemapMode.STANDARD_40x32 ? "40x32 Standard" : "80x32 Hi-Res");
		tilemapSizeLabel.setText(String.format("%dx%d tiles", tilemap.columns(), tilemap.rows()));
		tilemapByteSizeLabel.setText(String.format("%d bytes", tilemap.byteSize()));
		tilemapModeLabel.getParent().layout(true);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		var file = getEditorInput().getAdapter(IFile.class);
		try {
			var bytes = new ByteArrayOutputStream();
			try (var wtr = Channels.newChannel(bytes)) {
				tilemap.save(wtr);
			}
			file.write(bytes.toByteArray(), false, false, true, monitor);
			markClean();
		} catch (CoreException | IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
		tilemapGrid.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		history.dispose(undoContext, true, true, true);
		getSite().getWorkbenchWindow().getPartService().removePartListener(this);
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
	}

	@Override
	public void partOpened(IWorkbenchPart part) {
	}

	private IFile getFile() {
		return getEditorInput().getAdapter(IFile.class);
	}

	private void markDirty() {
		if (!dirty) {
			dirty = true;
			firePropertyChange(PROP_DIRTY);
		}
	}

	private void markClean() {
		if (dirty) {
			dirty = false;
			firePropertyChange(PROP_DIRTY);
		}
	}

	private void setupUndo() {
		history = OperationHistoryFactory.getOperationHistory();
		history.setLimit(undoContext, 1000);
		undoHandler = new UndoActionHandler(getSite(), undoContext);
		redoHandler = new RedoActionHandler(getSite(), undoContext);
		var bars = getEditorSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoHandler);
		bars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoHandler);
		bars.updateActionBars();
	}

	private void execute(IUndoableOperation op) {
		op.addContext(undoContext);
		try {
			history.execute(op, null, null);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Cannot perform operation.", e);
		}
	}

	/**
	 * Undo/redo operation for tilemap drawing.
	 */
	private class TilemapDrawOperation extends AbstractOperation {
		private final TilemapEntry[][] before;
		private final TilemapEntry[][] after;

		public TilemapDrawOperation(TilemapEntry[][] before, TilemapEntry[][] after) {
			super("Draw Tiles");
			this.before = before;
			this.after = after;
		}

		@Override
		public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
			markDirty();
			return Status.OK_STATUS;
		}

		@Override
		public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
			tilemap.restore(before);
			tilemapGrid.notifyDataChanged();
			markDirty();
			return Status.OK_STATUS;
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
			tilemap.restore(after);
			tilemapGrid.notifyDataChanged();
			markDirty();
			return Status.OK_STATUS;
		}
	}
}
