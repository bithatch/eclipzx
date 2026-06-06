package uk.co.bithatch.drawzx.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import uk.co.bithatch.drawzx.Activator;
import uk.co.bithatch.drawzx.sprites.SpriteCell;
import uk.co.bithatch.drawzx.tilemaps.Tilemap;
import uk.co.bithatch.drawzx.tilemaps.TilemapEntry;

/**
 * A canvas widget that displays and allows editing of a ZX Next tilemap. Each
 * cell in the grid represents a tile position and renders the tile from the
 * associated tile definition set.
 * 
 * <p>The user can click on a cell to place the currently selected tile, or select
 * cells for other operations.</p>
 */
public class TilemapEditorGrid extends Canvas {

	public enum TilemapPaintMode {
		SELECT, PLACE, FILL
	}

	private Tilemap tilemap;
	private int cellPixelSize = 16; // pixel size of each tile cell on screen
	private int selectedTileIndex = 0;
	private TilemapPaintMode mode = TilemapPaintMode.SELECT;
	private boolean drawing;
	private Point selectStart, selectEnd;
	private Rectangle selection;
	private Color gridColor;
	private Color selectedColor;
	private final List<DrawListener> drawListeners = new ArrayList<>();
	private int scrollX = 0;
	private int scrollY = 0;
	private Image cachedImage;
	private boolean cacheDirty = true;
	private int cachedScrollX = -1;
	private int cachedScrollY = -1;
	private final Map<Long, Image> tileCellCache = new HashMap<>();
	private Point lastPlacedCell;
	private boolean lineDrawing;
	private boolean shiftDown;
	private Point lineStart;
	private Point lineEnd;
	
	// Paste preview state
	private TilemapEntry[][] pastePreview;
	private int pastePreviewCols;
	private int pastePreviewRows;
	private Point pastePreviewPos; // top-left cell for paste

	public TilemapEditorGrid(Composite parent, Tilemap tilemap, int style) {
		this(parent, tilemap, 16, style);
	}

