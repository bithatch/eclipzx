package uk.co.bithatch.drawzx.widgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import uk.co.bithatch.drawzx.Colors;
import uk.co.bithatch.zyxy.graphics.Palette;
import uk.co.bithatch.zyxy.graphics.Palette.Entry;

public class PaletteGrid extends Canvas {

	private static final int DEFAULT_CELL_SIZE = 16;
	private int cellSize;
	private List<Color> colors = new ArrayList<>();
	private int columns;
	private int gridX;
	private Palette palette;
	private LinkedHashSet<Integer> selected = new LinkedHashSet<>();
	private Color selectedColor;
	private int potentialDropTarget = -1;
	private Color borderColor;
	private Color fgColor;
	private Color bgColor;
	private boolean border;
	private Runnable picking;
	private Consumer<Integer> picker;
	private boolean showOffsets;
	private int offset;

	public PaletteGrid(Composite parent, int style) {
		this(parent, DEFAULT_CELL_SIZE, style);
	}

	public PaletteGrid(Composite parent, int cellSize, int style) {
		this(parent, Palette.rgb333(), cellSize, style);
	}

	public PaletteGrid(Composite parent, Palette palette, int style) {
		this(parent, palette, 16, style);
	}

	public PaletteGrid(Composite parent, Palette palette, int cellSize, int style) {
		this(parent, palette, cellSize, 0, style);
	}

	public PaletteGrid(Composite parent, Palette palette, int cellSize, int gridWidth, int style) {
		super(parent, style);
		this.columns = gridWidth;
		this.cellSize = cellSize;
		border = (style & SWT.BORDER) != 0;
		selectedColor = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		borderColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER);
		fgColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
		bgColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

