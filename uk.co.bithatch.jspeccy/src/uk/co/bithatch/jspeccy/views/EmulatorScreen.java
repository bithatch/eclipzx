/*
 * JScreen.java
 *
 * Created on 15 de enero de 2008, 12:50
 * 
 * Cut down version that removes most filters and has fully dynamic resizing. Intended for use
 * in the EclipZX suite.
 */
package uk.co.bithatch.jspeccy.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.SwingUtilities;

import machine.Screen;
import machine.Spectrum;

/**
 *
 * @author jsanchez
 */
@SuppressWarnings("serial")
public class EmulatorScreen extends javax.swing.JComponent implements Screen {

	private BufferedImage tvImage;
	private BufferedImage tvPalImage;
	private Graphics2D tvPalImageGc;
	private int screenWidth, screenHeight;
	private boolean anyFilter = false;
	private boolean palFilter = false;
	private int[] imagePalBuffer;
	private int[] scanline1 = new int[256];
	private int[] scanline2 = new int[256];

	private int tableYUV[][] = new int[3][32];
	private static final int Yuv = 0;
	private static final int yUv = 1;
	private static final int yuV = 2;

	private final static int SCREEN_WIDTH = 256;
	private final static int SCREEN_HEIGHT = 192;

	private boolean borderEnabled;
	private boolean borderExtend;
	private Color borderColor = Color.WHITE;

	/** Creates new form JScreen */
	public EmulatorScreen() {
		initComponents();

		screenWidth = SCREEN_WIDTH;
		screenHeight = SCREEN_HEIGHT;

		Dimension screenSize = new Dimension(screenWidth, screenHeight);
		setPreferredSize(screenSize);

		tvPalImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
		tvPalImageGc = tvPalImage.createGraphics();
		imagePalBuffer = ((DataBufferInt) tvPalImage.getRaster().getDataBuffer()).getBankData()[0];

		scanline1[0] = scanline2[0] = 0; // scanline3 [0] = 0;
		for (int color = 1; color < scanline1.length; color++) {
			scanline1[color] = (int) (color * 0.80f);
			scanline2[color] = (int) (color * 0.70f);
		}

		int yuv[] = new int[3];
		for (int color = 0; color < Spectrum.Paleta.length; color++) {
			int rgb = Spectrum.Paleta[color];
			rgb2yuv(rgb, yuv);
			rgb %= 31;
			tableYUV[Yuv][rgb] = yuv[0];
			tableYUV[yUv][rgb] = yuv[1];
			tableYUV[yuV][rgb] = yuv[2];
		}
	}

	public boolean isBorderExtend() {
		return borderExtend;
	}

	public void setBorderExtend(boolean borderExtend) {
		if (borderExtend != this.borderExtend) {
			this.borderExtend = borderExtend;
			repaint();
		}
	}

	public void setTvImage(BufferedImage bImage) {
		tvImage = bImage;
	}

	public void setBorderEnabled(boolean border) {
		if (borderEnabled == border)
			return;

		borderEnabled = border;

		screenWidth = SCREEN_WIDTH;
		screenHeight = SCREEN_HEIGHT;

		if (borderEnabled) {
			screenWidth += 64;
			screenHeight += 48;
		}

		tvPalImage = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
		if (tvPalImageGc != null)
			tvPalImageGc.dispose();
		tvPalImageGc = tvPalImage.createGraphics();
		imagePalBuffer = ((DataBufferInt) tvPalImage.getRaster().getDataBuffer()).getBankData()[0];

		Dimension screenSize = new Dimension(screenWidth, screenHeight);
		setPreferredSize(screenSize);
	}

	public boolean isBorderEnabled() {
		return borderEnabled;
	}

	@Override
	public void repaintScreen(Rectangle area) {
//    	System.out.println("RSCR " +  area + " : " + Thread.currentThread().getName());
		if (area == null)
			repaint();
		else {
			var asz = calcAvailableSize();
			var r = calcResizeRatio(asz);
			var off = calcResizedImageOffset(asz);
			repaint((int) ((float) area.x * r) + off.x, (int) ((float) area.y * r) + off.y,
					(int) ((float) area.width * r), (int) ((float) area.height * r));
		}
	}

	@Override
	public void borderUpdated(int border) {
		borderColor = new Color(border);
		if (borderExtend) {
			SwingUtilities.invokeLater(this::repaint);
		}
	}

	@Override
	public void paintComponent(Graphics gc) {
		super.paintComponent(gc);

		if (borderExtend) {
			gc.setColor(borderColor);
			gc.fillRect(0, 0, getSize().width, getSize().height);
		}

		var gc2 = (Graphics2D) gc;
		var asz = calcAvailableSize();

		if (asz.width == 0 || asz.height == 0)
			return;

		var rsz = calcResizedImageSize(asz);
		var off = calcResizedImageOffset(asz);

		if (palFilter) {
			tvPalImageGc.drawImage(tvImage, 0, 0, null);
			palFilterYUV();
			gc2.drawImage(tvPalImage, off.x, off.y, off.x + rsz.width, off.y + rsz.height, 0, 0, screenWidth,
					screenHeight, null);
		} else {
			gc2.drawImage(tvImage, off.x, off.y, off.x + rsz.width, off.y + rsz.height, 0, 0, screenWidth, screenHeight,
					null);
		}
	}

	protected Point calcResizedImageOffset() {
		return calcResizedImageOffset(calcAvailableSize());
	}

