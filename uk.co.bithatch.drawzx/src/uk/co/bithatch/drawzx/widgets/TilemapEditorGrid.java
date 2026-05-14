package uk.co.bithatch.drawzx.widgets;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
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

	public TilemapEditorGrid(Composite parent, Tilemap tilemap, int style) {
		this(parent, tilemap, 16, style);
	}

	public TilemapEditorGrid(Composite parent, Tilemap tilemap, int cellPixelSize, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED);
		this.tilemap = tilemap;
		this.cellPixelSize = cellPixelSize;
		gridColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER);
		selectedColor = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);

		addPaintListener(e -> paintTilemap(e.gc));
		setupMouseListeners();
	}

	private void setupMouseListeners() {
		addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}

			@Override
			public void mouseDown(MouseEvent e) {
				var cell = cellAt(e.x, e.y);
				if (cell == null) return;

				if (mode == TilemapPaintMode.SELECT) {
					selectStart = cell;
					selectEnd = cell;
					selection = null;
					redraw();
				} else if (mode == TilemapPaintMode.PLACE) {
					drawing = true;
					fireDrawStarted();
					placeTileAt(cell);
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
				}
				if (drawing) {
					drawing = false;
					fireDrawFinished();
				}
			}
		});

		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				if (mode == TilemapPaintMode.SELECT && selectStart != null) {
					var cell = cellAt(e.x, e.y);
					if (cell != null) {
						selectEnd = cell;
						redraw();
					}
				} else if (mode == TilemapPaintMode.PLACE && drawing) {
					var cell = cellAt(e.x, e.y);
					if (cell != null) {
						placeTileAt(cell);
					}
				}
			}
		});
	}

	private void placeTileAt(Point cell) {
		var entry = tilemap.entry(cell.x, cell.y);
		if (entry.tileIndex() != selectedTileIndex) {
			entry.tileIndex(selectedTileIndex);
			redraw();
			fireChanged();
		}
	}

	private void floodFill(int col, int row, int newTileIndex) {
		if (col < 0 || col >= tilemap.columns() || row < 0 || row >= tilemap.rows()) return;
		var targetIndex = tilemap.entry(col, row).tileIndex();
		if (targetIndex == newTileIndex) return;
		floodFillRecurse(col, row, targetIndex, newTileIndex);
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
		var col = (px + scrollX) / cellPixelSize;
		var row = (py + scrollY) / cellPixelSize;
		if (col >= 0 && col < tilemap.columns() && row >= 0 && row < tilemap.rows()) {
			return new Point(col, row);
		}
		return null;
	}

	private Rectangle normalizeRect(Point a, Point b) {
		var x1 = Math.min(a.x, b.x);
		var y1 = Math.min(a.y, b.y);
		var x2 = Math.max(a.x, b.x);
		var y2 = Math.max(a.y, b.y);
		return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
	}

	private void paintTilemap(GC gc) {
		var bounds = getClientArea();
		var cols = tilemap.columns();
		var rows = tilemap.rows();
		var tileDefs = tilemap.tileDefinitions();

		// Background
		gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		gc.fillRectangle(bounds);

		// Draw each tile
		for (var r = 0; r < rows; r++) {
			for (var c = 0; c < cols; c++) {
				var px = (c * cellPixelSize) - scrollX;
				var py = (r * cellPixelSize) - scrollY;

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
			var y = (r * cellPixelSize) - scrollY;
			gc.drawLine(-scrollX, y, (cols * cellPixelSize) - scrollX, y);
		}
		for (var c = 0; c <= cols; c++) {
			var x = (c * cellPixelSize) - scrollX;
			gc.drawLine(x, -scrollY, x, (rows * cellPixelSize) - scrollY);
		}
		gc.setAlpha(255);

		// Selection highlight
		if (selectStart != null && selectEnd != null) {
			var rect = normalizeRect(selectStart, selectEnd);
			gc.setForeground(selectedColor);
			gc.setLineWidth(2);
			gc.drawRectangle(
				(rect.x * cellPixelSize) - scrollX,
				(rect.y * cellPixelSize) - scrollY,
				rect.width * cellPixelSize,
				rect.height * cellPixelSize
			);
		} else if (selection != null) {
			gc.setBackground(selectedColor);
			gc.setAlpha(64);
			gc.fillRectangle(
				(selection.x * cellPixelSize) - scrollX,
				(selection.y * cellPixelSize) - scrollY,
				selection.width * cellPixelSize,
				selection.height * cellPixelSize
			);
			gc.setAlpha(255);
			gc.setForeground(selectedColor);
			gc.setLineWidth(2);
			gc.drawRectangle(
				(selection.x * cellPixelSize) - scrollX,
				(selection.y * cellPixelSize) - scrollY,
				selection.width * cellPixelSize,
				selection.height * cellPixelSize
			);
		}
	}

	private void drawTileCell(GC gc, SpriteCell cell, int px, int py, TilemapEntry entry) {
		var tileSize = cell.size();
		var scale = cellPixelSize / tileSize;
		if (scale < 1) scale = 1;
		var pal = cell.palette();

		for (var ty = 0; ty < tileSize; ty++) {
			for (var tx = 0; tx < tileSize; tx++) {
				// Apply mirror/rotate transforms
				int sx = tx, sy = ty;
				if (entry.rotate()) {
					// 90 degree clockwise rotation
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
				var trans = pal.transparency();
				if (trans.isPresent() && trans.get() == colorIndex) continue;

				var color = Activator.getDefault().getColorCache().color(pal.color(colorIndex));
				gc.setBackground(color);
				gc.fillRectangle(px + (tx * scale), py + (ty * scale), scale, scale);
			}
		}
	}

	// --- Public API ---

	public Tilemap tilemap() {
		return tilemap;
	}

	public void tilemap(Tilemap tilemap) {
		this.tilemap = tilemap;
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

	public void scrollTo(int x, int y) {
		this.scrollX = x;
		this.scrollY = y;
		redraw();
	}

	public void notifyDataChanged() {
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
}
