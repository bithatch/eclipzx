package uk.co.bithatch.drawzx.sprites;

import uk.co.bithatch.zyxy.graphics.Palette;

public class Sprite {

	private final Palette palette;
	private final int[][] data;
	private int xoffset;
	private int cols;
	private int rows;
	private int cellSize;
	private final SpriteCell[][] cells;
	
	public Sprite() {
		this(2, 2);
	}

	public Sprite(int cols, int rows) {
		this(Palette.mono(), cols, rows);
	}

	public Sprite(Palette palette, int cols, int rows) {
		this(palette, 0, cols, rows);
	}
	
	public Sprite(Palette palette, int xoffset, int cols, int rows) {
		this(palette, xoffset, cols, rows, 8);
	}

	public Sprite(Palette palette, int xoffset, int cols, int rows, int cellSize) {
		this(palette, xoffset, cols, rows, cellSize, new int[rows * cellSize][cols * cellSize]);
	}
	
	public Sprite(Palette palette, int xoffset, int cols, int rows, int cellSize, int[][] data) {
		super();
		this.palette = palette;
		this.data = data;
		this.xoffset = xoffset;
		this.cellSize = cellSize;
		this.rows = rows;
		this.cols = cols;
		
		cells = new SpriteCell[rows][cols];
		for(var r = 0 ; r < rows ; r++) {
			for(var c = 0 ; c < cols; c++) {
				var cellXOffset = xoffset + (c * cellSize) + (r * cols * cellSize);
				cells[r][c] = new SpriteCell(palette, cellXOffset, cellSize, data);
			}
		}
	}
	
	public int xoffset() {
		return xoffset;
	}

	public Palette getPalette() {
		return palette;
	}
	
	public int cellSize() {
		return cellSize;
	}
	
	public int cols() {
		return cols;
	}
	
	public int rows() {
		return rows;
	}
	
	public int[][] data() {
		return data;
	}
	
}
