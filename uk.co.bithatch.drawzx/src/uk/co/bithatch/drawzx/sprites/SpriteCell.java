package uk.co.bithatch.drawzx.sprites;

import uk.co.bithatch.zyxy.graphics.Palette;
import uk.co.bithatch.zyxy.graphics.Palette.Entry;

public class SpriteCell {

	private final Palette palette;
	private final int[][] data;
	private final int xoffset;
	private final int size;
	
	public SpriteCell() {
		this(Palette.mono(), 0, 8, new int[8][8]);
	}
	
	public SpriteCell(Palette palette, int xoffset, int size, int[][] data) {
		super();
		this.palette = palette;
		this.data = data;
		this.xoffset = xoffset;
		this.size = size;
	}
	
	public int xoffset() {
		return xoffset;
	}

	public Palette palette() {
		return palette;
	}
	
	public int size() {
		return size;
	}
	
	public int[][] data() {
		return data;
	}

	public Palette.Entry color(int x, int y) {
		return palette.color(index(x, y));
	}

	public int index(int x, int y) {
		return data[y][x + xoffset];
	}

	public int index(int x, int y, int index) {
		var was = index(x,y);
		data[y][x + xoffset] = index;
		return was;
	}

	public int inverseIndex(int x, int y, int max) {
		return max - 1 - index(x, y);
	}

	public Palette.Entry inverse(int x, int y, int max) {
		return palette.color(inverseIndex(x, y, max));
	}

	public Palette.Entry color(int x, int y, Palette.Entry newColor) {
		var was = color(x, y);
		int indexOf = palette.indexOf(newColor);
		index(x, y, indexOf);
		return was;
	}
	

	public void mirrorH() {
		for(var y = 0 ; y < size; y++) {
			for(var x = 0 ; x < size / 2 ; x++) {
				index(size -x - 1, y, index(x, y, index(size - x - 1, y)));
			}
		}
	}

	public void mirrorV() {
		for(var x = 0 ; x < size ; x++) {
			for(var y = 0 ; y < size/ 2 ; y++) {
				index(x, size -y - 1, index(x, y, index(x, size - y - 1)));
			}
		}
	}

	public void invert(int max) {
		for(var x = 0 ; x < size ; x++) {
			for(var y = 0 ; y < size ; y++) {
				index(x, y, inverseIndex(x, y, max));
			}
		}
	}

	public void shift(int h, int v) {
		var tmp = new int[size];
		if(h != 0) {
			for(var y = 0 ; y < size; y++) {
				System.arraycopy(data[y], xoffset, tmp, 0, size);
				for(var x = 0 ; x < size ; x++) {
					data[y][xoffset + x] = tmp[roll(x + (h*-1), size)]; 
				}
			}
		}

		if(v != 0) {
			for(var x = 0 ; x < size; x++) {
				
				for(var y = 0 ; y < size ; y++)
					tmp[y] = data[y][xoffset + x];
				
				for(var y = 0 ; y < size ; y++) {
					int r = roll(y + (v*-1), size);
					data[y][xoffset + x] = tmp[r]; 
				}
			}
		}
	}

	public void rotate(int degrees) {
		// Transpose first
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                int tmp = data[i][j + xoffset];
                data[i][j + xoffset] = data[j][i + xoffset];
                data[j][i + xoffset] = tmp;
            }
        }

        if (degrees == 90) {
            // Reverse each row (clockwise)
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size / 2; j++) {
                    int tmp = data[i][j + xoffset];
                    data[i][j + xoffset] = data[i][size - 1 - j + xoffset];
                    data[i][size - 1 - j + xoffset] = tmp;
                }
            }
        } else if (degrees == -90) {
            // Reverse each column (counter-clockwise)
            for (int j = 0; j < size; j++) {
                for (int i = 0; i < size / 2; i++) {
                    int tmp = data[i][j + xoffset];
                    data[i][j + xoffset] = data[size - 1 - i][j + xoffset];
                    data[size - 1 - i][j + xoffset] = tmp;
                }
            }
        } else {
            throw new IllegalArgumentException("Only 90 and -90 degrees are supported.");
        }
		
	}
	
	private static int roll(int v, int sz) {
		if(v < 0) {
			return sz + -(-v % sz);
		}
		else {
			return v % sz;
		}
	}

	public void clear(Entry color) {
		clear(palette.indexOf(color));
	}
	
	public void clear(int index) {
		for(var x = 0 ; x < size ; x++) {
			for(var y = 0 ; y < size ; y++) {
				index(x, y, index);
			}
		}
	}

	public SpriteCell withPalette(Palette palette) {
		return new SpriteCell(palette, xoffset, size, data);
	}
	
}
