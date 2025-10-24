package uk.co.bithatch.drawzx.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import uk.co.bithatch.drawzx.sprites.SpriteCell;

public class SpriteGrid extends AbstractSpriteGrid {

	private static final int DEFAULT_CELL_SIZE = 16;
	
	private boolean selected;

	public SpriteGrid(Composite parent, int style) {
		this(parent, DEFAULT_CELL_SIZE, style);
	}

	public SpriteGrid(Composite parent, int cellSize, int style) {
		this(parent, new SpriteCell(), cellSize, style);
	}

	public SpriteGrid(Composite parent, SpriteCell spriteCell, int style) {
		this(parent, spriteCell, 4, style);
	}

	public SpriteGrid(Composite parent, SpriteCell spriteCell, int cellSize, int style) {
		super(parent, spriteCell, cellSize, style);
		addListener(SWT.MouseDown, event -> {

			selected = true;
			redraw();
			
			var ue = new Event();
			ue.widget = this;
			ue.display = getDisplay();
			ue.doit = true;
			ue.data = spriteCell();
			var e = new SelectionEvent(ue);
			getTypedListeners(SWT.Selection, SelectionListener.class).forEach(l -> l.widgetSelected(e));
		});
		setSize(cellSize, cellSize);
	}

	public void addSelectionListener(SelectionListener listener) {
		addTypedListener(listener, SWT.Selection, SWT.DefaultSelection);
	}

	public void removeSelectionListener(SelectionListener listener) {
		removeListener(SWT.Selection, listener);
		removeListener(SWT.DefaultSelection, listener);
	}
	
	public boolean getSelection() {
		return selected;
	}

	public void setSelection(boolean selected) {
		if(selected != this.selected) {
			this.selected = selected;
			redraw();
		}
	}
	

	protected void onDrawTileAfterBorder(GC g, int offsetX, int offsetY, int pixelSize) {
		if(selected) {
			var tsize = calPixelSize() * spriteCell().size();
			var lineWidth = Math.max(4, tsize / 8);
			g.setForeground(selectedColor);
			g.setLineWidth(lineWidth);
			g.drawRectangle(offsetX, offsetY, tsize, tsize);
		}
	}

}
