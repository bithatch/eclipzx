package uk.co.bithatch.drawzx.views;

import java.util.Objects;
import java.util.stream.IntStream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import uk.co.bithatch.drawzx.editor.ISpriteSwatchEditor;
import uk.co.bithatch.drawzx.sprites.SpriteCell;
import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.drawzx.widgets.SpriteGrid;
import uk.co.bithatch.drawzx.widgets.SpriteSwatch;
import uk.co.bithatch.widgetzx.FontStyleHelper;

/**
 * A view that displays a {@link SpriteSwatch} and spritesheet/sprite info,
 * linked to the currently active sprite editor. Follows the same pattern as
 * {@link ColourPickerView}.
 */
public class SpriteView extends ViewPart implements IPartListener2, ISpriteView {
	public static final String ID = "uk.co.bithatch.drawzx.views.spriteView";

	private static final int SWATCH_SPACING = 2;

	private Composite parent;
	private Composite contentArea;
	private Composite swatchArea;
	private GridData swatchAreaGridData;
	private Label unsupportedLabel;
	private GridData unsupportedGridData;

	private ISpriteSwatchEditor editor;
	private IWorkbenchPart activePart;
	private boolean horizontal;
	private boolean linkedWithEditor = true;

	// Swatch
	private SpriteSwatch spriteSwatch;
	private Composite swatchContainer;

	// Spritesheet info
	private Composite spritesheetInfoPanel;
	private GridData spritesheetInfoGridData;
	private Label spriteSheetCells;
	private Label spriteSheetCellSizeLabel;
	private Combo spriteSheetCellSize;
	private Label spriteSheetSize;
	private Label spriteSheetBpp;

	// Sprite info
	private Composite spriteInfoPanel;
	private GridData spriteInfoGridData;
	private Label indexLabel;
	private SpriteGrid spritePreviewNormal;
	private SpriteGrid spritePreviewInverse;
	private Composite previews;

	// Layout groups
	private Composite viewGroups;
	private GridData swatchContainerGridData;

	// Editor-configured base values
	private int baseColumns;
	private int baseCellSize;
	private FontStyleHelper fontStyleHelper;

	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;

		contentArea = new Composite(parent, SWT.NONE);
		var contentLayout = new GridLayout(1, true);
		contentLayout.marginWidth = 0;
		contentLayout.marginHeight = 0;
		contentArea.setLayout(contentLayout);

		swatchArea = new Composite(contentArea, SWT.NONE);
		var swatchAreaLayout = new GridLayout(1, true);
		swatchAreaLayout.marginWidth = 0;
		swatchAreaLayout.marginHeight = 0;
		swatchArea.setLayout(swatchAreaLayout);
		swatchAreaGridData = GridDataFactory.fillDefaults().grab(true, true).create();
		swatchArea.setLayoutData(swatchAreaGridData);

		createSwatchContent(swatchArea);

