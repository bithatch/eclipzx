package uk.co.bithatch.drawzx.widgets;

import java.util.function.BiConsumer;

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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import uk.co.bithatch.drawzx.Activator;
import uk.co.bithatch.drawzx.editor.EditorFileProperties.SpritePaintMode;
import uk.co.bithatch.drawzx.sprites.SpriteCell;

public class SpriteEditorGrid extends AbstractSpriteGrid {

	private static final int DEFAULT_CELL_SIZE = 16;
	
	public static Point normalize(Point point, int max, int pixelSize, int offsetX, int offsetY) {
		return new Point(Math.max(0, Math.min(max - 1, (point.x - offsetX) / pixelSize)), Math.max(0, Math.min(max - 1, (point.y - offsetY) / pixelSize)));
	}
	
	@Deprecated
	public static Rectangle normalizeSelectionRect(Point selectPoint, Point dragPoint, int max, int pixelSize, int offsetX, int offsetY) {
		
		var sx = ( selectPoint.x - offsetX ) / pixelSize;
		var sy = ( selectPoint.y - offsetY ) / pixelSize;
		var dx = ( dragPoint.x - offsetX ) / pixelSize;
		var dy = ( dragPoint.y - offsetY ) / pixelSize;
		if(dx < sx) {
			var a = dx;
			dx = sx;
			sx = a;
		}
		if(dy < sy) {
			var a = dy;
			dy = sy;
			sy = a;
		}
		
		sx = Math.max(0, sx);
		sy = Math.max(0, sy);
		dx = Math.min(max - 1, dx);
		dy = Math.min(max - 1, dy);
		return new Rectangle(sx, sy, dx + 1 - sx, dy + 1 - sy);
	}
	private int color;
	private int secondaryColor;
	private Point dragPoint, normDragPoint, mouseDownPoint;
	private boolean drawing;
	private boolean lineDrawing;
	private SpritePaintMode mode = SpritePaintMode.SELECT;
	private Rectangle selection;
	private Point selectPoint, normSelectPoint;
	private boolean shiftDown;
	private boolean drawWithSecondary;

	public SpriteEditorGrid(Composite parent, int style) {
		this(parent, DEFAULT_CELL_SIZE, style);
	}

	public SpriteEditorGrid(Composite parent, int cellSize, int style) {
		this(parent, new SpriteCell(), cellSize, style);
	}

	public SpriteEditorGrid(Composite parent, SpriteCell spriteCell, int style) {
		this(parent, spriteCell, 4, style);
	}

