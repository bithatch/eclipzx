package uk.co.bithatch.drawzx.widgets;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

public class ColorCell extends Canvas {

	private Color bg;
	private boolean selected;
	private Color selectedColor;
	private boolean selectOnClick = true;
	private RGBA color;

	public ColorCell(Composite parent,  int size) {
		this(parent, size, new RGBA(0xff,0xff,0xff, 0xff));
	}

	public ColorCell(Composite parent,  int size, RGB color) {
		this(parent, size, toRGBA(color));
	}

	private static RGBA toRGBA(RGB color) {
		return new RGBA(color.red, color.green, color.blue, 0xff);
	}
	
	public ColorCell(Composite parent,  int size, RGBA color) {
		super(parent, SWT.DOUBLE_BUFFERED);
        setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        addPaintListener(new ZoomablePaintListener());
        addMouseListener(new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent e) {
			}
			
			@Override
			public void mouseDown(MouseEvent e) {
				
				if(selectOnClick) {
					selected = true;
					redraw();
				}
				
				var evt = selectionEventFromMouseEvent(e);
				evt.data = bg;
				getTypedListeners(SWT.Selection, SelectionListener.class).forEach(l -> l.widgetSelected(evt));
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent e) {

				if(selectOnClick) {
					selected = true;
					redraw();
				}
				
				var evt = selectionEventFromMouseEvent(e);
				evt.data = bg;
				getTypedListeners(SWT.Selection, SelectionListener.class).forEach(l -> l.widgetSelected(evt));
			}
		});
        setSize(size, size);
        setColor(color);
        selectedColor = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
	}
	
	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		if(this.selected != selected) {
			this.selected = selected;
			var evt = new Event();
			evt.widget = this;
			evt.display = getDisplay();
			evt.doit = true;
			evt.data = bg;
			var sel = new SelectionEvent(evt);
			getTypedListeners(SWT.Selection, SelectionListener.class).forEach(l -> l.widgetDefaultSelected(sel));
			
			redraw();
		}
	}

	public void addSelectionListener (SelectionListener listener) {
		addTypedListener(listener, SWT.Selection, SWT.DefaultSelection);
	}
	
	public void removeSelectionListener (SelectionListener listener) {
		removeListener(SWT.Selection, listener);
		removeListener(SWT.DefaultSelection, listener);
	}

	public RGBA getColor() {
		return color;
	}

	public void setColor(RGB color) {
		setColor(toRGBA(color));
	}
	
	public void setColor(RGBA color) {
		this.color = color;
		if(bg != null)
			bg.dispose();
		bg = new Color(getDisplay(), color);
		redraw();
	}

    public boolean isSelectOnClick() {
		return selectOnClick;
	}

	public void setSelectOnClick(boolean selectOnClick) {
		this.selectOnClick = selectOnClick;
	}

	private class ZoomablePaintListener implements PaintListener {
        @Override
        public void paintControl(PaintEvent e) {
//            Transform transform = new Transform(e.gc.getDevice());
//            transform.scale(zoomLevel, zoomLevel);
//            e.gc.setTransform(transform);
//
//            // Example content
//            e.gc.drawRectangle(50, 50, 100, 100);
//            e.gc.drawText("Zoom Level: " + String.format("%.2f", zoomLevel), 60, 160);
        	
        	e.gc.setBackground(bg);
        	e.gc.fillRectangle(0, 0, getSize().x, getSize().y);
        	
        	if(selected) {
        		e.gc.setForeground(selectedColor);
        		e.gc.setLineWidth(Math.min(8, Math.min( getSize().x / 4, getSize().y / 4)));
        		e.gc.drawRectangle(0, 0, getSize().x - 1, getSize().y - 1);
        	}
//
//            transform.dispose();
        }
    }

	private SelectionEvent selectionEventFromMouseEvent(MouseEvent e) {
		var utyped = new Event();
		utyped.widget = e.widget;
		utyped.button = e.button;
		utyped.count = e.count;
		utyped.display = e.display;
		utyped.doit = true;
		utyped.stateMask = e.stateMask;
		utyped.time = e.time;
		utyped.x = e.x;
		utyped.y = e.y;
		var evt = new SelectionEvent(utyped);
		evt.detail = e.count;
		return evt;
	}
}