		unsupportedLabel = new Label(contentArea, SWT.WRAP | SWT.CENTER);
		unsupportedGridData = GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).grab(true, true).create();
		unsupportedGridData.exclude = true;
		unsupportedLabel.setLayoutData(unsupportedGridData);
		unsupportedLabel.setText("Open a sprite, UDG, tile definition, or tilemap editor");
		unsupportedLabel.setVisible(false);

		contributeToActionBars();

		getSite().getWorkbenchWindow().getPartService().addPartListener(this);

		parent.addListener(SWT.Resize, e -> {
			updateOrientation();
			fitSwatchToAvailableSpace();
		});

		IEditorPart activeEditor = getSite().getPage().getActiveEditor();
		updateView(activeEditor);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager toolbar = bars.getToolBarManager();
		toolbar.add(new LinkWithEditorAction());
	}

	private class LinkWithEditorAction extends Action {
		public LinkWithEditorAction() {
			super("Link with Editor", IAction.AS_CHECK_BOX);
			setChecked(true);
			setToolTipText("Link with Editor");
			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
					.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
		}

		@Override
		public void run() {
			linkedWithEditor = isChecked();
			if (linkedWithEditor) {
				// Re-sync with current editor
				IEditorPart activeEditor = getSite().getPage().getActiveEditor();
				updateView(activeEditor);
			}
		}
	}

	private void updateOrientation() {
		if (viewGroups == null || viewGroups.isDisposed())
			return;
		var size = parent.getSize();
		var nowHorizontal = size.x > size.y;
		if (nowHorizontal != horizontal) {
			horizontal = nowHorizontal;
			var layout = (GridLayout) viewGroups.getLayout();
			layout.numColumns = horizontal ? 3 : 1;
			layout.makeColumnsEqualWidth = false;

			// Info panels: don't grab excess space, stay at natural size
			spritesheetInfoGridData.grabExcessVerticalSpace = false;
			spritesheetInfoGridData.verticalAlignment = SWT.BEGINNING;
			spritesheetInfoGridData.grabExcessHorizontalSpace = false;
			spritesheetInfoGridData.horizontalAlignment = SWT.FILL;

			// Swatch container: grab ALL leftover space
			swatchContainerGridData.grabExcessHorizontalSpace = true;
			swatchContainerGridData.grabExcessVerticalSpace = true;
			swatchContainerGridData.horizontalAlignment = SWT.FILL;
			swatchContainerGridData.verticalAlignment = SWT.FILL;

			spriteInfoGridData.grabExcessVerticalSpace = false;
			spriteInfoGridData.verticalAlignment = SWT.BEGINNING;
			spriteInfoGridData.grabExcessHorizontalSpace = false;
			spriteInfoGridData.horizontalAlignment = SWT.FILL;

			// Switch preview grids between side-by-side (vertical view) and stacked (horizontal view)
			var previewLayout = (GridLayout) previews.getLayout();
			previewLayout.numColumns = horizontal ? 1 : 2;
			previews.layout(true, true);

			// Update swatch alignment for centering
			updateSwatchAlignment();

			// Layout first so container sizes settle, then defer fit
			viewGroups.layout(true, true);
			parent.layout(true, true);
			parent.getDisplay().asyncExec(this::fitSwatchToAvailableSpace);
		}
	}

	private void updateSwatchAlignment() {
		if (spriteSwatch == null || spriteSwatch.isDisposed())
			return;
		var ld = spriteSwatch.getLayoutData();
		if (ld instanceof GridData gd) {
			gd.horizontalAlignment = SWT.CENTER;
			gd.verticalAlignment = SWT.CENTER;
			gd.grabExcessHorizontalSpace = true;
			gd.grabExcessVerticalSpace = true;
		}
	}

	private void fitSwatchToAvailableSpace() {
		if (spriteSwatch == null || spriteSwatch.isDisposed() || editor == null)
			return;

		var containerSize = swatchContainer.getSize();
		if (containerSize.x <= 0 || containerSize.y <= 0)
			return;

		var sheet = editor.spriteSheet();
		if (sheet == null)
			return;

		var totalCells = sheet.size();
		var baseRows = (totalCells + baseColumns - 1) / baseColumns;

		// When horizontal, transpose: use baseRows as columns and baseColumns as rows
		var cols = horizontal ? baseRows : baseColumns;
		var rows = (totalCells + cols - 1) / cols;

		// Calculate the max cell size that fits
		var availW = containerSize.x;
		var availH = containerSize.y;

		var maxCellW = (availW - (cols - 1) * SWATCH_SPACING - 2) / cols;
		var maxCellH = (availH - (rows - 1) * SWATCH_SPACING - 2) / rows;
		var fitCellSize = Math.min(maxCellW, maxCellH);

		// Don't go larger than the base, but shrink if needed. Min 4px.
		var newCellSize = Math.max(4, Math.min(baseCellSize, fitCellSize));

		if (newCellSize != spriteSwatch.cellSize() || cols != spriteSwatch.columns()) {
			spriteSwatch.update(sheet, cols, newCellSize);
			updateSwatchAlignment();
			swatchContainer.layout(true, true);
		}
	}

	@Override
	public void dispose() {
		if (this.editor != null) {
			this.editor.spriteView(null);
		}
		getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		fontStyleHelper.dispose();
		super.dispose();
	}

	@Override
	public void setFocus() {
		swatchArea.setFocus();
	}

	// --- ISpriteView implementation ---

	@Override
	public void updateSpriteSheet(SpriteSheet spriteSheet, int columns, int cellSize) {
		baseColumns = columns;
		baseCellSize = cellSize;
		if (spriteSwatch != null && !spriteSwatch.isDisposed()) {
			spriteSwatch.update(spriteSheet, columns, cellSize);
			// Defer fit to after layout
			spriteSwatch.getDisplay().asyncExec(this::fitSwatchToAvailableSpace);
		}
		updateSpriteSheetInfo();
	}

	@Override
	public void updateSelection(int index) {
		if (spriteSwatch != null && !spriteSwatch.isDisposed()) {
			spriteSwatch.selected(index);
		}
	}

	@Override
	public void updateSpriteSheetInfo() {
		if (editor == null)
			return;
		var sheet = editor.spriteSheet();
		if (sheet == null)
			return;

		spriteSheetCells.setText(String.valueOf(sheet.size()));
		spriteSheetSize.setText(String.format("%d bytes", sheet.byteSize()));
		spriteSheetBpp.setText(String.format("%d Bpp", sheet.bpp()));

		var cellSizes = editor.cellSizes();
		if (cellSizes.length > 0) {
			var cellSizeList = IntStream.of(cellSizes).mapToObj(String::valueOf).toList();
			spriteSheetCellSize.setItems(cellSizeList.toArray(String[]::new));
			var idx = cellSizeList.indexOf(String.valueOf(sheet.cellSize()));
			spriteSheetCellSize.select(idx == -1 ? 0 : idx);
			updateVisibility(spriteSheetCellSizeLabel, true);
			updateVisibility(spriteSheetCellSize, true);
		} else {
			updateVisibility(spriteSheetCellSizeLabel, false);
			updateVisibility(spriteSheetCellSize, false);
		}

		spritesheetInfoPanel.layout(true);
	}

	@Override
	public void redrawSwatch() {
		if (spriteSwatch != null && !spriteSwatch.isDisposed()) {
			spriteSwatch.redrawSelected();
		}
		if (spritePreviewNormal != null && !spritePreviewNormal.isDisposed()) {
			spritePreviewNormal.redraw();
		}
		if (spritePreviewInverse != null && !spritePreviewInverse.isDisposed()) {
			spritePreviewInverse.redraw();
		}
	}

	@Override
	public void updateSpritePreview(SpriteCell cell, int index) {
		if (indexLabel != null && !indexLabel.isDisposed()) {
			indexLabel.setText(String.valueOf(index));
		}
		if (spritePreviewNormal != null && !spritePreviewNormal.isDisposed()) {
			spritePreviewNormal.setSpriteCell(cell);
		}
		if (spritePreviewInverse != null && !spritePreviewInverse.isDisposed()) {
			spritePreviewInverse.setSpriteCell(cell);
		}
	}

	// --- IPartListener2 implementation ---

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		if (!linkedWithEditor)
			return;
		if (partRef instanceof IEditorReference) {
			IWorkbenchPart part = partRef.getPart(false);
			if (!Objects.equals(activePart, part)) {
				updateView(part);
			}
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		if (partRef instanceof IEditorReference) {
			IWorkbenchPart part = partRef.getPart(false);
			if (part != null && part.equals(editor)) {
				updateView(null);
			}
		}
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
	}

	// --- Private helpers ---

	private void createSwatchContent(Composite parent) {
		viewGroups = new Composite(parent, SWT.NONE);
		var vgLayout = new GridLayout(1, false);
		vgLayout.marginWidth = 4;
		vgLayout.marginHeight = 4;
		vgLayout.verticalSpacing = 4;
		vgLayout.horizontalSpacing = 4;
		viewGroups.setLayout(vgLayout);
		viewGroups.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		// Spritesheet info — plain composite, no border
		spritesheetInfoPanel = new Composite(viewGroups, SWT.NONE);
		var spritesheetInfoLayout = new GridLayout(2, false);
		spritesheetInfoLayout.horizontalSpacing = 8;
		spritesheetInfoLayout.verticalSpacing = 4;
		spritesheetInfoLayout.marginWidth = 4;
		spritesheetInfoLayout.marginHeight = 4;
		spritesheetInfoPanel.setLayout(spritesheetInfoLayout);
		spritesheetInfoGridData = GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.BEGINNING).create();
		spritesheetInfoPanel.setLayoutData(spritesheetInfoGridData);

		fontStyleHelper = new FontStyleHelper(spritesheetInfoPanel);

		var cellsLabel = fontStyleHelper.bold(new Label(spritesheetInfoPanel, SWT.NONE));
		cellsLabel.setText("Cells:");
		spriteSheetCells = new Label(spritesheetInfoPanel, SWT.NONE);

		spriteSheetCellSizeLabel = fontStyleHelper.bold(new Label(spritesheetInfoPanel, SWT.NONE));
		spriteSheetCellSizeLabel.setText("Cell Size:");
		spriteSheetCellSize = new Combo(spritesheetInfoPanel, SWT.READ_ONLY);
		spriteSheetCellSize.setLayoutData(GridDataFactory.fillDefaults().hint(56, SWT.DEFAULT).grab(true, false).create());
		spriteSheetCellSize.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (editor != null) {
				var cellSizes = editor.cellSizes();
				var selIdx = spriteSheetCellSize.getSelectionIndex();
				if (selIdx >= 0 && selIdx < cellSizes.length) {
					editor.cellSizeChanged(cellSizes[selIdx]);
				}
			}
		}));

		var sizeLabel = fontStyleHelper.bold(new Label(spritesheetInfoPanel, SWT.NONE));
		sizeLabel.setText("Size:");
		spriteSheetSize = new Label(spritesheetInfoPanel, SWT.NONE);

		var depthLabel = fontStyleHelper.bold(new Label(spritesheetInfoPanel, SWT.NONE));
		depthLabel.setText("Depth:");
		spriteSheetBpp = new Label(spritesheetInfoPanel, SWT.NONE);

		// Swatch container — grabs ALL leftover space
		swatchContainer = new Composite(viewGroups, SWT.NONE);
		var swatchLayout = new GridLayout(1, true);
		swatchLayout.marginWidth = 0;
		swatchLayout.marginHeight = 0;
		swatchContainer.setLayout(swatchLayout);
		swatchContainerGridData = GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).create();
		swatchContainer.setLayoutData(swatchContainerGridData);

		// Sprite info — plain composite, no border
		spriteInfoPanel = new Composite(viewGroups, SWT.NONE);
		var spriteInfoLayout = new GridLayout(2, false);
		spriteInfoLayout.marginWidth = 4;
		spriteInfoLayout.marginHeight = 4;
		spriteInfoLayout.verticalSpacing = 4;
		spriteInfoLayout.horizontalSpacing = 8;
		spriteInfoPanel.setLayout(spriteInfoLayout);
		spriteInfoGridData = GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.BEGINNING).create();
		spriteInfoPanel.setLayoutData(spriteInfoGridData);

		var idxLbl = fontStyleHelper.bold(new Label(spriteInfoPanel, SWT.NONE));
		idxLbl.setText("Index:");
		indexLabel = new Label(spriteInfoPanel, SWT.NONE);
		indexLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		indexLabel.setText("");

		previews = new Composite(spriteInfoPanel, SWT.NONE);
		var previewsGridData = GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create();
		previews.setLayoutData(previewsGridData);
		previews.setLayout(new GridLayout(2, true));

		spritePreviewNormal = new SpriteGrid(previews, 48, SWT.NONE);
		spritePreviewNormal.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		spritePreviewInverse = new SpriteGrid(previews, 48, SWT.NONE);
		spritePreviewInverse.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		spritePreviewInverse.setInverse(true);
	}

	private void updateView(IWorkbenchPart part) {
		this.activePart = part;
		if (part instanceof ISpriteSwatchEditor newEditor) {
			// Unlink old editor
			if (this.editor != null && this.editor != newEditor) {
				this.editor.spriteView(null);
			}

			this.editor = newEditor;
			baseColumns = editor.swatchColumns();
			baseCellSize = editor.swatchCellSize();

			swatchArea.setVisible(true);
			unsupportedGridData.exclude = true;
			unsupportedLabel.setVisible(false);
			swatchAreaGridData.exclude = false;

			// Rebuild swatch
			rebuildSwatch();

			// Show/hide previews based on editor type
			var show = editor.showPreviews();
			updateVisibility(spriteInfoPanel, show);

			// Update info
			updateSpriteSheetInfo();

			// Select current sprite and update preview
			var idx = editor.selectedSpriteIndex();
			if (spriteSwatch != null && idx >= 0) {
				spriteSwatch.selected(idx);
			}
			if (show && idx >= 0 && idx < editor.spriteSheet().size()) {
				updateSpritePreview(editor.spriteSheet().cell(idx), idx);
			}

			// Link editor to this view
			this.editor.spriteView(this);

			contentArea.pack();

			// Fit after layout settles
			parent.getDisplay().asyncExec(this::fitSwatchToAvailableSpace);
		} else {
			// Unlink old editor
			if (this.editor != null) {
				this.editor.spriteView(null);
			}

			swatchArea.setVisible(false);
			unsupportedLabel.setVisible(true);
			unsupportedGridData.exclude = false;
			swatchAreaGridData.exclude = true;
			contentArea.pack();
			this.editor = null;
		}
		parent.layout();
	}

	private void rebuildSwatch() {
		// Dispose old swatch
		if (spriteSwatch != null && !spriteSwatch.isDisposed()) {
			spriteSwatch.dispose();
		}

		var sheet = editor.spriteSheet();
		var cols = baseColumns;
		var cellSz = baseCellSize;

		spriteSwatch = new SpriteSwatch(swatchContainer, sheet, cellSz, cols, SWT.BORDER);
		spriteSwatch.spacing(SWATCH_SPACING);
		spriteSwatch.backgroundType(editor.backgroundType());

		// Centered alignment
		var gd = GridDataFactory.fillDefaults().grab(true, true)
				.align(SWT.CENTER, SWT.CENTER)
				.create();
		spriteSwatch.setLayoutData(gd);

		spriteSwatch.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (editor != null) {
				var spriteCell = (uk.co.bithatch.drawzx.sprites.SpriteCell) e.data;
				var selIdx = editor.spriteSheet().index(spriteCell);
				editor.selectSprite(selIdx);
			}
		}));

		swatchContainer.layout(true, true);
	}

	private void updateVisibility(Control ctrl, boolean visible) {
		ctrl.setVisible(visible);
		var ld = ctrl.getLayoutData();
		if (ld instanceof GridData gd) {
			gd.exclude = !visible;
		}
	}
}