	public SpriteEditorGrid(Composite parent, SpriteCell spriteCell, int cellSize, int style) {
		super(parent, spriteCell, cellSize, style);
		
		addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.SHIFT) {
					shiftDown = true;
					if(!lineDrawing) {
						lineDrawing = true;
						if(selectPoint == null && mouseDownPoint != null) {
							setSelectPoint(mouseDownPoint);
							setDragPoint(mouseDownPoint);
							
						}
					}
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.keyCode == SWT.SHIFT) {
					lineDrawing = false;
					shiftDown = false;
				}
				
			}
		});
		
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}
			
			@Override
			public void mouseDown(MouseEvent e) {
				mouseDownPoint = new Point(e.x, e.y);
				if(mode == SpritePaintMode.SELECT) {
					setDragPoint(e);
					setSelectPoint(e);
					redraw();
				}
				else if(mode == SpritePaintMode.DRAW) {
					if(lineDrawing) {
						drawWithSecondary = ( e.stateMask & SWT.CONTROL ) != 0;
						setDragPoint(e);
//						if(normSelectPoint == null)
//							setSelectPoint(e);
						redraw();
					}
					else {
						resetDragAndSelectPoints();
						if(e.button == 1) {
							drawing = true;
							drawWithSecondary = ( e.stateMask & SWT.CONTROL ) != 0;
							drawAt(normalize(mouseDownPoint));
						}
					}
				}
			}
			
			@Override
			public void mouseUp(MouseEvent e) {
				if(mode == SpritePaintMode.SELECT && dragPoint != null) {
					
					var ue = new Event();
					ue.widget = SpriteEditorGrid.this;
					ue.display = getDisplay();
					ue.doit = true;
					ue.data = selection = normalizeSelectionRect(
						selectPoint, 
						dragPoint, 
						spriteCell.size(), 
						calPixelSize(), 
						calcOffsetX(), 
						calcOffsetY()
					);
					
					var ev = new SelectionEvent(ue);
					getTypedListeners(SWT.Selection, SelectionListener.class).forEach(l -> l.widgetSelected(ev));
				}
				else if(lineDrawing) {
					applyToSelectedLine(calcOffsetX(), calcOffsetY(), calPixelSize(), (x,y) -> {
						spriteCell().index(x, y, drawWithSecondary ? secondaryColor : color);
						redraw();
						fireChanged();				
					});
				}
				if(shiftDown) {
					setSelectPoint(e);
					setDragPoint(e);
					mouseDownPoint = selectPoint;
				}
				else {
					resetDragAndSelectPoints();
				}
				drawing = false;
				redraw();
			}
		});
		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				if(dragPoint != null || (lineDrawing && dragPoint == null)) {
					setDragPoint(e);
					redraw();
				}
				else if(mode == SpritePaintMode.DRAW && drawing) {
					mouseDownPoint = new Point(e.x, e.y);
					drawWithSecondary = ( e.stateMask & SWT.CONTROL ) != 0;
					drawAt(normalize(mouseDownPoint));
				}
			}
		});
		
	}

	public void addModifyListener(ModifyListener listener) {
		addTypedListener(listener, SWT.Modify);
	}
	
	public void addSelectionListener(SelectionListener listener) {
		addTypedListener(listener, SWT.Selection, SWT.DefaultSelection);
	}
	
	public void clear() {
		clear(color());
	}

	public void clear(int index) {
		spriteCell().clear(index);
		redraw();
		fireChanged();
	}

	public int clearIndex() {
		return adjustForPaletteOffset(spriteCell().palette().transparency().orElse(0));
	}
	
	public int[][] copyPixels() {
		return cutOrCopy(false);
	}

	public int[][] cutPixels() {
		return cutOrCopy(true);
	}

	public void deselect() {
		if(selection != null) {
			selection = null;
			redraw();
		}
	}

	public int color() {
		return color;
	}

	public int secondaryColor() {
		return secondaryColor;
	}

	public SpritePaintMode mode() {
		return mode;
	}
	
	public boolean hasSelection() {
		return selection != null;
	}

	public void invert() {
		spriteCell().invert(paletteOffset() == -1 ? spriteCell().palette().size() : 16);
		redraw();
		fireChanged();
	}

	public void mirrorH() {
		spriteCell().mirrorH();
		redraw();
		fireChanged();
	}

	public void mirrorV() {
		spriteCell().mirrorV();
		redraw();
		fireChanged();
	}

	public void paste(int[][] pixels) {
		var ox = selection == null ? 0 : selection.x;
		var oy = selection == null ? 0 : selection.y;
		if(pixels.length == 0)
			return;
		for(var r = 0 ; r < pixels.length; r++) {
			if(r + oy >= spriteCell().size())
				break;
			for(var c = 0 ; c < pixels[r].length; c++) {
				if(c + ox >= spriteCell().size())
					break;
				spriteCell().index(c + ox, r + oy, adjustForPaletteOffset(pixels[r][c]));
			}
		}
		selection = new Rectangle(ox, oy, pixels[0].length, pixels.length);
		redraw();
	}

	public void removeModifyListener(ModifyListener listener) {
		removeListener(SWT.Modify, listener);
	}

	public void removeSelectionListener(SelectionListener listener) {
		removeListener(SWT.Selection, listener);
		removeListener(SWT.DefaultSelection, listener);
	}

	public void rotate(int degrees) {
		spriteCell().rotate(degrees);
		redraw();
		fireChanged();
		
	}

	public Rectangle selection() {
		return selection;
	}

	public void color(int color) {
		if(paletteOffset() != -1) {
			color = color % 16;
		}
		this.color = color;
	}

	public void secondaryColor(int secondaryColor) {
		if(paletteOffset() != -1) {
			secondaryColor = secondaryColor % 16;
		}
		this.secondaryColor = secondaryColor;
	}

	public void mode(SpritePaintMode mode) {
		if(mode != this.mode) {
			this.mode = mode;
			selection = null;
			redraw();
		}
	}

	public void shift(int h, int v) {
		spriteCell().shift(h, v); 
		redraw();
		fireChanged();
	}

	protected int adjustForPaletteOffset(int index) {
		return paletteOffset() == -1 ? index : index % 16;
	}

	protected void applyToSelectedLine(int offsetX, int offsetY, int pixelSize, BiConsumer<Integer, Integer> function) {
		int x, y;
		int dx, dy;
		int incx, incy;
		int balance;

		if (normDragPoint.x >= normSelectPoint.x) {
			dx = normDragPoint.x - normSelectPoint.x;
			incx = 1;
		} else {
			dx = normSelectPoint.x - normDragPoint.x;
			incx = -1;
		}

		if (normDragPoint.y >= normSelectPoint.y) {
			dy = normDragPoint.y - normSelectPoint.y;
			incy = 1;
		} else {
			dy = normSelectPoint.y - normDragPoint.y;
			incy = -1;
		}

		x = normSelectPoint.x;
		y = normSelectPoint.y;

		if(dx >= dy) {
			dy <<= 1;
			balance = dy - dx;
			dx <<= 1;
			while(x != normDragPoint.x + incx) {
				function.accept(x, y);
				if (balance >= 0) {
					y += incy;
					balance -= dx;
				}
				balance += dy;
				x += incx;
			}
		}
		else {
			dx <<= 1;
			balance = dx - dy;
			dy <<= 1;
			while(y != normDragPoint.y + incy) {
				function.accept(x, y);
				if (balance >= 0) {
					x += incx;
					balance -= dy;
				}
				balance += dx;
				y += incy;
			}
		}
	}

	@Override
	protected void onDrawTile(GC g, int offsetX, int offsetY, int pixelSize) {
		if(lineDrawing && normSelectPoint != null) {		
			var cellColor = Activator.getDefault().getColorCache().color(spriteCell().palette().color(
				drawWithSecondary ? secondaryColor : color
			));
			applyToSelectedLine(offsetX, offsetY, pixelSize, (x,y) -> {
				g.setBackground(cellColor);
				g.fillRectangle((x * pixelSize) + offsetX, (y * pixelSize) + offsetY, pixelSize, pixelSize);				
			});
		}
	}
	@Override
	protected void onDrawTileAfterBorder(GC g, int offsetX, int offsetY, int pixelSize) {
		if(mode == SpritePaintMode.SELECT && selectPoint != null && !lineDrawing) {
			var x1 = Math.min(selectPoint.x, dragPoint.x);
			var x2 = Math.max(selectPoint.x, dragPoint.x);
			var y1 = Math.min(selectPoint.y, dragPoint.y);
			var y2 = Math.max(selectPoint.y, dragPoint.y);
			g.setForeground(selectedColor);
			g.setLineWidth(Math.min(4, pixelSize / 8));
			g.drawRectangle(x1, y1, x2 - x1, y2 - y1);
		}
		else if(selection != null) {

			g.setBackground(selectedColor);
			g.setAlpha(128);
			g.fillRectangle((selection.x * pixelSize) + offsetX, (selection.y * pixelSize) + offsetY,
					selection.width * pixelSize, selection.height * pixelSize);
			g.setAlpha(255);
			
			g.setForeground(selectedColor);
			g.setLineWidth(Math.min(4, pixelSize / 8));
			g.drawRectangle((selection.x * pixelSize) + offsetX, (selection.y * pixelSize) + offsetY,
					selection.width * pixelSize, selection.height * pixelSize);
			
		}
	}
	
	@Override
	protected void onPaletteOffsetChanged() {
		if(paletteOffset() != -1) {
			color = color % 16;
			secondaryColor = secondaryColor % 16;
		}
		super.onPaletteOffsetChanged();
	}
	
	protected void resetDragAndSelectPoints() {
		normDragPoint = dragPoint = selectPoint = normSelectPoint = null;
		drawWithSecondary = lineDrawing = false;
	}
	
	protected void setDragPoint(Point point) {
		dragPoint = point;
		normDragPoint = normalize(dragPoint, spriteCell().size(), calPixelSize(), calcOffsetX(), calcOffsetY());
	}

	private int[][] cutOrCopy(boolean cut) {
		if(selection == null)
			return new int[0][0];
		else {
			var sel = new int[selection.height][selection.width];
			for(var r = 0 ; r < selection.height; r++) {
				for(var c = 0 ; c < selection.width; c++) {
					if(cut)
						sel[r][c] = spriteCell().index(c + selection.x, r + selection.y, clearIndex());
					else
						sel[r][c] = spriteCell().index(c + selection.x, r + selection.y);
				}
			}
			return sel;
		}
	}

	private void drawAt(Point point) {

		var was = spriteCell().index(point.x, point.y);
		var drawCol = drawWithSecondary ? secondaryColor : color;
		if(was != drawCol) {
			spriteCell().index(point.x, point.y, drawCol);
			redraw();
			fireChanged();
		}
	}

	private void fireChanged() {
		var ue = new Event();
		ue.widget = this;
		ue.display = getDisplay();
		ue.doit = true;
		ue.data = spriteCell();
		var e = new ModifyEvent(ue);
		getTypedListeners(SWT.Modify, ModifyListener.class).forEach(l -> l.modifyText(e));
	}

	private void setDragPoint(MouseEvent e) {
		setDragPoint(new Point(e.x, e.y));
	}

	private void setSelectPoint(MouseEvent e) {
		setSelectPoint(new Point(e.x, e.y));
	}

	private void setSelectPoint(Point point) {
		selectPoint = point;
		normSelectPoint = normalize(point);
	}

	private Point normalize(Point point) {
		return normalize(point, spriteCell().size(),calPixelSize(), calcOffsetX(), calcOffsetY());
	}

}
