package uk.co.bithatch.drawzx.widgets;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import uk.co.bithatch.drawzx.Activator;
import uk.co.bithatch.zyxy.graphics.AttributedVideoMemory;
import uk.co.bithatch.zyxy.graphics.Constants;
import uk.co.bithatch.zyxy.graphics.MaskedAttributedVideoMemory;
import uk.co.bithatch.zyxy.graphics.Palette;
import uk.co.bithatch.zyxy.graphics.StandardAttributedVideoMemory;
import uk.co.bithatch.zyxy.graphics.VideoMemory;
import uk.co.bithatch.zyxy.graphics.VideoMode;

public final class DrawSurface {

	public record XY(int x, int y) {}
	
	public interface DirtyListener  {
		void dirty(Rectangle bounds);
	}
	
	private Brush brush = Brush.CIRCLE;
	private int brushSize = 1;
	private int sprayDensity = 25;
	private VideoMemory buffer;
	private boolean forceClear;
	private boolean clearOn;
	private int cursorX;
	private int cursorY;
	private boolean flashOn;
	private boolean standardAttributed;
	private boolean attributed ;
	private boolean maskedAttributed;
	private boolean monochrome;
	private int ink = 7;
	private int alpha = Constants.DEFAULT_TRANSPARENCY;
	private boolean inkOn = true;

	private Palette palette;
	private int paper = Constants.DEFAULT_TRANSPARENCY;	
	private boolean paperOn = true;
	private int widthProperty = Constants.SCREEN_WIDTH;
	private int heightProperty = Constants.SCREEN_HEIGHT;
	private VideoMemory snapshot;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	

	private ImageData blockImage;
//	private final ObjectProperty<WritableImage> image = new SimpleObjectProperty<>();
//	private PixelReader pixelReader;
	private List<DirtyListener> dirtyListeners = new ArrayList<>();
	private Rectangle dirtyBounds;
	private boolean flashActive;

	public DrawSurface() {
		this(Palette.rgb16(), VideoMode.STANDARD);
	}

	public DrawSurface(Palette palette, VideoMode mode) {
		this(palette, mode.createBuffer());
	}
	
	public DrawSurface(Palette palette, VideoMemory buffer) {
		this.buffer = buffer;
		attributed = buffer instanceof AttributedVideoMemory;
		this.palette = palette;
		monochrome = palette.size() < 3;
		redraw();
//		image.set(createImage(buffer));
//		dirty = true;
	}
	
	public void addDirtyListener(DirtyListener listener) {
		this.dirtyListeners.add(listener);
	}
	
