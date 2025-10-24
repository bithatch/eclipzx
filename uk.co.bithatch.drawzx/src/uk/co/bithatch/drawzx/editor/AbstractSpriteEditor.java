package uk.co.bithatch.drawzx.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;

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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.EditorPart;

import uk.co.bithatch.drawzx.editor.EditorFileProperties.PaletteSource;
import uk.co.bithatch.drawzx.editor.EditorFileProperties.SpritePaintMode;
import uk.co.bithatch.drawzx.sprites.SpriteCell;
import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.drawzx.views.ColourPickerView;
import uk.co.bithatch.drawzx.views.IColourPicker;
import uk.co.bithatch.drawzx.widgets.SpriteEditorGrid;
import uk.co.bithatch.drawzx.widgets.SpriteGrid;
import uk.co.bithatch.drawzx.widgets.SpriteSwatch;
import uk.co.bithatch.zyxy.graphics.Palette;

public abstract class AbstractSpriteEditor extends EditorPart implements IPartListener, IColouredEditor {

	protected SpriteCell spriteCell;
	protected SpriteEditorGrid spriteGrid;
	protected SpriteGrid spritePreviewInverse;
	protected SpriteGrid spritePreviewNormal;
	protected SpriteSheet spriteSheet;
	protected SpriteSwatch spriteSwatch;

	private Clipboard clipboard;
	private final IResourceDeltaVisitor deltaVisitor = new IResourceDeltaVisitor() {
		@Override
		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();
			if (resource instanceof IFile file) {
				if ("pal".equalsIgnoreCase(file.getFileExtension())
						|| "npl".equalsIgnoreCase(file.getFileExtension())) {
					// Check if this file is the one currently in use
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
			return true; // keep visiting children
		}
	};

	private boolean dirty;
	private Label index;
	private PersistentPropertyWatcher propertyWatcher;
	private Composite root;
	private Label spriteSheetBpp;
	private Label spriteSheetCells;
	private Label spriteSheetSize;

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

	public void clear() {
		spriteGrid.clear();
		markDirty();
	}

	public void copySelectionToClipboard() {
		var text = selectionString(spriteGrid.copyPixels());
		var textTransfer = TextTransfer.getInstance();
		clipboard.setContents(new Object[] { text }, new Transfer[] { textTransfer });
		spriteGrid.deselect();
	}

	@Override
	public final void createPartControl(Composite parent) {
		root = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		root.setLayout(layout);

		createSpriteEditorAndSwatch(root);
		createInfo(root);

		updateSpriteSheetInfo();

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
				IResourceChangeEvent.POST_CHANGE // only real changes, after they happen
		);

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(this);


		try {
			reloadPalette();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void cutSelectionToClipboard() {
		var text = selectionString(spriteGrid.cutPixels());
		var textTransfer = TextTransfer.getInstance();
		clipboard.setContents(new Object[] { text }, new Transfer[] { textTransfer });
		spriteGrid.deselect();
		markDirty();
	}

	@Override
	public void dispose() {
		super.dispose();
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		clipboard.dispose();
		propertyWatcher.dispose();
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
			spriteSheet = loadSheet(file.getContents());
		} catch (IOException ioe) {
			throw new PartInitException(Status.error("Failed to load spritesheet.", ioe));
		} catch (CoreException e) {
			throw new PartInitException(e.getStatus());
		}
		spriteCell = spriteSheet.cell(0);
	}

	public void invertSprite() {
		spriteGrid.invert();
		markDirty();
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
		spriteGrid.mirrorH();
		markDirty();
	}

	public void mirrorSpriteV() {
		spriteGrid.mirrorV();
		markDirty();
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		if (part.equals(this)) {
			var cmdService = PlatformUI.getWorkbench().getService(ICommandService.class);
			if (cmdService != null) {
				cmdService.refreshElements("uk.co.bithatch.drawzx.sprites.commands.mode", null);
			}
			openColourPickerView(PlatformUI.getWorkbench());
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
				spriteGrid.paste(pixels);
				markDirty();
			} catch (NumberFormatException nfe) {
				// TODO some indication
			}
		}
	}

	public void rotate(int degrees) {
		spriteGrid.rotate(degrees);
		markDirty();

	}

	@Override
	public final void setFocus() {
		spriteGrid.setFocus();
	}

	public void setSpriteSheet(SpriteSheet spriteSheet) {
		this.spriteSheet = spriteSheet;
		if (spriteSheetCells != null) {
			spriteSwatch.spriteSheet(spriteSheet);
			updateSpriteSheetInfo();
			selectCell(spriteSheet.cell(spriteSwatch.selected()));
			if(picker != null)
				picker.palette(spriteSheet.palette());
		}

	}

	@Override
	public void picker(IColourPicker picker) {
		this.picker = picker;		
	}

	public void shift(int h, int v) {
		spriteGrid.shift(h, v);
		markDirty();

	}

	@Override
	public void colorSelected(int index, boolean primary) {
		if( ( primary ? spriteGrid.color() : spriteGrid.secondaryColor() ) != index) {
			if(primary) {
				spriteGrid.color(index);
				EditorFileProperties.setProperty(getFile(), EditorFileProperties.PALETTE_PRIMARY_PROPERTY, index);
			}
			else {
				spriteGrid.secondaryColor(index);
				EditorFileProperties.setProperty(getFile(), EditorFileProperties.PALETTE_SECONDARY_PROPERTY, index);
			}
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
						return true; // allow folders
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
		updateSpriteSheetInfo();
	}

	protected GridLayout createGroupLayout(int cols, boolean equal) {
		var l = new GridLayout(cols, equal);
		l.verticalSpacing = 8;
		l.marginWidth = 8;
		l.marginHeight = 8;
		return l;
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

	protected void layoutGridAndSwatch(Composite parent) {
		var palettes = new Composite(parent, SWT.NONE);
		var layout = new GridLayout();
		layout.verticalSpacing = 8;
		palettes.setLayout(layout);
		palettes.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		spriteGrid = new SpriteEditorGrid(palettes, spriteCell, SWT.BORDER);
		spriteGrid.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		spriteSwatch = new SpriteSwatch(palettes, spriteSheet, 24, 16, SWT.BORDER);
		spriteSwatch.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 240).create());
	}