		palette(palette);
		addPaintListener(e -> drawTile(e.gc));
		addListener(SWT.MouseMove, event -> {
			updateCellTooltip(indexAt(event.x, event.y));
		});
		addListener(SWT.MouseHover, event -> {
			updateCellTooltip(indexAt(event.x, event.y));
		});
		addListener(SWT.MouseDown, event -> {
			select(event);
		});
	}

	public Palette palette() {
		return palette;
	}

	public void cellSize(int cellSize) {
		if(cellSize != this.cellSize) {
			this.cellSize = cellSize;
			rebuild();
		}
	}

	public void paletteOffset(int offset) {
		if(offset != this.offset) {
			var newSel = new LinkedHashSet<>(selected);
			this.offset = offset;
			if(newSel.size() > 0) {
				select(newSel.stream().map(sel -> (offset * 16 ) + ( sel % 16)).toList());
			}
			else {
				redraw();
			}
		}
	}
	
	public boolean showOffsets() {
		return showOffsets;
	}

	public void showOffsets(boolean showOffsets) {
		if(showOffsets != this.showOffsets) {
			this.showOffsets = showOffsets;
			rebuild();
		}
	}

	public Runnable pickColor(Consumer<Integer> onPick) {
		if(picking != null)
			throw new IllegalStateException("Already picking colour.");
		
		this.picker = onPick;
		setCursor(getDisplay().getSystemCursor(SWT.CURSOR_CROSS));
		
		return picking = () -> {
			endPick(true);
		};
	}

	public void addModifyListener(ModifyListener listener) {
		addTypedListener(listener, SWT.Modify);
	}

	public void removeModifyListener(ModifyListener listener) {
		removeListener(SWT.Modify, listener);
	}

	public void addSelectionListener(SelectionListener listener) {
		addTypedListener(listener, SWT.Selection, SWT.DefaultSelection);
	}

	@Override
	public void dispose() {
		colors.forEach(Color::dispose);
		super.dispose();
	}

	public void removeSelectionListener(SelectionListener listener) {
		removeListener(SWT.Selection, listener);
		removeListener(SWT.DefaultSelection, listener);
	}


	public void select(int index) {
		select(index, false);
	}
	
	public void select(int index, boolean secondary) {
		select(index, null, secondary);
	}

	public void select(Collection<Integer> indexes) {
		selected.clear();
		for(var i : indexes) {
			selected.add(i);
		}
		redraw();
		var it = indexes.iterator();
		if(it.hasNext()) {
			fireSelect(it.next(), null, false);
			if(it.hasNext())
				fireSelect(it.next(), null, true);
		}
	}
	
	public void select(int... indexes) {
		selected.clear();
		for(var i : indexes) {
			selected.add(i);
		}
		redraw();
		fireSelect(indexes[0], null, false);
		if(indexes.length > 1)
			fireSelect(indexes[1], null, true);
	}

	public void asDragSource() {

		var operations = /* DND.DROP_MOVE | */ DND.DROP_COPY;
		var source = new DragSource(this, operations);

		var types = new Transfer[] { TextTransfer.getInstance() };
		source.setTransfer(types);

		source.addDragListener(new DragSourceListener() {

			private int draggingIndex = -1;

			@Override
			public void dragFinished(DragSourceEvent event) {
				draggingIndex = -1;
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				if (TextTransfer.getInstance().isSupportedType(event.dataType) && draggingIndex != -1) {
					event.data = palette.color(draggingIndex).toWeb();
				}
			}

			@Override
			public void dragStart(DragSourceEvent event) {
				draggingIndex = indexAt(event.x, event.y);
			}
		});
	}

	public void asDropTarget() {

		var operations = DND.DROP_COPY | DND.DROP_DEFAULT;
		var target = new DropTarget(this, operations);

		var textTransfer = TextTransfer.getInstance();
		var types = new Transfer[] { textTransfer };
		target.setTransfer(types);

		target.addDropListener(new DropTargetListener() {

			@Override
			public void dragEnter(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT) {
					if ((event.operations & DND.DROP_COPY) != 0) {
						event.detail = DND.DROP_COPY;
					} else {
						event.detail = DND.DROP_NONE;
					}
				}
			}

			@Override
			public void dragLeave(DropTargetEvent event) {
			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT) {
					if ((event.operations & DND.DROP_COPY) != 0) {
						event.detail = DND.DROP_COPY;
					} else {
						event.detail = DND.DROP_NONE;
					}
				}
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
				var rel = getDisplay().map(null, PaletteGrid.this, event.x, event.y);
				var targetted = indexAt(rel.x, rel.y);
				if(targetted != potentialDropTarget) {
					potentialDropTarget = targetted;
					redraw();
				}
			}

			@Override
			public void drop(DropTargetEvent event) {
				if (textTransfer.isSupportedType(event.currentDataType)) {
					String text = (String) event.data;
					var entry = Entry.ofWeb(text);
					
					var ue = new Event();
					ue.widget = PaletteGrid.this;
					ue.display = getDisplay();
					ue.doit = true;
					ue.data = potentialDropTarget;
					ue.count = 1;
					var e = new ModifyEvent(ue);
					e.data = new Object[] { potentialDropTarget, entry}; 
					getTypedListeners(SWT.Modify, ModifyListener.class).forEach(l -> l.modifyText(e));
				}
				potentialDropTarget = -1;
				redraw();
			}

			@Override
			public void dropAccept(DropTargetEvent event) {
			}
		});
	}

	public void entry(int index, Entry entry) {
		colors.set(index, Colors.toColor(entry, getDisplay()));
		redraw();
	}

	public void palette(Palette palette) {
		if (!Objects.equals(palette, this.palette)) {
			this.palette = palette;
			rebuild();
		}
	}

	public int columns() {
		return columns;
	}

	public void columns(int columns) {
		if(columns != this.columns) {
			this.columns = columns;
			rebuild();
		}
	}

	public int selectedIndex() {
		return selected.isEmpty() ? -1 : selected.iterator().next();
	}

	public Set<Integer> selectedIndices() {
		return selected;
	}

	private void select(int index, Event base, boolean secondary) {
		if(selected.contains(index) && selected.size() == 1 && !secondary)
			return;
		
		var psel = secondary ? selected.isEmpty() ? -1  : selected.getFirst() : index;
		var ssel = secondary ? index : selected.size() < 2 ? -1 : selected.getLast();
		
		selected.clear();
		if(psel > -1)
			selected.add(psel);
		if(ssel > -1)
			selected.add(ssel);
		
		redraw();
		fireSelect(index, base, secondary);
	}
		

	private void fireSelect(int index, Event base, boolean secondary) {
		var ue = new Event();
		ue.widget = this;
		ue.display = getDisplay();
		ue.doit = true;
		ue.data = index;
		ue.count = base == null ? 1 : base.count;
		var e = new SelectionEvent(ue);
		e.stateMask = base == null ? (secondary ? SWT.CTRL : 0) : base.stateMask;
		e.detail = base == null ? 1 : base.count;
		getTypedListeners(SWT.Selection, SelectionListener.class).forEach(l -> l.widgetSelected(e));
	}

	private void rebuild() {
		colors.forEach(Color::dispose);
		colors.clear();
		for (var rgb : palette.colors()) {
			colors.add(Colors.toColor(rgb, getDisplay()));
		}
		gridX = columns > 0 ? columns : ( palette.size() == 2 ? 2 : (int) (Math.sqrt(palette.size())));
		
		var tw = cellSize * gridX;
		var th = cellSize * calcRows();
		setSize(tw, th);
		
		requestLayout();
	}

	private int calcOffsetX() {
		return (calcIndent() + getClientArea().width - (cellSize() * gridX)) / 2;
	}

	private int calcOffsetY() {
		return (getClientArea().height - ((cellSize() * calcRows()) )) / 2;
	}

	private int calcRows() {
		return gridX == 0 ? 0 : (this.palette.size() + gridX - 1) / gridX;
	}
	
	private int calcIndent() {
		return showOffsets ? 16 : 0;
	}

	private int cellSize() {
		var bounds = getClientArea();
		bounds.width -= calcIndent();
		
		var rows = calcRows();
		var cellX = gridX == 0 ? 0 : bounds.width / gridX;
		var cellY = rows == 0 ? 0 :  bounds.height / rows;
		var cellSize = Math.min(cellX, cellY);
		return cellSize;
	}

	private void drawTile(GC gc) {

		var rows = calcRows();
		var cellSize = cellSize();
		var indent = calcIndent();
		var offsetX = calcOffsetX();
		var offsetY = calcOffsetY();
		var lineWidth = Math.min(8, cellSize / 4);

		var index = 0;
		for (var y = 0; y < rows; y++) {
			var yy = (y * cellSize) + offsetY;
			if(showOffsets) {
				gc.setBackground(bgColor);
				gc.setForeground(fgColor);
				if(y == offset) {
					gc.setAlpha(255);
				}
				else {
					gc.setAlpha(64);
				}
				
				gc.drawString(String.format("%1x", y), offsetX - indent, yy - 1);
			}
			for (var x = 0; x < gridX && index < palette.size(); x++) {
				var xx = (x * cellSize) + offsetX;
				var cellColor = colors.get(index);
				gc.setBackground(cellColor);
				gc.fillRectangle(xx, yy, cellSize, cellSize);
				
				var ptrans = palette.transparency().orElse(-1); 
				var trans = index;
				if(showOffsets) {
					trans = trans % 16;
					ptrans = ptrans % 16;
				}
				
				if(ptrans == trans) {
					if(borderColor.getRGB().equals(cellColor.getRGB()))
						gc.setForeground(fgColor);
					else
						gc.setForeground(borderColor);
					gc.drawLine(xx, yy, xx + cellSize, yy +  cellSize);
					gc.drawLine(xx, yy + cellSize, xx + cellSize, yy);
				}

				index++;
			}

			if(showOffsets) {
				gc.setAlpha(255);
			}
		}
		
		if(border) {
			gc.setForeground(borderColor);
			for (var y = 0; y < ( rows + 1 ); y++) {
				var ey = ( y * cellSize) + offsetY;
				gc.drawLine(offsetX, ey, offsetX + (gridX * cellSize), ey);
			}	
			for (var x = 0; x < ( gridX + 1 ) ; x++) {
				var ex = offsetX + (x * cellSize);
				gc.drawLine(ex, offsetY, ex, offsetY + (rows * cellSize));
			}
		}

		gc.setForeground(selectedColor);
		gc.setLineWidth(lineWidth);
		var primary = true;
		for (var sel : selected) {
			drawBox(gc, cellSize, offsetX, offsetY, sel, primary);
			primary = false;
		}
		if(potentialDropTarget != -1)
			drawBox(gc, cellSize, offsetX, offsetY, potentialDropTarget, false);
	}

	private void drawBox(GC gc, int cellSize, int offsetX, int offsetY, Integer sel, boolean primary) {
		var x = sel % gridX;
		var y = sel / gridX;
		var xx = (x * cellSize) + offsetX;
		var yy = (y * cellSize) + offsetY;
		if(!primary) {
			gc.setLineStyle(SWT.LINE_DASH);
		}
		gc.drawRectangle(xx, yy, cellSize - 1, cellSize - 1);
		if(!primary) {
			gc.setLineStyle(SWT.LINE_SOLID);
		}
	}

	private int indexAt(int x, int y) {
		var cellSize = cellSize();
		if(cellSize == 0)
			return -1;
		var col = (Math.max(0, x - calcOffsetX())) / cellSize;
		var row = (Math.max(0, (y - calcOffsetY()))) / cellSize;
		return Math.min(palette.size() - 1, (row * gridX) + col);
	}

	private void select(Event event) {
		select(indexAt(event.x, event.y), event, (event.stateMask & SWT.CTRL ) != 0);
		if(picker != null) {
			endPick(false);
		}
	}

	private void updateCellTooltip(int idx) {
		if(idx > -1) {
			var ent = palette.color(idx);
			setToolTipText(String.format("@ %d%n%s%n%s%n%s%n%s%n", idx, ent.toValues(), ent.encoded(), ent.toEncodedHex(),
					ent.toEncodedBinary(), ent.toWeb()));
		}
	}

	private void endPick(boolean cancelled) {
		try {
			if(!cancelled) {
				picker.accept(selectedIndex());
			}
		}
		finally {
			setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
			picking = null;
			picker = null;
		}
	}
}
