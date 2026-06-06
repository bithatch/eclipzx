package uk.co.bithatch.drawzx.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
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
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.EditorPart;

import uk.co.bithatch.bitzx.ZXPerspectives;
import uk.co.bithatch.drawzx.Activator;
import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.drawzx.tilemaps.Tilemap;
import uk.co.bithatch.drawzx.tilemaps.Tilemap.TilemapMode;
import uk.co.bithatch.drawzx.tilemaps.TilemapEntry;
import uk.co.bithatch.drawzx.views.ISpriteView;
import uk.co.bithatch.drawzx.views.SpriteView;
import uk.co.bithatch.drawzx.widgets.DrawListener;
import uk.co.bithatch.drawzx.widgets.TilemapEditorGrid;
import uk.co.bithatch.drawzx.wizards.NewTilemapWizard;
import uk.co.bithatch.widgetzx.FontStyleHelper;
import uk.co.bithatch.widgetzx.ZXPerspectivesUI;

/**
 * Eclipse editor for ZX Next tilemap files (.til).
 * 
 * <p>The editor layout consists of:</p>
 * <ul>
 *   <li>Left: tile definition swatch shown in the SpriteView (selecting
 *       which tile to place)</li>
 *   <li>Center: the tilemap grid where tiles are placed</li>
 *   <li>Right: info panel showing tilemap properties and selected tile info</li>
 * </ul>
 */
public class TilemapEditor extends EditorPart implements IPartListener, ISpriteSwatchEditor {

	protected Tilemap tilemap;
	protected TilemapEditorGrid tilemapGrid;
	protected SpriteSheet tileDefinitions;
	private ScrolledComposite gridScroll;
	private IResourceChangeListener resourceChangeListener;

	private boolean dirty;
	private final IUndoContext undoContext = new ObjectUndoContext(this);
	private IOperationHistory history;
	private UndoActionHandler undoHandler;
	private RedoActionHandler redoHandler;
	private TilemapEntry[][] pendingSnapshot;

	// Tile property controls
	private org.eclipse.swt.widgets.Spinner palOffsetSpinner;
	private org.eclipse.swt.widgets.Button mirrorXCheck;
	private org.eclipse.swt.widgets.Button mirrorYCheck;
	private org.eclipse.swt.widgets.Button rotateCheck;
	private org.eclipse.swt.widgets.Button ulaOverCheck;
	private boolean updatingTileProps;
	private org.eclipse.swt.graphics.Rectangle selectedEntryCell;

	// Info labels
	private Label tilemapModeLabel;
	private Label tilemapSizeLabel;
	private Label tilemapByteSizeLabel;
	private Label selectedTileLabel;
	private org.eclipse.swt.widgets.Link tilDefPathLink;

	// Sprite view link
	private ISpriteView spriteView;
	private int selectedTileIndex;
	private FontStyleHelper fontStyleHelper;

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
			int bpp = bppStr != null ? Integer.parseInt(bppStr) : 4;

			if (tilPath != null && !tilPath.isEmpty()) {
				// Try workspace-relative path first
				var wsRoot = ResourcesPlugin.getWorkspace().getRoot();
				var tilFile = wsRoot.getFile(new Path(tilPath));
				if (tilFile != null && tilFile.exists()) {
					try (var tilIn = tilFile.getContents()) {
						var sheet = SpriteSheet.load(tilIn, bpp, true);
						// Tile definitions are always 8x8, but load() may use larger cell size
						if (sheet.cellSize() != 8) {
							sheet = sheet.withCellSize(8);
						}
						return sheet;
					}
				}
				// Try as filesystem path
				var fsPath = java.nio.file.Path.of(tilPath);
				if (java.nio.file.Files.exists(fsPath)) {
					var sheet = SpriteSheet.load(fsPath, bpp, true);
					if (sheet.cellSize() != 8) {
						sheet = sheet.withCellSize(8);
					}
					return sheet;
				}
			}