	protected void loadPaletteFile(IFile file) throws CoreException {
		try (var in = Channels.newChannel(file.getContents())) {
			setSpriteSheet(spriteSheet.withPalette(Palette.load(in, 256)));
		} catch (IOException ioe) {
			throw new CoreException(Status.error("Failed to load palette.", ioe));
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

	protected void spriteCellSelected(SelectionEvent e) {
		selectCell((SpriteCell) e.data);
		EditorFileProperties.setProperty(getFile(), EditorFileProperties.SPRITE_INDEX_PROPERTY, spriteSwatch.selected());
	}

	private void createInfo(Composite parent) {
		var groups = new Composite(parent, SWT.NONE);
		groups.setLayout(new GridLayout(1, false));
		groups.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).create());

		var spritesheetInfo = new Group(groups, SWT.TITLE);
		spritesheetInfo.setLayout(createGroupLayout(2, false));
		spritesheetInfo.setText("Sprite sheet");
		spritesheetInfo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		var cellsLabel = new Label(spritesheetInfo, SWT.NONE);
		cellsLabel.setText("Cells:");
		spriteSheetCells = new Label(spritesheetInfo, SWT.BOLD);

		var sizeLabel = new Label(spritesheetInfo, SWT.NONE);
		sizeLabel.setText("Size:");
		spriteSheetSize = new Label(spritesheetInfo, SWT.BOLD);

		var depthLabel = new Label(spritesheetInfo, SWT.NONE);
		depthLabel.setText("Depth:");
		spriteSheetBpp = new Label(spritesheetInfo, SWT.BOLD);

		var spriteInfo = new Group(groups, SWT.TITLE);
		spriteInfo.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		spriteInfo.setLayout(createGroupLayout(2, false));
		spriteInfo.setText("Sprite");

		var indexLabel = new Label(spriteInfo, SWT.NONE);
		indexLabel.setText("Index:");
		index = new Label(spriteInfo, SWT.BOLD);
		index.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		index.setText("");

		var previews = new Composite(spriteInfo, SWT.NONE);
		previews.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
		previews.setLayout(new GridLayout(2, true));

		spritePreviewNormal = new SpriteGrid(previews, 48, SWT.NONE);
		spritePreviewNormal.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		spritePreviewInverse = new SpriteGrid(previews, 48, SWT.NONE);
		spritePreviewInverse.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		spritePreviewInverse.setInverse(true);

	}

	private void createSpriteEditorAndSwatch(Composite parent) {

		layoutGridAndSwatch(parent);

		spriteSwatch.selected(EditorFileProperties.getIntProperty(getFile(), EditorFileProperties.SPRITE_INDEX_PROPERTY, 0));

		spriteGrid.addModifyListener(t -> {
			markDirty();
			spritePreviewNormal.redraw();
			spritePreviewInverse.redraw();
			spriteSwatch.redraw();
		});
		
		spriteGrid.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
			commandService.refreshElements("org.eclipse.ui.edit.copy", null);
			commandService.refreshElements("org.eclipse.ui.edit.cut", null);
			commandService.refreshElements("org.eclipse.ui.edit.paste", null);
		}));

		spriteGrid.mode(SpritePaintMode.valueOf(EditorFileProperties
				.getProperty(getFile(), EditorFileProperties.EDITOR_MODE_PROPERTY, SpritePaintMode.SELECT.name()).toUpperCase()));

		spriteSwatch.addSelectionListener(SelectionListener.widgetSelectedAdapter(this::spriteCellSelected));

	}

	private void selectCell(SpriteCell spriteCell) {
		this.spriteCell = spriteCell;
		spriteGrid.setSpriteCell(spriteCell);
		index.setText(String.valueOf(spriteSheet.index(spriteCell)));
		updatePreview();
//		if(e.detail == 2) {
//			choose(fullPal.color((Integer)e.data));
//		}
	}

	private String selectionString(int[][] pixels) {
		var sb = new StringBuilder();
		for (var row : pixels) {
			for (var pixel : row) {
				sb.append(pixel).append(" "); // or format differently
			}
			sb.append("\n");
		}

		var text = sb.toString();
		return text;
	}

	private void updatePreview() {
		spritePreviewNormal.setSpriteCell(spriteCell);
		spritePreviewInverse.setSpriteCell(spriteCell);
	}

	private void updateSpriteSheetInfo() {
		spriteSheetCells.setText(String.valueOf(spriteSheet.size()));
		spriteSheetSize.setText(String.format("%d bytes", spriteSheet.byteSize()));
		spriteSheetBpp.setText(String.format("%d Bpp", spriteSheet.bpp()));
		if(picker != null) {
			picker.updatePaletteInfo();
		}
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
					var view = page.showView(ColourPickerView.ID);
					page.bringToTop(view);
					return (ColourPickerView) view;
				} catch (PartInitException e) {
					throw new IllegalStateException("Failed to open colour picker view.", e);
				}
			}
		}
	}

}