	public void removeDirtyListener(DirtyListener listener) {
		this.dirtyListeners.remove(listener);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public void apply() {
		reset();
	}
	
	public Brush brush() {
		return brush;
	}
	
	public void brush(Brush brush) {
		if(!Objects.equals(brush, this.brush)) {
			this.brush = brush;
			pcs.firePropertyChange("brush", this.brush, brush);
		}
	}
	
	public ImageData blockImage() {
		return blockImage;
	}
	
	public void blockImage(ImageData blockImage) {
		if(!Objects.equals(blockImage, this.blockImage)) {
			this.blockImage = blockImage;
			pcs.firePropertyChange("blockImage", this.blockImage, blockImage);
		}
	}
	
	public int brushSize() {
		return brushSize;
	}

	public void brushSize(int brushSize) {
		if(!Objects.equals(brushSize, this.brushSize)) {
			this.brushSize = brushSize;
			pcs.firePropertyChange("brushSize", this.brushSize, brushSize);
		}
	}

	public int alpha() {
		return alpha;
	}

	public void alpha(int alpha) {
		if(!Objects.equals(alpha, this.alpha)) {
			this.alpha = alpha;
			pcs.firePropertyChange("alpha", this.alpha, alpha);
		}
	}

	public int sprayDensity() {
		return sprayDensity;
	}

	public void sprayDensity(int sprayDensity) {

		if(!Objects.equals(sprayDensity, this.sprayDensity)) {
			this.sprayDensity = sprayDensity;
			pcs.firePropertyChange("sprayDensity", this.sprayDensity, sprayDensity);
		}
		
	}

	public VideoMemory buffer() {
		return buffer;
	}

	public void clear() {
		forceClear = true;
		try {
			fillRect(0, 0, buffer().mode().width(), buffer().mode().height());
		}
		finally {
			forceClear = false;
		}
	}

	public boolean clearOn() {
		return clearOn;
	}
	
	public void clearOn(boolean clearOn) {
		if(!Objects.equals(clearOn, this.clearOn)) {
			this.clearOn = clearOn;
			pcs.firePropertyChange("clearOn", this.clearOn, clearOn);
		}
	}

	public boolean flashActive() {
		return flashActive;
	}
	
	public void flashActiveOn(boolean flashActive) {
		if(!Objects.equals(flashActive, this.flashActive)) {
			this.flashActive = flashActive;
			pcs.firePropertyChange("clearOn", this.flashActive, flashActive);
			redraw();
		}
	}
	
	public boolean attributed() {
		return attributed;
	}
	
	public void attributed(boolean attributed) {
		if(!Objects.equals(attributed, this.attributed)) {
			this.attributed = attributed;
			pcs.firePropertyChange("attributed", this.attributed, attributed);
		}
	}
	
	public boolean standardAttributed() {
		return standardAttributed;
	}
	
	public void standardAttributed(boolean standardAttributed) {
		if(!Objects.equals(standardAttributed, this.standardAttributed)) {
			this.standardAttributed = standardAttributed;
			pcs.firePropertyChange("standardAttributed", this.standardAttributed, standardAttributed);
		}
	}
	
	public boolean maskedAttributed() {
		return maskedAttributed;
	}
	
	public void maskedAttributed(boolean maskedAttributed) {
		if(!Objects.equals(maskedAttributed, this.maskedAttributed)) {
			this.maskedAttributed = maskedAttributed;
			pcs.firePropertyChange("maskedAttributed", this.maskedAttributed, maskedAttributed);
		}
	}
	
	public boolean monochrome() {
		return monochrome;
	}
	
	public void monochrome(boolean monochrome) {
		if(!Objects.equals(monochrome, this.monochrome)) {
			this.monochrome = monochrome;
			pcs.firePropertyChange("monochrome", this.monochrome, monochrome);
		}
	}
	
	public int cursorX() {
		return cursorX;
	}
	
	public void cursorX(int cursorX) {
		if(!Objects.equals(cursorX, this.cursorX)) {
			this.cursorX = cursorX;
			pcs.firePropertyChange("cursorX", this.cursorX, cursorX);
		}
	}
	
	public int cursorY() {
		return cursorY;
	}
	
	public void cursorY(int cursorY) {
		if(!Objects.equals(cursorY, this.cursorY)) {
			this.cursorY = cursorY;
			pcs.firePropertyChange("cursorY", this.cursorY, cursorY);
		}
	}
	
	public void line(int x1, int y1, int x2, int y2) {
		recolor(doDrawLine(x1, y1, x2, y2, brush(), brushSize(), new LinkedHashSet<XY>()));
		fireDirtyListeners();
	}

	public void lineTo(int x, int y) {
		var cx = cursorX;
		var cy = cursorY;
		line(cx == -1 ? x : cx, cy == -1 ? y : cy, x, y);
		cursorX = x;
		cursorY = y;
	}
	
	public void fill(int x, int y) {
		var inkCol = ink();
		if(!inside(inkCol, x, y)) {
			return;
		}
		
		var s = new Stack<int[]>();
		var cells = new LinkedHashSet<XY>();
		s.add(new int[] {x, y});
		while(!s.isEmpty()) {
			var c = s.removeFirst();
			var xx = c[0];
			var yy = c[1];
			var lx = xx;
			while(inside(inkCol, lx - 1, yy)) {
				plot(lx - 1, yy, cells);
				lx--;
			}
			while(inside(inkCol, xx, yy)) {
				plot(xx, yy, cells);
				xx++;
			}
			scan(inkCol, lx, xx - 1, yy + 1, s);
			scan(inkCol, lx, xx - 1, yy - 1, s);
		}
		recolor(cells);
		fireDirtyListeners();
	}

	public void fillCircle(int cx, int cy, int cr) {
		recolor(doFillCircle(cx, cy, cr, new LinkedHashSet<XY>()));
		fireDirtyListeners();
	}

	public void fillCircleTo(int cr) {
		var cx = cursorX;
		var cy = cursorY;
		var sx = cx == -1 ? 0 : cx;
		var sy = cy == -1 ? 0 : cy;
		fillCircle(sx, sy, cr);
		cursorX = sx + cr;
		cursorY = sy + cr;
	}

	public void fillRect(int x, int y, int w, int h) {
		if (w < 1 || h < 1)
			return;
		recolor(doFillRect(x, y, w, h, Brush.CIRCLE, 1, new LinkedHashSet<XY>()));
		fireDirtyListeners();
	}

	public void fillRectTo(int w, int h) {
		var cx = cursorX;
		var cy = cursorY;
		var sx = cx == -1 ? 0 : cx;
		var sy = cy == -1 ? 0 : cy;
		fillRect(sx, sy, w, h);
		cursorX = sx + w;
		cursorY = sy + h;
	}


	public void drawRect(int x, int y, int w, int h) {
		if (w < 1 || h < 1)
			throw new IllegalArgumentException();

		recolor(doDrawRect(x, y, w, h, brush(), brushSize(), new LinkedHashSet<XY>()));
		fireDirtyListeners();
	}
	
	public boolean flashOn() {
		return flashOn;
	}

	public void flashOn(boolean flashOn) {
		if(!Objects.equals(flashOn, this.flashOn)) {
			this.flashOn = flashOn;
			pcs.firePropertyChange("flashOn", this.flashOn, flashOn);
		}
	}
	
//	public ObjectProperty<WritableImage> imageProperty() {
//		return image;
//	}
//
//	public WritableImage image() {
//		return image.get();
//	}

	public int ink() {
		return ink;
	}
	
	public void ink(int ink) {
		if(!Objects.equals(ink, this.ink)) {
			this.ink = ink;
			pcs.firePropertyChange("ink", this.ink, ink);
		}
	}

	public boolean inkOn() {
		return inkOn;
	}

	public void inkOn(boolean inkOn) {
		if(!Objects.equals(inkOn, this.inkOn)) {
			this.inkOn = inkOn;
			pcs.firePropertyChange("inkOn", this.inkOn, inkOn);
		}
	}

	public Palette palette() {
		return palette;
	}
	
	public int paper() {
		return paper;
	}

	public void paper(int paper) {
		if(!Objects.equals(paper, this.paper)) {
			this.paper = paper;
			pcs.firePropertyChange("paper", this.paper, paper);
		}
	}
	
	public boolean paperOn() {
		return paperOn;
	}

	public void paperOn(boolean paperOn) {
		if(!Objects.equals(paperOn, this.paperOn)) {
			this.paperOn = paperOn;
			pcs.firePropertyChange("paperOn", this.paperOn, paperOn);
		}
	}

	public void reset() {
		cursorX = -1;
		cursorY = -1;
	}

	public void buffer(VideoMemory buffer, Palette palette) {
		this.palette = palette;
		this.buffer = buffer;

		monochrome = palette.size() < 3;
		attributed = buffer instanceof AttributedVideoMemory;
		standardAttributed = buffer instanceof StandardAttributedVideoMemory;
		maskedAttributed = buffer instanceof MaskedAttributedVideoMemory;
		heightProperty= buffer.mode().height();
		widthProperty = buffer.mode().width();

		redraw();	
	}
	
	public void snapshot() {
		snapshot  = buffer().snapshot();
	}
	
	public void restore() {
		var sdata = snapshot.data();
		buffer().data().put(0, sdata, 0, sdata.limit());
		redraw();
	}

	public void redraw() {
		dirty(0, 0, width(), height(), true);
	}
	
	public int height() {		
		return heightProperty;
	}
	
	public int width() {
		return widthProperty;
	}

	private void dirty(int x, int y, boolean fireListeners) {
		dirty(x, y, 1, 1, fireListeners);
	}
	
	private void dirty(int x, int y, int width, int height, boolean fireListeners) {
		if(dirtyBounds == null) {
			dirtyBounds = new Rectangle(x, y, width, height);
			if(fireListeners)
				fireDirtyListeners();
		}
		else {
			var newBounds = dirtyBounds.union(new Rectangle(x, y, width, height));
			if(!Objects.equals(newBounds, dirtyBounds)) {
				dirtyBounds = newBounds;
				if(fireListeners)
					fireDirtyListeners();
			}
		}
			
	}

	protected void fireDirtyListeners() {
		dirtyListeners.forEach(l -> l.dirty(dirtyBounds));
		dirtyBounds = null;
	}
	
	
	private Set<XY> doDrawLine(int x1, int y1, int x2, int y2, Brush brush, int brushSize, Set<XY> cells) {
		int x, y;
		int dx, dy;
		int incx, incy;
		int balance;

		if (x2 >= x1) {
			dx = x2 - x1;
			incx = 1;
		} else {
			dx = x1 - x2;
			incx = -1;
		}

		if (y2 >= y1) {
			dy = y2 - y1;
			incy = 1;
		} else {
			dy = y1 - y2;
			incy = -1;
		}

		x = x1;
		y = y1;

		if (dx >= dy) {
			dy <<= 1;
			balance = dy - dx;
			dx <<= 1;
			while (x != x2) {
				stroke(x, y, brush, brushSize, cells);
				if (balance >= 0) {
					y += incy;
					balance -= dx;
				}
				balance += dy;
				x += incx;
			}
			stroke(x, y, brush, brushSize, cells);
		} else {
			dx <<= 1;
			balance = dx - dy;
			dy <<= 1;

			while (y != y2) {
				stroke(x, y, brush, brushSize, cells);
				if (balance >= 0) {
					x += incx;
					balance -= dy;
				}
				balance += dx;
				y += incy;
			}
			stroke(x, y, brush, brushSize, cells);
		}

		return cells;
	}

	private Set<XY> doFillCircle(int cx, int cy, int cr, Set<XY> cells) {
		double r = cr, x, y;
		double p0 = (1) - r;
		x = 0;
		y = r;
		while (x <= y) {
			doDrawLine((int) Math.round(x + r) + cx, (int) Math.round(y + r) + cy, (int) Math.round(-x + r) + cx,
					(int) Math.round(y + r) + cy, Brush.CIRCLE, 1, cells);
			doDrawLine((int) Math.round(y + r) + cx, (int) Math.round(x + r) + cy, (int) Math.round(-y + r) + cx,
					(int) Math.round(x + r) + cy, Brush.CIRCLE, 1, cells);
			doDrawLine((int) Math.round(-x + r) + cx, (int) Math.round(-y + r) + cy, (int) Math.round(x + r) + cx,
					(int) Math.round(-y + r) + cy, Brush.CIRCLE, 1, cells);
			doDrawLine((int) Math.round(-y + r) + cx, (int) Math.round(-x + r) + cy, (int) Math.round(y + r) + cx,
					(int) Math.round(-x + r) + cy, Brush.CIRCLE, 1, cells);
			if (p0 <= 0) {
				x++;
				p0 += 1 + (2 * x);
			} else {
				x++;
				y--;
				p0 += 1 + (2 * x) - (2 * y);
			}
		}
		return cells;
	}

	private Set<XY> doFillRect(int x, int y, int w, int h, Brush brush, int brushSize, Set<XY> cells) {
		for (int yy = y; yy < y + h; yy++) {
			for (int xx = x; xx < x + w; xx++) {
				stroke(xx, yy, brush, brushSize, cells);
			}
		}
		return cells;
	}

	private boolean inside(int inkCol, int x, int y) {
		var mode = this.buffer.mode();
		return x < mode.width() && y < mode.height() &&
			   x >= 0 && y >=0 && resolve(x, y) != inkCol;
	}

	private void paintFromBuffer(GC gc, int x, int y) {
		var buffer = this.buffer;
		var palette = this.palette;
		if(buffer instanceof AttributedVideoMemory attrBuffer) {
			var attr = attrBuffer.get(x, y);
			var p = attrBuffer.isSet(x, y);
			var ink =  attrBuffer.ink(x, y);
			if(attr == alpha()) {
				setPixel(gc, palette, x, y, alpha());
				if(p) {
					setPixel(gc, palette, x, y, ink);
				}
			}
			else {
				var paper = attrBuffer.paper(x, y);
				
				if(attrBuffer instanceof StandardAttributedVideoMemory sattrBuffer) {
					if(flashActive && sattrBuffer.isFlash(x, y)) {
						if(p) {
							setPixel(gc, palette, x, y, paper);
						}
						else {
							setPixel(gc, palette, x, y, ink);
						}
					}
					else {
						if(p) {
							setPixel(gc, palette, x, y, ink);
						}
						else {
							setPixel(gc, palette, x, y, paper);
						}
					}
				}
				else {
					if(p) {
						setPixel(gc, palette, x, y, ink);
					}
					else {
						setPixel(gc, palette, x, y, paper);
					}					
				}
			}
		}
		else {
			setPixel(gc, palette, x, y, buffer.color(x, y));
		}
	}
	
	private void setPixel(GC gc, Palette palette, int x, int y, int color) {
		if(color == alpha())
			gc.setAlpha(0);
		var rgb = palette.color(color);
		gc.setForeground(Activator.getDefault().getColorCache().color(rgb));
		gc.drawPoint(x, y);
		if(color == alpha())
			gc.setAlpha(255);
	}

	private void plot(int x, int y, Set<XY> cells) {
		var buffer = this.buffer;
		if(x < 0 || x >= buffer.mode().width() || y < 0 || y >= buffer.mode().height())
			return;
		
		if(forceClear || clearOn) {
			if(buffer instanceof AttributedVideoMemory attrBuffer) {
				if(forceClear || inkOn()) {
					attrBuffer.clear(x, y);
				}
				if(forceClear || paperOn()) {
					attrBuffer.set(x, y, alpha());
				}
			}
			else {
				buffer.color(x, y, alpha());
				dirty(x, y, false);
				return;
			}
		}
		else { 

			if(buffer instanceof AttributedVideoMemory attrBuffer) {
				if(inkOn()) {
					attrBuffer.set(x, y);
					attrBuffer.ink(x, y, ink());
				}
				if(paperOn()) {
					attrBuffer.paper(x, y, paper());
				}
				if(buffer instanceof StandardAttributedVideoMemory sattrBuffer) {
					sattrBuffer.flash(x, y, flashOn());
				}
			}
			else {
				if(inkOn()) {
					buffer.color(x, y, ink());
				}
				else if(paperOn()) {
					buffer.color(x, y, alpha());
				}
				dirty(x, y, false);
				return;
			}
			
		}
		
		var attrBuffer = (AttributedVideoMemory)buffer;
		
		var c = x / attrBuffer.attributeCellWidth();
		var r = y / attrBuffer.attributeCellHeight();
		cells.add(new XY(c, r));
	}
	
	private void recolor(Set<XY> cells) {
		if(buffer instanceof AttributedVideoMemory abuffer) {
			var acw = abuffer.attributeCellWidth();
			var ach = abuffer.attributeCellHeight();
			for(var cell : cells) {
				for(var y = 0 ; y < ach ; y++) {
					for(var x = 0 ; x < acw ; x++) {
						dirty((cell.x * acw ) + x, ( cell.y * ach ) + y, false);
					}
				}
			}
		}
	}


	public void paint(GC gc) {
		paint(gc, 0, 0, width(), height());
	}
	

	public void paint(GC gc, int bx, int by, int bw, int bh) {
		System.out.println("Paint IN " + bx + ", " +by + " " + bw + " x" + bh);

		if(bx > width() || by > height())
			return;
		
		if(bx + bw > width()) {
			bw = Math.max(0, width() - bx);
		}
		if(by + bh > height()) {
			bh = Math.max(0, height() - by);
			
		}
		
		System.out.println("Paint " + bx + ", " +by + " " + bw + " x" + bh);
		for(int y = by ; y < by + bh; y++) {
			for(int x = bx ; x < bx + bw; x++) {
				paintFromBuffer(gc, x, y);
			}
		}
		
		reset();
	}

	public int resolve(int x, int y) {
		return this.buffer.color(x, y);
	}

	private void scan(int inkCol, int lx, int rx, int y, Stack<int[]> s) {
		var spanAdded = false;
		for(var x = lx ; x < rx; x++) {
			if(!inside(inkCol, x, y)) {
				spanAdded = false;
			}
			else if(!spanAdded) {
				s.add(new int[] {x,y});
				spanAdded = true;
			}
		}
	}

	private void stroke(int x, int y, Brush brush, int bs, Set<XY> cells) {
		if(brush == Brush.BLOCK) {
//			if(pixelReader != null) {
//				var img = blockImage.get();
//				var halfY = (int)img.getHeight() / 2;
//				var halfX = (int)img.getWidth() / 2;
//				var buf = buffer.get();
//				for(int py = 0 ; py < img.getHeight(); py++) {
//					for(int px = 0 ; px < img.getWidth(); px++) {
//						var pixel = RGB.ofArgb(pixelReader.getArgb(px, py));
//						var index = palette.get().indexOf(pixel); /* TODO .. slow .. get direct from SpriteImage? */
//						if(index == -1) {
////							System.out.println("No palette index for " + pixel);
//						}
//						else {
//						
//							// TODO only hi color for now?
//							//plot(x - halfX + px, y - halfY + py, cells);
//							var bx = x - halfX + px;
//							var by = y - halfY + py;
//							if(bx < 0 || bx >= buf.mode().width() || by < 0 || by >= buf.mode().height()) {
//								System.out.println("Out of bounds for " + bx + ", " +by);
//							}
//							else {
//								if(pixel.a() < 0x7f) {
//									buf.color(bx, by, alpha());
//								}
//								else {
//									buf.color(bx, by, index);
//								}
//								dirty(bx, by);
//							}
//						}
//					}
//				}
//			}			
		}
		else {
		
			if(bs == 1)
				plot(x, y, cells);
			else {
				var half = bs / 2;
				switch(brush) {
				case SPRAY:
					var dots= (int)(( (double)( bs * bs ) / 100.0 ) * Math.min(100, Math.max(1, sprayDensity())));
	//				var dots = Math.max(1, ( 100 / ( bs * bs ) * Math.min(100, Math.max(1, sprayDensity())))); 
					for(var c = 0 ; c < dots ; c++) {
						plot((int)(x - half + ( bs * Math.random())), (int)(y - half + (bs * Math.random() )), cells);
					}
					break;
				case CIRCLE:
					doFillCircle(x - half, y - half, half, cells);
					break;
				default:
					doFillRect(x - half, y - half, bs, bs, Brush.CIRCLE, 1, cells);
					break;
				}
			}
		}
	}
	

	private Set<XY> doDrawRect(int x, int y, int w, int h, Brush brush, int brushSize, Set<XY> cells) {
		doDrawLine(x, y, x + w - 1, y, brush, brushSize, cells);
		doDrawLine(x + w - 1, y, x + w - 1, y + h - 1, brush, brushSize, cells);
		doDrawLine(x + w - 1, y + h - 1, x, y + h - 1, brush, brushSize, cells);
		doDrawLine(x, y + h - 1, x, y, brush, brushSize, cells);
		return cells;
	}
	
//	private static WritableImage createImage(VideoMemory buffer) {
//		return new WritableImage(buffer instanceof AttributedVideoMemory ? buffer.mode().width() * 2: buffer.mode().width(), buffer.mode().height());
//	}
//	
//	private void blockImageChanged() {
//		if(blockImage.get() == null) {
//			pixelReader = null;
//		}
//		else {
//			pixelReader = blockImage.get().getPixelReader();
//		}
//	}
}
