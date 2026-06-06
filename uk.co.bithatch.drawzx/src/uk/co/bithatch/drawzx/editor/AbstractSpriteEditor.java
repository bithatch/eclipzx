package uk.co.bithatch.drawzx.editor;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.EditorPart;

import uk.co.bithatch.bitzx.ZXPerspectives;
import uk.co.bithatch.drawzx.Activator;
import uk.co.bithatch.drawzx.editor.EditorFileProperties.PaletteSource;
import uk.co.bithatch.drawzx.editor.EditorFileProperties.SpritePaintMode;
import uk.co.bithatch.drawzx.sprites.ClearOperation;
import uk.co.bithatch.drawzx.sprites.ColorSelectOperation;
import uk.co.bithatch.drawzx.sprites.CutOperation;
import uk.co.bithatch.drawzx.sprites.DrawOperation;
import uk.co.bithatch.drawzx.sprites.InvertOperation;
import uk.co.bithatch.drawzx.sprites.MirrorHOperation;
import uk.co.bithatch.drawzx.sprites.MirrorVOperation;
import uk.co.bithatch.drawzx.sprites.PasteOperation;
import uk.co.bithatch.drawzx.sprites.RotateOperation;
import uk.co.bithatch.drawzx.sprites.ShiftOperation;
import uk.co.bithatch.drawzx.sprites.SpriteCell;
import uk.co.bithatch.drawzx.sprites.SpriteCellSelectOperation;
import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.drawzx.views.ColourPickerView;
import uk.co.bithatch.drawzx.views.IColourPicker;
import uk.co.bithatch.drawzx.views.ISpriteView;
import uk.co.bithatch.drawzx.views.SpriteView;
import uk.co.bithatch.drawzx.widgets.DrawListener;
import uk.co.bithatch.drawzx.widgets.SpriteEditorGrid;
import uk.co.bithatch.widgetzx.ZXPerspectivesUI;
import uk.co.bithatch.zyxy.graphics.Palette;

public abstract class AbstractSpriteEditor extends EditorPart implements IPartListener, IColouredEditor, ISpriteSwatchEditor {

	protected SpriteCell spriteCell;
	protected SpriteEditorGrid spriteGrid;
	protected SpriteSheet spriteSheet;