	public TilemapEditorGrid(Composite parent, Tilemap tilemap, int cellPixelSize, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED);
		this.tilemap = tilemap;
		this.cellPixelSize = cellPixelSize;
		gridColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER);
		selectedColor = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);

		addPaintListener(e -> paintCached(e.gc));
		addListener(SWT.Resize, e -> invalidateCache());
		setupKeyListeners();
		setupMouseListeners();
	}

	// ...existing code...

	private void setupKeyListeners() {
		addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ESC && pastePreview != null) {
					cancelPaste();
				} else if (e.keyCode == SWT.SHIFT && mode == TilemapPaintMode.PLACE) {
					shiftDown = true;
					if (!lineDrawing) {
						lineDrawing = true;
						// If we already have a last placed cell from a normal click, use it as line start
						if (lineStart == null && lastPlacedCell != null) {
							lineStart = lastPlacedCell;
							lineEnd = lastPlacedCell;
						}
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.SHIFT) {
					// Abort line drawing - don't commit (commit happens on mouseUp)
					lineDrawing = false;
					shiftDown = false;
					lineStart = null;
					lineEnd = null;
					redraw();
				}
			}
		});
	}

	/**
	 * Mark the cached tilemap image as dirty so it will be rebuilt on next paint.
	 */
	public void invalidateCache() {
		cacheDirty = true;
	}

	/**
	 * Invalidate both the tilemap cache and all cached tile images.
	 * Call this when tile definitions or palettes change.
	 */
	public void invalidateAllCaches() {
		cacheDirty = true;
		disposeTileCellCache();
	}

	@Override
	public void dispose() {
		disposeCachedImage();
		disposeTileCellCache();
		super.dispose();
	}

	private void setupMouseListeners() {
		addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}

			@Override
			public void mouseDown(MouseEvent e) {
				// If pasting, click commits the paste
				if (pastePreview != null) {
					updatePastePreviewPosition(e.x, e.y);
					if (commitPaste()) {
						fireDrawStarted();
						fireChanged();
						fireDrawFinished();
					}
					return;
				}

				var cell = cellAt(e.x, e.y);
				if (cell == null) return;

				if (mode == TilemapPaintMode.SELECT) {
					selectStart = cell;
					selectEnd = cell;
					selection = null;
					redraw();
				} else if (mode == TilemapPaintMode.PLACE) {
					if (lineDrawing) {
						// In line drawing mode, update the end point on click
						lineEnd = cell;
						redraw();
					} else {
						drawing = true;
						lastPlacedCell = cell;
						fireDrawStarted();
						placeTileAt(cell);
					}
				} else if (mode == TilemapPaintMode.FILL) {
					fireDrawStarted();
					floodFill(cell.x, cell.y, selectedTileIndex);
					fireDrawFinished();
				}
			}

			@Override
			public void mouseUp(MouseEvent e) {
				if (mode == TilemapPaintMode.SELECT && selectStart != null) {
					var cell = cellAt(e.x, e.y);
					if (cell != null) {
						selectEnd = cell;
						selection = normalizeRect(selectStart, selectEnd);
					}
					selectStart = null;
					selectEnd = null;
					redraw();
					fireSelectionChanged();
				} else if (lineDrawing && lineStart != null) {
					var cell = cellAt(e.x, e.y);
					if (cell != null) {
						lineEnd = cell;
					}
					// Commit the line
					commitLine(lineStart, lineEnd);
				}
				// After committing (or not), update line state
				if (shiftDown) {
					// Shift still held - chain: new line starts from current end point
					var cell = cellAt(e.x, e.y);
					if (cell != null) {
						lineStart = cell;
						lineEnd = cell;
						lastPlacedCell = cell;
					}
				} else {
					lineStart = null;
					lineEnd = null;
				}
				if (drawing) {
					drawing = false;
					fireDrawFinished();
				}
				redraw();
			}
		});

		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				if (pastePreview != null) {
					updatePastePreviewPosition(e.x, e.y);
				} else if (lineDrawing) {
					var cell = cellAt(e.x, e.y);
					if (cell != null && (lineEnd == null || lineEnd.x != cell.x || lineEnd.y != cell.y)) {
						lineEnd = cell;
						redraw();
					}
				} else if (mode == TilemapPaintMode.SELECT && selectStart != null) {
					var cell = cellAt(e.x, e.y);
					if (cell != null) {
						selectEnd = cell;
						redraw();
					}
				} else if (mode == TilemapPaintMode.PLACE && drawing) {
					var cell = cellAt(e.x, e.y);
					if (cell != null) {
						if (lastPlacedCell != null && (lastPlacedCell.x != cell.x || lastPlacedCell.y != cell.y)) {
							// Interpolate between last and current cell (Bresenham)
							interpolateCells(lastPlacedCell, cell);
						} else {
							placeTileAt(cell);
						}
						lastPlacedCell = cell;
					}
				}
			}
		});
	}

	private void placeTileAt(Point cell) {
		var entry = tilemap.entry(cell.x, cell.y);
		if (entry.tileIndex() != selectedTileIndex) {
			entry.tileIndex(selectedTileIndex);
			invalidateCache();
			redraw();
			fireChanged();
		}
	}

	private void floodFill(int col, int row, int newTileIndex) {
		if (col < 0 || col >= tilemap.columns() || row < 0 || row >= tilemap.rows()) return;
		var targetIndex = tilemap.entry(col, row).tileIndex();
		if (targetIndex == newTileIndex) return;
		floodFillRecurse(col, row, targetIndex, newTileIndex);
		invalidateCache();
		redraw();
		fireChanged();
	}

	private void floodFillRecurse(int col, int row, int targetIndex, int newTileIndex) {
		if (col < 0 || col >= tilemap.columns() || row < 0 || row >= tilemap.rows()) return;
		if (tilemap.entry(col, row).tileIndex() != targetIndex) return;
		tilemap.entry(col, row).tileIndex(newTileIndex);
		floodFillRecurse(col - 1, row, targetIndex, newTileIndex);
		floodFillRecurse(col + 1, row, targetIndex, newTileIndex);
		floodFillRecurse(col, row - 1, targetIndex, newTileIndex);
		floodFillRecurse(col, row + 1, targetIndex, newTileIndex);
	}

	private Point cellAt(int px, int py) {
		var ox = calcOffsetX();
		var oy = calcOffsetY();
		var col = (px - ox + scrollX) / cellPixelSize;
		var row = (py - oy + scrollY) / cellPixelSize;
		if (col >= 0 && col < tilemap.columns() && row >= 0 && row < tilemap.rows()) {
			return new Point(col, row);
		}
		return null;
	}

	/**
	 * Compute the horizontal offset to centre the tilemap within the widget
	 * when the widget is larger than the tilemap content.
	 */
	public int calcOffsetX() {
		var tilemapWidth = tilemap.columns() * cellPixelSize;
		var clientWidth = getClientArea().width;
		return Math.max(0, (clientWidth - tilemapWidth) / 2);
	}

	/**
	 * Compute the vertical offset to centre the tilemap within the widget
	 * when the widget is larger than the tilemap content.
	 */
	public int calcOffsetY() {
		var tilemapHeight = tilemap.rows() * cellPixelSize;
		var clientHeight = getClientArea().height;
		return Math.max(0, (clientHeight - tilemapHeight) / 2);
	}

	private Rectangle normalizeRect(Point a, Point b) {
		var x1 = Math.min(a.x, b.x);
		var y1 = Math.min(a.y, b.y);
		var x2 = Math.max(a.x, b.x);
		var y2 = Math.max(a.y, b.y);
		return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
	}

	private void disposeCachedImage() {
		if (cachedImage != null && !cachedImage.isDisposed()) {
			cachedImage.dispose();
			cachedImage = null;
		}
	}

	private void paintCached(GC gc) {
		var bounds = getClientArea();
		if (bounds.width <= 0 || bounds.height <= 0)
			return;

		// Rebuild cache if dirty, resized, or scroll position changed
		if (cacheDirty || cachedImage == null || cachedImage.isDisposed()
				|| cachedImage.getBounds().width != bounds.width
				|| cachedImage.getBounds().height != bounds.height
				|| cachedScrollX != scrollX || cachedScrollY != scrollY) {
			disposeCachedImage();
			cachedImage = new Image(getDisplay(), bounds.width, bounds.height);
			var imgGC = new GC(cachedImage);
			try {
				paintTilemapContent(imgGC, bounds);
			} finally {
				imgGC.dispose();
			}
			cachedScrollX = scrollX;
			cachedScrollY = scrollY;
			cacheDirty = false;
		}

		// Blit cached tilemap + grid
		gc.drawImage(cachedImage, 0, 0);

		// Draw selection overlay directly (changes frequently during drag)
		paintSelectionOverlay(gc);
	}

	private void paintTilemapContent(GC gc, Rectangle bounds) {
		var started = System.currentTimeMillis();
		var cols = tilemap.columns();
		var rows = tilemap.rows();
		var tileDefs = tilemap.tileDefinitions();
		var ox = calcOffsetX();
		var oy = calcOffsetY();

		// Background - fill with widget background, then tilemap area with black
		gc.setBackground(getBackground());
		gc.fillRectangle(bounds);
		var tilemapWidth = cols * cellPixelSize;
		var tilemapHeight = rows * cellPixelSize;
		gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		gc.fillRectangle(ox - scrollX, oy - scrollY, tilemapWidth, tilemapHeight);

		// Draw each tile
		for (var r = 0; r < rows; r++) {
			for (var c = 0; c < cols; c++) {
				var px = ox + (c * cellPixelSize) - scrollX;
				var py = oy + (r * cellPixelSize) - scrollY;

				// Skip if off-screen
				if (px + cellPixelSize < 0 || py + cellPixelSize < 0 || px > bounds.width || py > bounds.height)
					continue;

				var entry = tilemap.entry(c, r);
				var tileIdx = entry.tileIndex();

				if (tileIdx >= 0 && tileIdx < tileDefs.size()) {
					var cell = tileDefs.cell(tileIdx);
					drawTileCell(gc, cell, px, py, entry);
				}
			}
		}

		// Grid lines
		gc.setForeground(gridColor);
		gc.setAlpha(64);
		for (var r = 0; r <= rows; r++) {
			var y = oy + (r * cellPixelSize) - scrollY;
			gc.drawLine(ox - scrollX, y, ox + (cols * cellPixelSize) - scrollX, y);
		}
		for (var c = 0; c <= cols; c++) {
			var x = ox + (c * cellPixelSize) - scrollX;
			gc.drawLine(x, oy - scrollY, x, oy + (rows * cellPixelSize) - scrollY);
		}
		gc.setAlpha(255);
		
	}

	private void paintSelectionOverlay(GC gc) {
		var ox = calcOffsetX();
		var oy = calcOffsetY();
		// Paste preview
		if (pastePreview != null && pastePreviewPos != null) {
			paintPastePreview(gc);
		}
		// Line drawing preview
		else if (lineDrawing && lineStart != null && lineEnd != null) {
			paintLinePreview(gc);
		}
		// Selection highlight
		else if (selectStart != null && selectEnd != null) {
			var rect = normalizeRect(selectStart, selectEnd);
			gc.setForeground(selectedColor);
			gc.setLineWidth(2);
			gc.drawRectangle(
				ox + (rect.x * cellPixelSize) - scrollX,
				oy + (rect.y * cellPixelSize) - scrollY,
				rect.width * cellPixelSize,
				rect.height * cellPixelSize
			);
		} else if (selection != null) {
			gc.setBackground(selectedColor);
			gc.setAlpha(64);
			gc.fillRectangle(
				ox + (selection.x * cellPixelSize) - scrollX,
				oy + (selection.y * cellPixelSize) - scrollY,
				selection.width * cellPixelSize,
				selection.height * cellPixelSize
			);
			gc.setAlpha(255);
			gc.setForeground(selectedColor);
			gc.setLineWidth(2);
			gc.drawRectangle(
				ox + (selection.x * cellPixelSize) - scrollX,
				oy + (selection.y * cellPixelSize) - scrollY,
				selection.width * cellPixelSize,
				selection.height * cellPixelSize
			);
		}
	}

	private void paintPastePreview(GC gc) {
		var tileDefs = tilemap.tileDefinitions();
		var px0 = pastePreviewPos.x;
		var py0 = pastePreviewPos.y;
		var ox = calcOffsetX();
		var oy = calcOffsetY();

		// Draw the tiles from the paste buffer with semi-transparency
		gc.setAlpha(180);
		for (var r = 0; r < pastePreviewRows; r++) {
			for (var c = 0; c < pastePreviewCols; c++) {
				var col = px0 + c;
				var row = py0 + r;
				if (col >= tilemap.columns() || row >= tilemap.rows()) continue;

				var entry = pastePreview[r][c];
				var tileIdx = entry.tileIndex();
				var px = ox + (col * cellPixelSize) - scrollX;
				var py = oy + (row * cellPixelSize) - scrollY;

				if (tileIdx >= 0 && tileIdx < tileDefs.size()) {
					var cell = tileDefs.cell(tileIdx);
					var tileImg = getOrCreateTileImage(cell, entry);
					if (tileImg != null) {
						gc.drawImage(tileImg, px, py);
					}
				}
			}
		}
		gc.setAlpha(255);

		// Draw border around paste region
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		gc.setLineWidth(2);
		gc.setLineStyle(SWT.LINE_DASH);
		var pw = Math.min(pastePreviewCols, tilemap.columns() - px0) * cellPixelSize;
		var ph = Math.min(pastePreviewRows, tilemap.rows() - py0) * cellPixelSize;
		gc.drawRectangle(
			ox + (px0 * cellPixelSize) - scrollX,
			oy + (py0 * cellPixelSize) - scrollY,
			pw, ph
		);
		gc.setLineStyle(SWT.LINE_SOLID);
	}

	private void drawTileCell(GC gc, SpriteCell cell, int px, int py, TilemapEntry entry) {
		var tileImg = getOrCreateTileImage(cell, entry);
		if (tileImg != null) {
			gc.drawImage(tileImg, px, py);
		}
	}

	/**
	 * Build a cache key from the tile index + entry flags (mirror/rotate).
	 * Uses a long: high 32 bits = tile index identity hash, low 32 bits = flags.
	 */
	private long tileCacheKey(SpriteCell cell, TilemapEntry entry) {
		long key = System.identityHashCode(cell);
		key = (key << 32)
			| (entry.rotate() ? 1 : 0)
			| (entry.mirrorX() ? 2 : 0)
			| (entry.mirrorY() ? 4 : 0)
			| ((entry.paletteOffset() & 0x0F) << 3);
		return key;
	}

	private Image getOrCreateTileImage(SpriteCell cell, TilemapEntry entry) {
		var key = tileCacheKey(cell, entry);
		var img = tileCellCache.get(key);
		if (img != null && !img.isDisposed()) {
			return img;
		}

		var tileSize = cell.size();

		img = new Image(getDisplay(), cellPixelSize, cellPixelSize);
		var imgGC = new GC(img);
		try {
			// Fill with black background (transparent tiles show black)
			imgGC.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
			imgGC.fillRectangle(0, 0, cellPixelSize, cellPixelSize);

			var pal = cell.palette();
			var trans = pal.transparency();

			for (var ty = 0; ty < tileSize; ty++) {
				for (var tx = 0; tx < tileSize; tx++) {
					int sx = tx, sy = ty;
					if (entry.rotate()) {
						var tmp = sx;
						sx = tileSize - 1 - sy;
						sy = tmp;
					}
					if (entry.mirrorX()) {
						sx = tileSize - 1 - sx;
					}
					if (entry.mirrorY()) {
						sy = tileSize - 1 - sy;
					}

					var colorIndex = cell.index(sx, sy);
					if (trans.isPresent() && trans.get() == colorIndex) continue;

					// Calculate pixel-perfect bounds to fill cellPixelSize exactly
					var px = tx * cellPixelSize / tileSize;
					var py = ty * cellPixelSize / tileSize;
					var pw = (tx + 1) * cellPixelSize / tileSize - px;
					var ph = (ty + 1) * cellPixelSize / tileSize - py;

					var color = Activator.getDefault().getColorCache().color(pal.color(colorIndex));
					imgGC.setBackground(color);
					imgGC.fillRectangle(px, py, pw, ph);
				}
			}
		} finally {
			imgGC.dispose();
		}

		tileCellCache.put(key, img);
		return img;
	}

	private void disposeTileCellCache() {
		for (var img : tileCellCache.values()) {
			if (img != null && !img.isDisposed()) {
				img.dispose();
			}
		}
		tileCellCache.clear();
	}

	// --- Public API ---

	public Tilemap tilemap() {
		return tilemap;
	}

	public void tilemap(Tilemap tilemap) {
		this.tilemap = tilemap;
		invalidateCache();
		redraw();
	}

	public int selectedTileIndex() {
		return selectedTileIndex;
	}

	public void selectedTileIndex(int index) {
		this.selectedTileIndex = index;
	}

	public TilemapPaintMode mode() {
		return mode;
	}

	public void mode(TilemapPaintMode mode) {
		if (this.mode != mode) {
			this.mode = mode;
			selection = null;
			redraw();
		}
	}

	public int cellPixelSize() {
		return cellPixelSize;
	}

	public void cellPixelSize(int cellPixelSize) {
		if (this.cellPixelSize != cellPixelSize) {
			this.cellPixelSize = cellPixelSize;
			invalidateAllCaches();
			redraw();
		}
	}

	public Rectangle selection() {
		return selection;
	}

	public void deselect() {
		if (selection != null) {
			selection = null;
			redraw();
		}
	}

	public boolean hasSelection() {
		return selection != null;
	}

	/**
	 * Show a paste preview overlay. The preview follows the mouse until
	 * {@link #commitPaste()} or {@link #cancelPaste()} is called.
	 */
	public void showPastePreview(TilemapEntry[][] entries, int cols, int rows) {
		this.pastePreview = entries;
		this.pastePreviewCols = cols;
		this.pastePreviewRows = rows;
		this.pastePreviewPos = new Point(0, 0);
		this.selection = null;
		redraw();
	}

	/**
	 * Update the paste preview position based on mouse coordinates.
	 */
	public void updatePastePreviewPosition(int px, int py) {
		if (pastePreview == null) return;
		var cell = cellAt(px, py);
		if (cell != null && (pastePreviewPos == null || pastePreviewPos.x != cell.x || pastePreviewPos.y != cell.y)) {
			pastePreviewPos = cell;
			redraw();
		}
	}

	/**
	 * Commit the paste preview at the current position.
	 * Returns true if paste was applied.
	 */
	public boolean commitPaste() {
		if (pastePreview == null || pastePreviewPos == null) return false;
		for (var r = 0; r < pastePreviewRows; r++) {
			for (var c = 0; c < pastePreviewCols; c++) {
				var col = pastePreviewPos.x + c;
				var row = pastePreviewPos.y + r;
				if (col < tilemap.columns() && row < tilemap.rows()) {
					var src = pastePreview[r][c];
					var dst = tilemap.entry(col, row);
					dst.tileIndex(src.tileIndex());
					dst.mirrorX(src.mirrorX());
					dst.mirrorY(src.mirrorY());
					dst.rotate(src.rotate());
					dst.ulaOverTilemap(src.ulaOverTilemap());
					dst.paletteOffset(src.paletteOffset());
				}
			}
		}
		pastePreview = null;
		pastePreviewPos = null;
		invalidateCache();
		redraw();
		return true;
	}

	/**
	 * Cancel the paste preview.
	 */
	public void cancelPaste() {
		if (pastePreview != null) {
			pastePreview = null;
			pastePreviewPos = null;
			redraw();
		}
	}

	public boolean isPasting() {
		return pastePreview != null;
	}

	public void scrollTo(int x, int y) {
		this.scrollX = x;
		this.scrollY = y;
		redraw();
	}

	public void notifyDataChanged() {
		invalidateCache();
		redraw();
		fireChanged();
	}

	// --- Listeners ---

	public void addModifyListener(ModifyListener listener) {
		addTypedListener(listener, SWT.Modify);
	}

	public void removeModifyListener(ModifyListener listener) {
		removeListener(SWT.Modify, listener);
	}

	public void addSelectionListener(SelectionListener listener) {
		addTypedListener(listener, SWT.Selection, SWT.DefaultSelection);
	}

	public void removeSelectionListener(SelectionListener listener) {
		removeListener(SWT.Selection, listener);
		removeListener(SWT.DefaultSelection, listener);
	}

	public void addDrawListener(DrawListener listener) {
		drawListeners.add(listener);
	}

	public void removeDrawListener(DrawListener listener) {
		drawListeners.remove(listener);
	}

	private void fireChanged() {
		var ue = new Event();
		ue.widget = this;
		ue.display = getDisplay();
		ue.doit = true;
		var e = new ModifyEvent(ue);
		getTypedListeners(SWT.Modify, ModifyListener.class).forEach(l -> l.modifyText(e));
	}

	private void fireSelectionChanged() {
		var ue = new Event();
		ue.widget = this;
		ue.display = getDisplay();
		ue.doit = true;
		ue.data = selection;
		var e = new SelectionEvent(ue);
		getTypedListeners(SWT.Selection, SelectionListener.class).forEach(l -> l.widgetSelected(e));
	}

	private void fireDrawStarted() {
		drawListeners.forEach(DrawListener::drawStarted);
	}

	private void fireDrawFinished() {
		drawListeners.forEach(DrawListener::drawFinished);
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		var w = tilemap.columns() * cellPixelSize;
		var h = tilemap.rows() * cellPixelSize;
		if (wHint != SWT.DEFAULT) w = wHint;
		if (hHint != SWT.DEFAULT) h = hHint;
		return new Point(w, h);
	}

	private void interpolateCells(Point from, Point to) {
		int x0 = from.x, y0 = from.y, x1 = to.x, y1 = to.y;
		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx + dy;
		while (true) {
			placeTileAtNoRedraw(x0, y0);
			if (x0 == x1 && y0 == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy) { err += dy; x0 += sx; }
			if (e2 <= dx) { err += dx; y0 += sy; }
		}
		invalidateCache();
		redraw();
	}

	private void placeTileAtNoRedraw(int col, int row) {
		if (col < 0 || col >= tilemap.columns() || row < 0 || row >= tilemap.rows()) return;
		var entry = tilemap.entry(col, row);
		if (entry.tileIndex() != selectedTileIndex) {
			entry.tileIndex(selectedTileIndex);
			fireChanged();
		}
	}

	/**
	 * Paint a preview of the line being drawn. Shows the selected tile at each
	 * cell along the Bresenham line with a semi-transparent overlay.
	 */
	private void paintLinePreview(GC gc) {
		var tileDefs = tilemap.tileDefinitions();
		var ox = calcOffsetX();
		var oy = calcOffsetY();
		int x0 = lineStart.x, y0 = lineStart.y, x1 = lineEnd.x, y1 = lineEnd.y;
		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx + dy;

		// Draw tile preview at each cell along the line
		while (true) {
			var px = ox + (x0 * cellPixelSize) - scrollX;
			var py = oy + (y0 * cellPixelSize) - scrollY;

			// Draw the preview tile
			if (selectedTileIndex >= 0 && selectedTileIndex < tileDefs.size()) {
				var cell = tileDefs.cell(selectedTileIndex);
				var entry = tilemap.entry(x0, y0);
				var tileImg = getOrCreateTileImage(cell, entry);
				if (tileImg != null) {
					gc.drawImage(tileImg, px, py);
				}
			}

			// Draw highlight border
			gc.setForeground(selectedColor);
			gc.setAlpha(192);
			gc.setLineWidth(2);
			gc.drawRectangle(px, py, cellPixelSize, cellPixelSize);
			gc.setAlpha(255);

			if (x0 == x1 && y0 == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy) { err += dy; x0 += sx; }
			if (e2 <= dx) { err += dx; y0 += sy; }
		}
	}

	/**
	 * Commit a line of tiles from start to end using Bresenham's algorithm.
	 */
	private void commitLine(Point from, Point to) {
		fireDrawStarted();
		int x0 = from.x, y0 = from.y, x1 = to.x, y1 = to.y;
		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx + dy;
		while (true) {
			placeTileAtNoRedraw(x0, y0);
			if (x0 == x1 && y0 == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy) { err += dy; x0 += sx; }
			if (e2 <= dx) { err += dx; y0 += sy; }
		}
		invalidateCache();
		redraw();
		fireDrawFinished();
	}
}
