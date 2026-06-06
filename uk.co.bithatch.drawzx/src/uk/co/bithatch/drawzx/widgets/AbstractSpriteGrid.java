package uk.co.bithatch.drawzx.widgets;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import uk.co.bithatch.drawzx.Activator;
import uk.co.bithatch.drawzx.sprites.SpriteCell;

public abstract class AbstractSpriteGrid extends Canvas {
	
	public enum BackgroundType {
		TRANSPARENT, LARGE_CHEQUER, SMALL_CHEQUER, BLACK, WHITE, FOREGROUND, BACKGROUND, INDEX
	}

	private static final int DEFAULT_CELL_SIZE = 16;
	private int cellSize;
	private SpriteCell spriteCell;
	private Color borderColor;
	private boolean border;
	private boolean inverse;
	protected Color selectedColor;
	private BackgroundType backgroundType = BackgroundType.TRANSPARENT;
	private int paletteOffset = -1;
	private Image cachedImage;
	private boolean cacheDirty = true;

	public AbstractSpriteGrid(Composite parent, int style) {
		this(parent, DEFAULT_CELL_SIZE, style);
	}

	public AbstractSpriteGrid(Composite parent, int cellSize, int style) {
		this(parent, new SpriteCell(), cellSize, style);
	}

	public AbstractSpriteGrid(Composite parent, SpriteCell spriteCell, int style) {
		this(parent, spriteCell, 4, style);
	}

