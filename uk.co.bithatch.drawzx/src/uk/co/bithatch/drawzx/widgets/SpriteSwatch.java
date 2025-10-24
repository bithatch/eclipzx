package uk.co.bithatch.drawzx.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import uk.co.bithatch.drawzx.sprites.SpriteSheet;
import uk.co.bithatch.drawzx.widgets.AbstractSpriteGrid.BackgroundType;

public class SpriteSwatch extends Composite {

	private int cellSize;
	private SpriteSheet spriteSheet;
	private int columns;
	private List<SpriteGrid> spriteCells = new  ArrayList<>();
	private int selected = -1;
	private int spacing = 3;
	private BackgroundType backgroundType = BackgroundType.LARGE_CHEQUER;

	public SpriteSwatch(Composite parent) {
		this(parent, SWT.NONE);
	}

	public SpriteSwatch(Composite parent, int style) {
		this(parent, SpriteSheet.udgSheet(), 48, style);
	}

	public SpriteSwatch(Composite parent, SpriteSheet spriteSheet, int columns, int style) {
		this(parent, spriteSheet, 4, columns, style);
	}

	public SpriteSwatch(Composite parent, SpriteSheet spriteSheet, int cellSize,  int columns, int style) {
		super(parent, style);
		this.columns = columns;
		this.cellSize = cellSize;
		spriteSheet(spriteSheet);
		select(0);
	}

	public void addSelectionListener(SelectionListener listener) {
		addTypedListener(listener, SWT.Selection, SWT.DefaultSelection);
	}

	public void removeSelectionListener(SelectionListener listener) {
		removeListener(SWT.Selection, listener);
		removeListener(SWT.DefaultSelection, listener);
	}

	public int selected() {
		return selected;
	}

	public void selected(int selected) {
		select(selected);
	}

	@Override
	public void dispose() {
		super.dispose();
	}


	public BackgroundType backgroundType() {
		return backgroundType;
	}

	public void backgroundType(BackgroundType backgroundType) {
		if(backgroundType != this.backgroundType) {
			this.backgroundType = backgroundType;
			rebuildAndRedraw();
		}
	}

	public int spacing() {
		return spacing;
	}

	public void spacing(int spacing) {
		if(spacing != this.spacing) {
			this.spacing = spacing;
			rebuildAndRedraw();
		}
	}

	public void spriteSheet(SpriteSheet spriteSheet) {
		if (!Objects.equals(spriteSheet, this.spriteSheet)) {
			this.spriteSheet = spriteSheet;
			rebuildAndRedraw();
		}
	}

	private void rebuildAndRedraw() {
		System.out.println("rebuildAndRedraw()");
		spriteCells.forEach(SpriteGrid::dispose);
		spriteCells.clear();
		var sz = this.spriteSheet.size();
		var rows = ( sz + columns - 1) / columns;
		var layout = new GridLayout(columns, true);
		layout.horizontalSpacing = layout.verticalSpacing = spacing;
		setLayout(layout);
		for(var r = 0 ; r < rows; r++) {
			for(var c = 0 ; c < columns ; c++) {
				var index = (r * columns) + c;
				var spriteGrid = new SpriteGrid(this, spriteSheet.cell(index), SWT.NONE);
				spriteGrid.setSelection(spriteCells.size() == selected);
				spriteGrid.backgroundType(backgroundType);
				spriteGrid.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
					select(index);
				}));
				spriteGrid.setLayoutData(GridDataFactory.create(SWT.NONE).grab(true, false).hint(cellSize, cellSize).create());
				spriteCells.add(spriteGrid);
			}
		}
		
		requestLayout();
	}
	
	private void select(int selected) {
		if(this.selected != selected) {
			this.selected = selected;
			var selCell = spriteCells.get(selected);
			spriteCells.forEach(c -> c.setSelection(selCell == c));
			redraw();
			
			var ue = new Event();
			ue.widget = this;
			ue.display = getDisplay();
			ue.doit = true;
			ue.data = selCell.spriteCell();
			var e = new SelectionEvent(ue);
			getTypedListeners(SWT.Selection, SelectionListener.class).forEach(l -> l.widgetSelected(e));
		}
	}


}