	protected Point calcResizedImageOffset(Dimension asz) {
		Dimension rsz = calcResizedImageSize(asz);
		int ox = (asz.width - rsz.width) / 2;
		int oy = (asz.height - rsz.height) / 2;
		return new Point(ox, oy);
	}

	protected Dimension calcResizedImageSize(Dimension asz) {
		return calcResizedImageSize(calcResizeRatio(asz));
	}

	protected Dimension calcResizedImageSize(float r) {
		int ax = (int) ((float) screenWidth * r);
		int ay = (int) ((float) screenHeight * r);
		return new Dimension(ax, ay);
	}

	protected Dimension calcAvailableSize() {
		Dimension sz = getSize();
		int vw = sz.width;
		int vh = sz.height;
		return new Dimension(Math.max(0, vw), Math.max(0, vh));
	}

	protected float calcResizeRatio(Dimension asz) {

		float rx = (float) asz.width / (float) screenWidth;
		float ry = (float) asz.height / (float) screenHeight;
		float r = Math.min(rx, ry);
		return r;
	}

	/*
	 * Equations from ITU.BT-601 Y'CbCr Y = R * .299000 + G * .587000 + B * .114000
	 * U = R * -.168736 + G * -.331264 + B * .500000 + 128 V = R * .500000 + G *
	 * -.418688 + B * -.081312 + 128 or U = (B - Y) * 0.565 + 128 V = (R - Y) *
	 * 0.713 + 128
	 * 
	 * http://www.fourcc.org/fccyvrgb.php
	 * http://softpixel.com/~cwright/programming/colorspace/
	 */
	private void rgb2yuv(int rgb, int[] yuv) {
		int r = rgb >>> 16;
//        int g = ((rgb >>> 8) & 0xff);
		int b = rgb & 0xff;

		int y = (int) (0.299 * r + 0.587 * ((rgb >>> 8) & 0xff) + 0.114 * b);
//        int u = (int)(r * -.168736 + g * -.331264 + b *  0.5);
//        int v = (int)(r * 0.5 + g * -0.418688 + b * -0.081312);
		int u = (int) ((b - y) * 0.565);
		int v = (int) ((r - y) * 0.713);

		yuv[0] = y;
		yuv[1] = u + 128;
		yuv[2] = v + 128;
	}

	/*
	 * Inverse equations of rgb2yuv R = Y + 1.4075 (V - 128) G = Y - 0.3455 (U -
	 * 128) - 0.7169 (V - 128) B = Y + 1.7790 (U - 128)
	 * 
	 * Inverse equation from JPEG/JFIF R = Y + 1.402 (V - 128) G = Y - 0.34414 (U -
	 * 128) - 0.71414 (V - 128) B = Y + 1.772 (U - 128)
	 */
	private int yuv2rgb(int y, int u, int v) {
		u -= 128;
		v -= 128;
//        int r = (int)(y + 1.4075 * v);
		int r = (int) (y + 1.402 * v);
		if (r < 0) {
			r = 0;
		}
		if (r > 255) {
			r = 255;
		}

//        int g = (int)(y - 0.3455 * u - 0.7169 * v);
		int g = (int) (y - 0.34414 * u - 0.71414 * v);
		if (g < 0) {
			g = 0;
		}
		if (g > 255) {
			g = 255;
		}

//        int b = (int)(y + 1.7790 * u);
		int b = (int) (y + 1.772 * u);
		if (b < 0) {
			b = 0;
		}
		if (b > 255) {
			b = 255;
		}

		return (r << 16) | (g << 8) | b;
	}

	private void palFilterYUV() {

//        long ini = System.currentTimeMillis();

		int idx1, idx2, idx3;

		int start = SCREEN_WIDTH;
		int limit = SCREEN_HEIGHT * SCREEN_WIDTH - 1;
		int pixel = 0;

		while (pixel < limit) {
			pixel = start;
			idx1 = imagePalBuffer[pixel++] % 31;
			idx2 = imagePalBuffer[pixel] % 31;
			for (int col = 0; col < 254; col++) {
				idx3 = imagePalBuffer[pixel + 1] % 31;
				imagePalBuffer[pixel++] = yuv2rgb(tableYUV[Yuv][idx2],
						(tableYUV[yUv][idx1] + 2 * tableYUV[yUv][idx2] + tableYUV[yUv][idx3]) >>> 2,
						(tableYUV[yuV][idx1] + 2 * tableYUV[yuV][idx2] + tableYUV[yuV][idx3]) >>> 2);
				idx1 = idx2;
				idx2 = idx3;
			}
			start += SCREEN_WIDTH;
		}

//        System.out.println(String.format("PalFilterYUV in %d ms.", System.currentTimeMillis() - ini));
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		setDoubleBuffered(false);
	}// </editor-fold>//GEN-END:initComponents
		// Variables declaration - do not modify//GEN-BEGIN:variables
		// End of variables declaration//GEN-END:variables

	/**
	 * @return the palFilter
	 */
	public boolean isPalFilter() {
		return palFilter;
	}

	/**
	 * @param palFilter the palFilter to set
	 */
	public void setPalFilter(boolean palFilter) {
		this.palFilter = palFilter;

		if (palFilter) {
			anyFilter = true;
		}

	}

	/**
	 * @return the anyFilter
	 */
	public boolean isAnyFilter() {
		return anyFilter;
	}

	/**
	 * @param anyFilter the anyFilter to set
	 */
	public void setAnyFilter(boolean anyFilter) {
		this.anyFilter = anyFilter;
		if (!anyFilter) {
			palFilter = false;
		}
	}

	@Override
	public int getZoom() {
		return 1;
	}
}