	public AbstractSpriteGrid(Composite parent, SpriteCell spriteCell, int cellSize, int style) {
		super(parent, style);
		this.cellSize = cellSize;
		border = (style & SWT.BORDER) != 0;
		borderColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER);
		selectedColor = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);

		setSpriteCell(spriteCell);
		addPaintListener(e -> paintCached(e.gc));
		addListener(SWT.Resize, e -> invalidateCache());
	}
	
	/**
	 * Mark the cached image as dirty so it will be rebuilt on next paint.
	 * Subclasses should call this when visual state changes.
	 */
	public void invalidateCache() {
		cacheDirty = true;
	}

	@Override
	public void redraw() {
		invalidateCache();
		super.redraw();
	}

	@Override
	public void redraw(int x, int y, int width, int height, boolean all) {
		invalidateCache();
		super.redraw(x, y, width, height, all);
	}

	public int paletteOffset() {
		return paletteOffset;
	}

	public void paletteOffset(int paletteOffset) {
		if(paletteOffset != this.paletteOffset) {
			this.paletteOffset = paletteOffset;
			onPaletteOffsetChanged();
		}
	}

	public BackgroundType backgroundType() {
		return backgroundType;
	}

	public void backgroundType(BackgroundType backgroundType) {
		this.backgroundType = backgroundType;
	}

	public SpriteCell spriteCell() {
		return spriteCell;
	}

	@Override
	public void dispose() {
		disposeCachedImage();
		super.dispose();
	}

	public boolean isInverse() {
		return inverse;
	}

	public void setInverse(boolean inverse) {
		if(this.inverse != inverse) {
			this.inverse = inverse;
			onPaletteOffsetChanged();
		}
	}

	public void setSpriteCell(SpriteCell spriteCell) {
		if (!Objects.equals(spriteCell, this.spriteCell)) {
			this.spriteCell = spriteCell;
			setSize(cellSize * spriteCell.size(), cellSize * spriteCell.size());
			onPaletteOffsetChanged();
			requestLayout();
		}
	}

	protected void onPaletteOffsetChanged() {
		invalidateCache();
		redraw();
	}

	protected int cellSize() {
		return cellSize;
	}

	protected int calPixelSize() {
		var bounds = getClientArea();
		var pixels = spriteCell.size();
		var cellX = bounds.width / pixels;
		var cellY = bounds.height / pixels;
		var cellSize = Math.min(cellX, cellY);
		return cellSize;
	}

	protected void onDrawTileAfterBorder(GC g, int offsetX, int offsetY, int pixelSize) {
	}

	protected void onDrawTile(GC g, int offsetX, int offsetY, int pixelSize) {
	}

	protected int calcOffsetX() {
		return (getClientArea().width - (calPixelSize() * spriteCell.size())) / 2;
	}

	protected int calcOffsetY() {
		return (getClientArea().height - (calPixelSize()* spriteCell.size())) / 2;
	}

	private void disposeCachedImage() {
		if (cachedImage != null && !cachedImage.isDisposed()) {
			cachedImage.dispose();
			cachedImage = null;
		}
	}

	private void paintCached(GC gc) {
		if (isDisposed())
			return;
		var area = getClientArea();
		if (area.width <= 0 || area.height <= 0)
			return;

		if (cacheDirty || cachedImage == null || cachedImage.isDisposed()
				|| cachedImage.getBounds().width != area.width
				|| cachedImage.getBounds().height != area.height) {
			disposeCachedImage();
			cachedImage = new Image(getDisplay(), area.width, area.height);
			var imgGC = new GC(cachedImage);
			try {
				// Fill with widget background first
				imgGC.setBackground(getBackground());
				imgGC.fillRectangle(0, 0, area.width, area.height);
				drawTile(imgGC);
			} finally {
				imgGC.dispose();
			}
			cacheDirty = false;
		}
		gc.drawImage(cachedImage, 0, 0);
	}

	private void drawTile(GC gc) {

		var rows = spriteCell.size();
		var pixelSize = calPixelSize();
		var offsetX = calcOffsetX();
		var offsetY = calcOffsetY();
		var pal = spriteCell.palette();
		var trans = pal.transparency();
		

		switch(backgroundType) {
		case LARGE_CHEQUER:
			var width = pixelSize * rows;
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
			gc.fillRectangle(offsetX, offsetY, width, width);
			gc.setBackground(getForeground());
			gc.setAlpha(64);
			gc.fillRectangle(offsetX, offsetY, width/ 2, width / 2);
			gc.fillRectangle(offsetX + ( width / 2), offsetY + ( width/ 2), width / 2, width / 2);
			gc.setAlpha(255);
			break;
		default:
			break;
		}

		for (var y = 0; y < rows; y++) {
				
			for (var x = 0; x < rows; x++) {
				var xx = (x * pixelSize) + offsetX;
				var yy = (y * pixelSize) + offsetY;
				
				var palIndex = adjustForPaletteOffset(inverse 
						? spriteCell.inverseIndex(x, y, paletteOffset == -1 
							? spriteCell.palette().size() 
							: 16) 
						: spriteCell.index(x, y));
				
				if(trans.isEmpty() ||  adjustForPaletteOffset(trans.get()) != palIndex || backgroundType == BackgroundType.INDEX) {
					var cellColor = Activator.getDefault().getColorCache().color(pal.color(palIndex));
					gc.setBackground(cellColor);
					gc.fillRectangle(xx, yy, pixelSize, pixelSize);	
				}
				else {
					switch(backgroundType) {
					case FOREGROUND:
						gc.setBackground(getForeground());
						gc.fillRectangle(xx, yy, pixelSize, pixelSize);
						break;
					case BACKGROUND:
						gc.setBackground(getBackground());
						gc.fillRectangle(xx, yy, pixelSize, pixelSize);
						break;
					case BLACK:
						gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
						gc.fillRectangle(xx, yy, pixelSize, pixelSize);
						break;
					case WHITE:
						gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
						gc.fillRectangle(xx, yy, pixelSize, pixelSize);
						break;
					case SMALL_CHEQUER:
						gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
						gc.fillRectangle(xx, yy, pixelSize, pixelSize);
						gc.setBackground(getForeground());
						gc.setAlpha(64);
						gc.fillRectangle(xx, yy, pixelSize / 2, pixelSize / 2);
						gc.fillRectangle(xx + (pixelSize / 2), yy + (pixelSize / 2), pixelSize / 2, pixelSize / 2);
						gc.setAlpha(255);
						break;
					default:
						break;
					}
				}
			}
		}

		onDrawTile(gc, offsetX, offsetY, pixelSize);
		
		
		if(border) {
			gc.setForeground(borderColor);
			for (var y = 0; y < ( rows + 1 ); y++) {
				var ey = ( y * pixelSize) + offsetY;
				gc.drawLine(offsetX, ey, offsetX + (rows * pixelSize), ey);
			}	
			for (var x = 0; x < ( rows + 1 ) ; x++) {
				var ex = offsetX + (x * pixelSize);
				gc.drawLine(ex, offsetY, ex, offsetY + (rows * pixelSize));
			}
		}
		
		
		onDrawTileAfterBorder(gc, offsetX, offsetY, pixelSize);

	}

	private int adjustForPaletteOffset(int index) {
		if(paletteOffset == -1)
			return index;
		else {
			var r = ( paletteOffset * 16 ) +index;
			return r;
		}
	}


}