	private Clipboard clipboard;
	private final IResourceDeltaVisitor deltaVisitor = new IResourceDeltaVisitor() {
		@Override
		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();
			if (resource instanceof IFile file) {
				if ("pal".equalsIgnoreCase(file.getFileExtension())
						|| "npl".equalsIgnoreCase(file.getFileExtension())) {
					if (file.equals(getPaletteFile())) {
						Display.getDefault().asyncExec(() -> {
							try {
								reloadPalette();
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
					}
				}
			}
			return true;
		}
	};

	private boolean dirty;
	private PersistentPropertyWatcher propertyWatcher;
	private Composite root;

	private final IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			try {
				event.getDelta().accept(deltaVisitor);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	};
	
	protected IColourPicker picker;
	protected ISpriteView spriteView;
	protected final IUndoContext undoContext = new ObjectUndoContext(this);
	protected IOperationHistory history;
	private UndoActionHandler undoHandler;
	private RedoActionHandler redoHandler;
	private DrawOperation pendingDrawOp;
	private int currentCellIndex;

	public void clear() {
		execute(new ClearOperation(spriteGrid, this::markDirty));
	}

	public void copySelectionToClipboard() {
		var text = selectionString(spriteGrid.copyPixels());
		var textTransfer = TextTransfer.getInstance();
		clipboard.setContents(new Object[] { text }, new Transfer[] { textTransfer });
		spriteGrid.deselect();
	}

	@Override
	public final void createPartControl(Composite parent) {
		
		setupUndo();
		
		root = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		root.setLayout(layout);

		createEditorLayout(root);
		setupEditorListeners();

		propertyWatcher = new PersistentPropertyWatcher(getFile(), (key, val) -> {
			if (key.equals(EditorFileProperties.EDITOR_MODE_PROPERTY)) {
				root.getDisplay().execute(() -> spriteGrid.mode(val == null ? SpritePaintMode.SELECT : SpritePaintMode.valueOf(val.toUpperCase())));
				;
			} else if (key.equals(EditorFileProperties.PALETTE_PROPERTY)
					|| key.equals(EditorFileProperties.PALETTE_SOURCE_PROPERTY)) {
				root.getDisplay().execute(() -> {
					try {
						reloadPalette();
					} catch (CoreException e) {
						e.printStackTrace();

					}
				});
			}

		}, EditorFileProperties.EDITOR_MODE_PROPERTY);

		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener,
				IResourceChangeEvent.POST_CHANGE
		);

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(this);


		try {
			reloadPalette();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IUndoContext.class) {
			return (T) undoContext;
		}
		return super.getAdapter(adapter);
	}

	public IUndoContext getUndoContext() {
		return undoContext;
	}

	public void cutSelectionToClipboard() {
		execute(new CutOperation(spriteGrid, this::markDirty, () -> {
			var text = selectionString(spriteGrid.cutPixels());
			var textTransfer = TextTransfer.getInstance();
			clipboard.setContents(new Object[] { text }, new Transfer[] { textTransfer });
			spriteGrid.deselect();
		}));
	}

	@Override
	public void dispose() {
		super.dispose();
		history.dispose(undoContext, true, true, true);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		clipboard.dispose();
		if(propertyWatcher != null) {
			propertyWatcher.dispose();
		}
	}

	@Override
	public final void doSave(IProgressMonitor monitor) {

		var file = getEditorInput().getAdapter(IFile.class);
		try {
			var bytes = new ByteArrayOutputStream();
			try (var wtr = Channels.newChannel(bytes)) {
				spriteSheet.save(wtr);
			}

			file.write(bytes.toByteArray(), false, false, true, monitor);
			markClean();
		} catch (CoreException | IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public final void doSaveAs() {
	}

	@Override
	public IFile getFile() {
		return getEditorInput().getAdapter(IFile.class);
	}

	public boolean hasSelection() {
		return spriteGrid.hasSelection();
	}

	@Override
	public Palette palette() {
		return spriteSheet.palette();
	}

	@Override
	public final void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());

		clipboard = new Clipboard(site.getWorkbenchWindow().getWorkbench().getDisplay());

		var file = getFile();
		try {
			var configuredCellSize = EditorFileProperties.getIntProperty(getFile(), EditorFileProperties.CELL_SIZE_PROPERTY, cellSizes()[0]);
			spriteSheet = loadSheet(file.getContents());
			if(spriteSheet.cellSize() != configuredCellSize) {
				spriteSheet = spriteSheet.withCellSize(configuredCellSize);
			}
		} catch (IOException ioe) {
			throw new PartInitException(Status.error("Failed to load spritesheet.", ioe));
		} catch (CoreException e) {
			throw new PartInitException(e.getStatus());
		}
		spriteCell = spriteSheet.cell(0);
	}

	public void invertSprite() {
		execute(new InvertOperation(spriteGrid, this::markDirty));
	}

	@Override
	public final boolean isDirty() {
		return dirty;
	}

	@Override
	public final boolean isSaveAsAllowed() {
		return false;
	}

	public void mirrorSpriteH() {
		execute(new MirrorHOperation(spriteGrid, this::markDirty));
	}

	public void mirrorSpriteV() {
		execute(new MirrorVOperation(spriteGrid, this::markDirty));
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		if (part.equals(this)) {
			var cmdService = PlatformUI.getWorkbench().getService(ICommandService.class);
			if (cmdService != null) {
				cmdService.refreshElements("uk.co.bithatch.drawzx.sprites.commands.mode", null);
			}
			
			if(ZXPerspectivesUI.isPerspective(ZXPerspectives.ZX_MEDIA_ID)) {
				openColourPickerView(PlatformUI.getWorkbench());
				openSpriteView(PlatformUI.getWorkbench());
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

	public void pasteFromClipboard() {
		var textTransfer = TextTransfer.getInstance();
		var text = (String) clipboard.getContents(textTransfer);
		if (text != null) {
			var lines = text.split("\\n");
			try {
				var pixels = new int[lines.length][];
				for (var i = 0; i < lines.length; i++) {
					var parts = lines[i].trim().split("\\s+");
					pixels[i] = new int[parts.length];
					for (var j = 0; j < parts.length; j++) {
						pixels[i][j] = Integer.parseInt(parts[j]);
					}
				}
				execute(new PasteOperation(spriteGrid, pixels, this::markDirty));
			} catch (NumberFormatException nfe) {
				// TODO some indication
			}
		}
	}

	public void rotate(int degrees) {
		execute(new RotateOperation(spriteGrid, degrees, this::markDirty));
	}

	@Override
	public final void setFocus() {
		spriteGrid.setFocus();
	}

	public void setSpriteSheet(SpriteSheet spriteSheet) {
		this.spriteSheet = spriteSheet;
		var idx = Math.min(currentCellIndex, spriteSheet.size() - 1);
		currentCellIndex = idx;
		selectCell(spriteSheet.cell(idx));
		if (spriteView != null) {
			spriteView.updateSpriteSheet(spriteSheet, swatchColumns(), swatchCellSize());
			spriteView.updateSpriteSheetInfo();
			spriteView.updateSelection(idx);
		}
		if(picker != null)
			picker.palette(spriteSheet.palette());
	}

	@Override
	public void picker(IColourPicker picker) {
		this.picker = picker;		
	}

	// --- ISpriteSwatchEditor implementation ---

	@Override
	public SpriteSheet spriteSheet() {
		return spriteSheet;
	}

	@Override
	public int swatchColumns() {
		return swatchColumnsFor(spriteSheet);
	}

	@Override
	public int swatchCellSize() {
		return swatchCellSizeFor(spriteSheet);
	}

	@Override
	public int selectedSpriteIndex() {
		return currentCellIndex;
	}

	@Override
	public void selectSprite(int index) {
		var oldIndex = currentCellIndex;
		if (oldIndex != index) {
			execute(new SpriteCellSelectOperation(oldIndex, index, this::applyCellSelection));
		}
	}

	@Override
	public void cellSizeChanged(int newCellSize) {
		EditorFileProperties.setProperty(getFile(), EditorFileProperties.CELL_SIZE_PROPERTY, String.valueOf(newCellSize));
		setSpriteSheet(spriteSheet.withCellSize(newCellSize));
	}

	@Override
	public void spriteView(ISpriteView spriteView) {
		this.spriteView = spriteView;
	}

	// --- End ISpriteSwatchEditor ---

	public void shift(int h, int v) {
		execute(new ShiftOperation(spriteGrid, h, v, this::markDirty));
	}

	@Override
	public void colorSelected(int index, boolean primary) {
		var currentColor = primary ? spriteGrid.color() : spriteGrid.secondaryColor();
		if (currentColor != index) {
			execute(new ColorSelectOperation(spriteGrid, primary, currentColor, index, () -> {
				EditorFileProperties.setProperty(getFile(),
						primary ? EditorFileProperties.PALETTE_PRIMARY_PROPERTY : EditorFileProperties.PALETTE_SECONDARY_PROPERTY,
						primary ? spriteGrid.color() : spriteGrid.secondaryColor());
				if (picker != null) {
					picker.updatePaletteInfo();
				}
			}));
		}
	}

	protected void createButtons(Composite buttons) {
		var openPalette = new Button(buttons, SWT.PUSH);
		openPalette.setToolTipText("Click to select a palette from your workspace.");
		openPalette.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
		openPalette.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {

			var dialog = new ElementTreeSelectionDialog(root.getShell(), new WorkbenchLabelProvider(),
					new WorkbenchContentProvider());

			dialog.setTitle("Select Palette or Pattern File");
			dialog.setMessage("Select a .npl or .pal file from the workspace:");
			dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
			dialog.setAllowMultiple(false);

			dialog.addFilter(new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (element instanceof IFile file) {
						var ext = file.getFileExtension();
						return ext != null && (ext.equalsIgnoreCase("npl") || ext.equalsIgnoreCase("pal"));
					} else if (element instanceof IContainer) {
						return true;
					}
					return false;
				}
			});

			if (dialog.open() == Window.OK) {
				EditorFileProperties.setProperty(getFile(), EditorFileProperties.PALETTE_SOURCE_PROPERTY,
						PaletteSource.FILE.name());
				EditorFileProperties.setProperty(getFile(), EditorFileProperties.PALETTE_PROPERTY,
						((IFile) dialog.getFirstResult()).getFullPath().toString());
			}

		}));
	}