			// Default: 256 blank 8x8 tiles
			return new SpriteSheet(256, bpp);
		} catch (Exception e) {
			// Fallback to default
			return new SpriteSheet(256, 4);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		setupUndo();

		var root = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(2, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		root.setLayout(layout);
		
		fontStyleHelper = new FontStyleHelper(root);

		// Center: tilemap grid in a scrolled composite
		gridScroll = new ScrolledComposite(root, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		gridScroll.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		gridScroll.setExpandHorizontal(true);
		gridScroll.setExpandVertical(true);
		gridScroll.setBackground(root.getBackground());

		tilemapGrid = new TilemapEditorGrid(gridScroll, tilemap, 16, SWT.NONE);
		tilemapGrid.setBackground(root.getBackground());
		gridScroll.setContent(tilemapGrid);
		updateGridSize();

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
		infoPanel.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).hint(240, SWT.DEFAULT).create());

		createInfoPanel(infoPanel);
		updateInfo();

		getSite().getWorkbenchWindow().getPartService().addPartListener(this);

		// Listen for .til file changes
		resourceChangeListener = this::handleResourceChanged;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
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

		// Tile definition file link and drop target
		fontStyleHelper.bold(new Label(tilemapInfo, SWT.NONE)).setText("Definitions:");
		tilDefPathLink = new org.eclipse.swt.widgets.Link(tilemapInfo, SWT.WRAP);
//		tilDefPathLink.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		tilDefPathLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> openTilFile()));
		updateTilDefPathLabel();

		fontStyleHelper.bold(new Label(tilemapInfo, SWT.NONE)).setText("Mode:");
		tilemapModeLabel = new Label(tilemapInfo, SWT.BOLD);

		fontStyleHelper.bold(new Label(tilemapInfo, SWT.NONE)).setText("Size:");
		tilemapSizeLabel = new Label(tilemapInfo, SWT.BOLD);

		fontStyleHelper.bold(new Label(tilemapInfo, SWT.NONE)).setText("Bytes:");
		tilemapByteSizeLabel = new Label(tilemapInfo, SWT.BOLD);

		// Restore last mode from persistent property, default to SELECT
		var savedMode = TilemapEditorGrid.TilemapPaintMode.SELECT;
		try {
			var modeStr = getFile().getPersistentProperty(
				new QualifiedName("uk.co.bithatch.drawzx", "tilemapMode"));
			if (modeStr != null) {
				savedMode = TilemapEditorGrid.TilemapPaintMode.valueOf(modeStr);
			}
		} catch (Exception e) {
			// ignore
		}
		tilemapGrid.mode(savedMode);

		// Set up drag-and-drop for .til files
		setupTilDropTarget(tilemapInfo);

		// Entry properties group
		var propsGroup = new Group(parent, SWT.NONE);
		propsGroup.setText("Tile");
		propsGroup.setLayout(new GridLayout(2, false));
		propsGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		fontStyleHelper.bold(new Label(propsGroup, SWT.NONE)).setText("Index:");
		selectedTileLabel = new Label(propsGroup, SWT.BOLD);
		selectedTileLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		selectedTileLabel.setText("0");

		fontStyleHelper.bold(new Label(propsGroup, SWT.NONE)).setText("Palette Offset:");
		palOffsetSpinner = new org.eclipse.swt.widgets.Spinner(propsGroup, SWT.BORDER);
		palOffsetSpinner.setMinimum(0);
		palOffsetSpinner.setMaximum(15);
		palOffsetSpinner.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		palOffsetSpinner.setEnabled(false);
		palOffsetSpinner.addModifyListener(e -> onEntryPropertyChanged());

		mirrorXCheck = new org.eclipse.swt.widgets.Button(propsGroup, SWT.CHECK);
		mirrorXCheck.setText("Mirror X");
		mirrorXCheck.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
		mirrorXCheck.setEnabled(false);
		mirrorXCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> onEntryPropertyChanged()));

		mirrorYCheck = new org.eclipse.swt.widgets.Button(propsGroup, SWT.CHECK);
		mirrorYCheck.setText("Mirror Y");
		mirrorYCheck.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
		mirrorYCheck.setEnabled(false);
		mirrorYCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> onEntryPropertyChanged()));

		rotateCheck = new org.eclipse.swt.widgets.Button(propsGroup, SWT.CHECK);
		rotateCheck.setText("Rotate");
		rotateCheck.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
		rotateCheck.setEnabled(false);
		rotateCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> onEntryPropertyChanged()));

		ulaOverCheck = new org.eclipse.swt.widgets.Button(propsGroup, SWT.CHECK);
		ulaOverCheck.setText("ULA over Tilemap");
		ulaOverCheck.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
		ulaOverCheck.setEnabled(false);
		ulaOverCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> onEntryPropertyChanged()));

		// Listen for grid selection changes to populate the properties
		tilemapGrid.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var sel = tilemapGrid.selection();
			if (sel != null && sel.width == 1 && sel.height == 1) {
				selectedEntryCell = sel;
				updateEntryProperties();
			} else {
				selectedEntryCell = null;
				setEntryPropsEnabled(false);
			}
		}));
	}

	private void updateEntryProperties() {
		if (selectedEntryCell == null) return;
		updatingTileProps = true;
		try {
			var entry = tilemap.entry(selectedEntryCell.x, selectedEntryCell.y);
			palOffsetSpinner.setSelection(entry.paletteOffset());
			mirrorXCheck.setSelection(entry.mirrorX());
			mirrorYCheck.setSelection(entry.mirrorY());
			rotateCheck.setSelection(entry.rotate());
			ulaOverCheck.setSelection(entry.ulaOverTilemap());
			selectedTileLabel.setText(String.valueOf(entry.tileIndex()));
			setEntryPropsEnabled(true);
		} finally {
			updatingTileProps = false;
		}
	}

	private void setEntryPropsEnabled(boolean enabled) {
		palOffsetSpinner.setEnabled(enabled);
		mirrorXCheck.setEnabled(enabled);
		mirrorYCheck.setEnabled(enabled);
		rotateCheck.setEnabled(enabled);
		ulaOverCheck.setEnabled(enabled);
	}

	private void onEntryPropertyChanged() {
		if (updatingTileProps || selectedEntryCell == null) return;
		var before = tilemap.snapshot();
		var entry = tilemap.entry(selectedEntryCell.x, selectedEntryCell.y);
		entry.paletteOffset(palOffsetSpinner.getSelection());
		entry.mirrorX(mirrorXCheck.getSelection());
		entry.mirrorY(mirrorYCheck.getSelection());
		entry.rotate(rotateCheck.getSelection());
		entry.ulaOverTilemap(ulaOverCheck.getSelection());
		var after = tilemap.snapshot();
		execute(new TilemapDrawOperation(before, after));
		tilemapGrid.invalidateCache();
		tilemapGrid.redraw();
		markDirty();
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
		if (resourceChangeListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		}
		super.dispose();
		history.dispose(undoContext, true, true, true);
		getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		fontStyleHelper.dispose();
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		if (part.equals(this)) {
			if(ZXPerspectivesUI.isPerspective(ZXPerspectives.ZX_MEDIA_ID)) {
				openSpriteView();
			}
			else {
				ZXPerspectivesUI.zxMediaPerspective(Activator.PLUGIN_ID);
			}
		}
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

	// --- ISpriteSwatchEditor implementation ---

	@Override
	public SpriteSheet spriteSheet() {
		return tileDefinitions;
	}

	@Override
	public int swatchColumns() {
		return tileDefinitions != null && tileDefinitions.size() > 256 ? 16 : 8;
	}

	@Override
	public int swatchCellSize() {
		return tileDefinitions != null && tileDefinitions.size() > 256 ? 16 : 20;
	}

	@Override
	public int[] cellSizes() {
		// Tile definitions are always 8x8
		return new int[0];
	}

	@Override
	public int selectedSpriteIndex() {
		return selectedTileIndex;
	}

	@Override
	public void selectSprite(int index) {
		selectedTileIndex = index;
		tilemapGrid.selectedTileIndex(index);
		if (selectedTileLabel != null) {
			selectedTileLabel.setText(String.valueOf(index));
			selectedTileLabel.getParent().layout(true);
		}
	}

	@Override
	public void cellSizeChanged(int newCellSize) {
		// Not applicable for tilemap tile definitions
	}

	@Override
	public void spriteView(ISpriteView spriteView) {
		this.spriteView = spriteView;
	}

	@Override
	public boolean showPreviews() {
		return false;
	}

	// --- End ISpriteSwatchEditor ---

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
			updateEntryProperties();
			return Status.OK_STATUS;
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
			tilemap.restore(after);
			tilemapGrid.notifyDataChanged();
			markDirty();
			updateEntryProperties();
			return Status.OK_STATUS;
		}
	}

	public void zoomIn() {
		var current = tilemapGrid.cellPixelSize();
		tilemapGrid.cellPixelSize(current + 4);
		updateGridSize();
	}

	public void zoomOut() {
		var current = tilemapGrid.cellPixelSize();
		if (current > 4) {
			tilemapGrid.cellPixelSize(current - 4);
			updateGridSize();
		}
	}

	public void resetZoom() {
		tilemapGrid.cellPixelSize(16);
		updateGridSize();
	}

	public void setMode(TilemapEditorGrid.TilemapPaintMode mode) {
		tilemapGrid.mode(mode);
		try {
			getFile().setPersistentProperty(
				new QualifiedName("uk.co.bithatch.drawzx", "tilemapMode"), mode.name());
		} catch (Exception e) {
			// ignore
		}
		var cmdService = getSite().getService(org.eclipse.ui.commands.ICommandService.class);
		if (cmdService != null) {
			cmdService.refreshElements("uk.co.bithatch.drawzx.tilemaps.commands.mode", null);
		}
	}

	public TilemapEditorGrid.TilemapPaintMode getMode() {
		return tilemapGrid.mode();
	}

	public boolean hasSelection() {
		return tilemapGrid.hasSelection();
	}

	/**
	 * Clipboard format: TILEMAP_CLIP:cols:rows\n
	 * Each row is comma-separated entries, rows separated by newlines.
	 * Each entry: tileIndex:attrByte (hex)
	 */
	public void copySelectionToClipboard() {
		var sel = tilemapGrid.selection();
		if (sel == null) return;
		var sb = new StringBuilder();
		sb.append("TILEMAP_CLIP:").append(sel.width).append(':').append(sel.height).append('\n');
		for (var r = 0; r < sel.height; r++) {
			for (var c = 0; c < sel.width; c++) {
				if (c > 0) sb.append(',');
				var entry = tilemap.entry(sel.x + c, sel.y + r);
				var encoded = entry.encode16bit();
				sb.append(String.format("%02X%02X", encoded[0] & 0xFF, encoded[1] & 0xFF));
			}
			sb.append('\n');
		}
		var clipboard = new org.eclipse.swt.dnd.Clipboard(getSite().getShell().getDisplay());
		try {
			clipboard.setContents(
				new Object[] { sb.toString() },
				new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() }
			);
		} finally {
			clipboard.dispose();
		}
	}

	public void cutSelectionToClipboard() {
		copySelectionToClipboard();
		deleteSelection();
	}

	public void deleteSelection() {
		var sel = tilemapGrid.selection();
		if (sel == null) return;
		pendingSnapshot = tilemap.snapshot();
		for (var r = 0; r < sel.height; r++) {
			for (var c = 0; c < sel.width; c++) {
				var entry = tilemap.entry(sel.x + c, sel.y + r);
				entry.tileIndex(0);
				entry.mirrorX(false);
				entry.mirrorY(false);
				entry.rotate(false);
				entry.ulaOverTilemap(false);
				entry.paletteOffset(0);
			}
		}
		var after = tilemap.snapshot();
		execute(new TilemapDrawOperation(pendingSnapshot, after));
		pendingSnapshot = null;
		tilemapGrid.invalidateCache();
		tilemapGrid.redraw();
		markDirty();
	}

	public void pasteFromClipboard() {
		var clipboard = new org.eclipse.swt.dnd.Clipboard(getSite().getShell().getDisplay());
		try {
			var text = (String) clipboard.getContents(org.eclipse.swt.dnd.TextTransfer.getInstance());
			if (text == null || !text.startsWith("TILEMAP_CLIP:")) return;

			var lines = text.split("\n");
			var header = lines[0];
			var parts = header.split(":");
			if (parts.length < 3) return;
			var cols = Integer.parseInt(parts[1]);
			var rows = Integer.parseInt(parts[2]);

			var entries = new uk.co.bithatch.drawzx.tilemaps.TilemapEntry[rows][cols];
			for (var r = 0; r < rows && r + 1 < lines.length; r++) {
				var cells = lines[r + 1].split(",");
				for (var c = 0; c < cols && c < cells.length; c++) {
					var hex = cells[c].trim();
					if (hex.length() >= 4) {
						var attr = Integer.parseInt(hex.substring(0, 2), 16);
						var tile = Integer.parseInt(hex.substring(2, 4), 16);
						entries[r][c] = uk.co.bithatch.drawzx.tilemaps.TilemapEntry.decode16bit(attr, tile);
					} else {
						entries[r][c] = new uk.co.bithatch.drawzx.tilemaps.TilemapEntry(0);
					}
				}
				// Fill remaining columns with empty
				for (var c = cells.length; c < cols; c++) {
					entries[r][c] = new uk.co.bithatch.drawzx.tilemaps.TilemapEntry(0);
				}
			}

			// Show paste preview - user clicks to place
			tilemapGrid.showPastePreview(entries, cols, rows);
		} finally {
			clipboard.dispose();
		}
	}

	private void updateGridSize() {
		var size = tilemapGrid.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		tilemapGrid.setSize(size);
		gridScroll.setMinSize(size);
		gridScroll.layout(true, true);
	}

	private void handleResourceChanged(IResourceChangeEvent event) {
		if (event.getDelta() == null) return;
		try {
			var tilPath = getFile().getPersistentProperty(NewTilemapWizard.PROP_TIL_PATH);
			if (tilPath == null || tilPath.isEmpty()) return;

			event.getDelta().accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) {
					if (delta.getKind() == IResourceDelta.CHANGED
							&& (delta.getFlags() & IResourceDelta.CONTENT) != 0
							&& delta.getResource() instanceof IFile changedFile) {
						var changedPath = changedFile.getFullPath().toString();
						if (changedPath.equals(tilPath) || (changedFile.getLocation() != null
								&& changedFile.getLocation().toOSString().equals(tilPath))) {
							getSite().getShell().getDisplay().asyncExec(() -> reloadTileDefinitions());
						}
					}
					return true;
				}
			});
		} catch (Exception e) {
			// Ignore
		}
	}

	private void reloadTileDefinitions() {
		if (tilemapGrid == null || tilemapGrid.isDisposed()) return;
		try {
			var newDefs = loadTileDefinitions(getFile());
			tileDefinitions = newDefs;
			tilemap.tileDefinitions(tileDefinitions);

			// Update the sprite view swatch
			if (spriteView != null) {
				spriteView.updateSpriteSheet(tileDefinitions, swatchColumns(), swatchCellSize());
			}

			// Refresh tilemap grid
			tilemapGrid.invalidateAllCaches();
			tilemapGrid.redraw();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateTilDefPathLabel() {
		try {
			var tilPath = getFile().getPersistentProperty(NewTilemapWizard.PROP_TIL_PATH);
			if (tilPath != null && !tilPath.isEmpty()) {
				var name = java.nio.file.Path.of(tilPath).getFileName().toString();
				tilDefPathLink.setText("<a>" + name + "</a>");
				tilDefPathLink.setToolTipText(tilPath);
			} else {
				tilDefPathLink.setText("Drop .til file here");
				tilDefPathLink.setToolTipText("Drag and drop a .til tile definition file to assign it to this tilemap");
			}
		} catch (CoreException e) {
			tilDefPathLink.setText("Drop .til file here");
		}
	}

	private void openTilFile() {
		try {
			var tilPath = getFile().getPersistentProperty(NewTilemapWizard.PROP_TIL_PATH);
			if (tilPath == null || tilPath.isEmpty()) return;

			// Try workspace-relative path first
			var wsRoot = ResourcesPlugin.getWorkspace().getRoot();
			var tilFile = wsRoot.getFile(new Path(tilPath));
			if (tilFile != null && tilFile.exists()) {
				var page = getSite().getPage();
				org.eclipse.ui.ide.IDE.openEditor(page, tilFile);
				return;
			}

			// Try as filesystem path - find workspace file by URI
			var fsPath = java.nio.file.Path.of(tilPath);
			if (java.nio.file.Files.exists(fsPath)) {
				var wsFiles = wsRoot.findFilesForLocationURI(fsPath.toUri());
				if (wsFiles != null && wsFiles.length > 0) {
					var page = getSite().getPage();
					org.eclipse.ui.ide.IDE.openEditor(page, wsFiles[0]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setupTilDropTarget(Group tilDefGroup) {
		var fileTransfer = FileTransfer.getInstance();
		var dropTarget = new DropTarget(tilDefGroup, DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(new Transfer[] { fileTransfer });
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void dragEnter(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT) {
					event.detail = DND.DROP_COPY;
				}
			}

			@Override
			public void drop(DropTargetEvent event) {
				if (fileTransfer.isSupportedType(event.currentDataType)) {
					var files = (String[]) event.data;
					if (files != null && files.length > 0) {
						var path = files[0];
						if (path.toLowerCase().endsWith(".til")) {
							applyTilFile(path);
						}
					}
				}
			}
		});
	}

	private void applyTilFile(String fsPath) {
		try {
			var tilPath = java.nio.file.Path.of(fsPath);
			if (!java.nio.file.Files.exists(tilPath)) return;

			// Determine BPP from existing property or default
			var mapFile = getFile();
			var bppStr = mapFile.getPersistentProperty(new QualifiedName("uk.co.bithatch.drawzx", "bpp"));
			int bpp = bppStr != null ? Integer.parseInt(bppStr) : 4;

			// Load new tile definitions
			var newDefs = SpriteSheet.load(tilPath, bpp, true);
			// Tile definitions are always 8x8
			if (newDefs.cellSize() != 8) {
				newDefs = newDefs.withCellSize(8);
			}

			// Try to find workspace-relative path
			var wsRoot = ResourcesPlugin.getWorkspace().getRoot();
			var wsFiles = wsRoot.findFilesForLocationURI(tilPath.toUri());
			String storedPath;
			if (wsFiles != null && wsFiles.length > 0) {
				storedPath = wsFiles[0].getFullPath().toString();
			} else {
				storedPath = fsPath;
			}

			// Store the association
			mapFile.setPersistentProperty(NewTilemapWizard.PROP_TIL_PATH, storedPath);

			// Update in-memory state
			tileDefinitions = newDefs;
			tilemap.tileDefinitions(tileDefinitions);

			// Update the sprite view swatch
			if (spriteView != null) {
				spriteView.updateSpriteSheet(tileDefinitions, swatchColumns(), swatchCellSize());
			}

			// Update label
			updateTilDefPathLabel();

			// Refresh tilemap grid
			tilemapGrid.invalidateAllCaches();
			tilemapGrid.redraw();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void openSpriteView() {
		var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			var page = window.getActivePage();
			if (page != null) {
				try {
					page.showView(SpriteView.ID, null, org.eclipse.ui.IWorkbenchPage.VIEW_VISIBLE);
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