	@Override
	public boolean isPaletteOffsetUsed() {
		return false;
	}

	@Override
	public boolean isPaletteHistoryUsed() {
		return false;
	}

	@Override
	public void currentPaletteUpdate() {
		if(spriteView != null) {
			spriteView.updateSpriteSheetInfo();
		}
	}

	protected GridLayout createGroupLayout(int cols, boolean equal) {
		var l = new GridLayout(cols, equal);
		l.verticalSpacing = 8;
		l.marginWidth = 8;
		l.marginHeight = 8;
		return l;
	}

	@Override
	public int[] cellSizes() {
		return new int[] { 8 };
	}

	protected int swatchColumnsFor(SpriteSheet sheet) {
		return 16;
	}

	protected int swatchCellSizeFor(SpriteSheet sheet) {
		return 24;
	}

	@Override
	public int paletteCellSize() {
		return 16;
	}

	@Override
	public int defaultPaletteIndex() {
		return 0;
	}

	@Override
	public int defaultSecondaryPaletteIndex() {
		return 1;
	}

	@Override
	public boolean isPalettedChangeAllowed() {
		return false;
	}

	protected void createEditorLayout(Composite root) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		root.setLayout(layout);

		spriteGrid = new SpriteEditorGrid(root, spriteCell, SWT.BORDER);
		spriteGrid.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
	}

	protected void loadPaletteFile(IFile file) throws CoreException {
		try (var in = Channels.newChannel(file.getContents())) {
			setSpriteSheet(spriteSheet.withPalette(Palette.load(in, 256)));
		} catch (IOException ioe) {
			throw new CoreException(Status.error("Failed to load palette.", ioe));
		}
	}

	protected void execute(IUndoableOperation op) {
		op.addContext(getUndoContext());
		try {
			history.execute(op, null, null);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Cannot perform operation.", e);
		}
	}

	protected abstract SpriteSheet loadSheet(InputStream file) throws IOException;

	protected void markClean() {
		if (dirty) {
			dirty = false;
			firePropertyChange(PROP_DIRTY);
		}
	}

	protected void markDirty() {
		if (!dirty) {
			dirty = true;
			firePropertyChange(PROP_DIRTY);
		}
	}

	@Override
	public int maxPaletteHistorySize() {
		return 1;
	}

	@Override
	public boolean isPaletteResettable() {
		return false;
	}

	protected void reloadPalette() throws CoreException {
		var palFile = getPaletteFile();
		if (palFile == null) {
			var src = EditorFileProperties.paletteSource(getFile());
			if (src == PaletteSource.DEFAULT) {
				setDefaultPalette();
			} else {
				setDefaultTransPalette();
			}
		} else {
			loadPaletteFile(palFile);
		}
	}

	@Override
	public void setDefaultPalette() {
		setSpriteSheet(spriteSheet.withPalette(Palette.rgb333()));
	}

	@Override
	public void setDefaultTransPalette() {
		if (spriteSheet.bpp() == 4)
			setSpriteSheet(
					spriteSheet.withPalette(Palette.rgb333().withTransparency(Palette.DEFAULT_TRANSPARENCY % 16)));
		else
			setSpriteSheet(spriteSheet.withPalette(Palette.rgb333().withTransparency()));
	}

	private void applyCellSelection(int idx) {
		currentCellIndex = idx;
		if (spriteView != null) {
			spriteView.updateSelection(idx);
		}
		selectCell(spriteSheet.cell(idx));
		EditorFileProperties.setProperty(getFile(), EditorFileProperties.SPRITE_INDEX_PROPERTY, idx);
	}

	private void selectCell(SpriteCell spriteCell) {
		this.spriteCell = spriteCell;
		spriteGrid.setSpriteCell(spriteCell);
		if (spriteView != null) {
			spriteView.updateSpritePreview(spriteCell, spriteSheet.index(spriteCell));
		}
	}

	private void setupEditorListeners() {

		currentCellIndex = EditorFileProperties.getIntProperty(getFile(), EditorFileProperties.SPRITE_INDEX_PROPERTY, 0);
		if (currentCellIndex > 0 && currentCellIndex < spriteSheet.size()) {
			spriteCell = spriteSheet.cell(currentCellIndex);
			spriteGrid.setSpriteCell(spriteCell);
		}

		spriteGrid.addModifyListener(t -> {
			markDirty();
			if (spriteView != null) {
				spriteView.redrawSwatch();
				spriteView.updateSpritePreview(spriteCell, currentCellIndex);
			}
		});
		
		spriteGrid.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
			commandService.refreshElements("org.eclipse.ui.edit.copy", null);
			commandService.refreshElements("org.eclipse.ui.edit.cut", null);
			commandService.refreshElements("org.eclipse.ui.edit.paste", null);
		}));

		spriteGrid.addDrawListener(new DrawListener() {
			@Override
			public void drawStarted() {
				pendingDrawOp = new DrawOperation(spriteGrid, snapshotSpriteData());
			}

			@Override
			public void drawFinished() {
				if (pendingDrawOp != null) {
					pendingDrawOp.captureAfter();
					var op = pendingDrawOp;
					pendingDrawOp = null;
					spriteGrid.getDisplay().asyncExec(() -> execute(op));
				}
			}
		});

		spriteGrid.mode(SpritePaintMode.valueOf(EditorFileProperties
				.getProperty(getFile(), EditorFileProperties.EDITOR_MODE_PROPERTY, SpritePaintMode.SELECT.name()).toUpperCase()));

	}

	private String selectionString(int[][] pixels) {
		var sb = new StringBuilder();
		for (var row : pixels) {
			for (var pixel : row) {
				sb.append(pixel).append(" ");
			}
			sb.append("\n");
		}

		var text = sb.toString();
		return text;
	}

	private int[][] snapshotSpriteData() {
		var cell = spriteGrid.spriteCell();
		var size = cell.size();
		var snapshot = new int[size][size];
		for (var y = 0; y < size; y++) {
			for (var x = 0; x < size; x++) {
				snapshot[y][x] = cell.index(x, y);
			}
		}
		return snapshot;
	}

	private ColourPickerView openColourPickerView(IWorkbench bench) {
		
		var window = bench.getActiveWorkbenchWindow();
		if (window == null) {
			throw new IllegalStateException("No active workbench.");
		} else {
			var page = window.getActivePage();
			if (page == null) {
				throw new IllegalStateException("No active page.");
			} else {
				try {
					var view = page.showView(ColourPickerView.ID, null, org.eclipse.ui.IWorkbenchPage.VIEW_VISIBLE);
					return (ColourPickerView) view;
				} catch (PartInitException e) {
					throw new IllegalStateException("Failed to open colour picker view.", e);
				}
			}
		}
	}

	private SpriteView openSpriteView(IWorkbench bench) {
		var window = bench.getActiveWorkbenchWindow();
		if (window == null) {
			throw new IllegalStateException("No active workbench.");
		} else {
			var page = window.getActivePage();
			if (page == null) {
				throw new IllegalStateException("No active page.");
			} else {
				try {
					var view = page.showView(SpriteView.ID, null, org.eclipse.ui.IWorkbenchPage.VIEW_VISIBLE);
					return (SpriteView) view;
				} catch (PartInitException e) {
					throw new IllegalStateException("Failed to open sprite view.", e);
				}
			}
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

}
