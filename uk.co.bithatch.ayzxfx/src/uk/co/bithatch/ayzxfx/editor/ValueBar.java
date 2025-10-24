package uk.co.bithatch.ayzxfx.editor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@SuppressWarnings("serial")
public class ValueBar extends JComponent {

	public static final String VALUE = "value";
	public static final String SELECTION = "selection";
	public static final String CANCELLED = "cancelled";
	private int value = 0;
	private int max = 100;
	private int selection = 0;
	private MouseAdapter mouse;
	private boolean cancelOnOutOfBoundsRelease;
	private boolean logarithmic;

	public ValueBar(int max, int initialValue) {
		this.max = Math.max(1, max);
		this.value = Math.min(Math.max(0, initialValue), max);

		mouse = new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				updateValueFromMouse(e.getX());
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				updateValueFromMouse(e.getX());
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				super.mouseMoved(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if(getBounds().contains(e.getPoint()) || !cancelOnOutOfBoundsRelease) {
					updateValueFromMouse(e.getX());
					var was = selection;
					if (was != value) {
						selection = value;
						firePropertyChange(SELECTION, was, value);
					}	
				}
				else {
					var was = value;
					value = selection;
					if(was != value)
						firePropertyChange(VALUE, was, value);
					firePropertyChange(CANCELLED, false, true);
				}
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		setFocusable(true);
		setOpaque(true);
	}

	public boolean isLogarithmic() {
		return logarithmic;
	}

	public void setLogarithmic(boolean logarithmic) {
		this.logarithmic = logarithmic;
	}

	public boolean isCancelOnOutOfBoundsRelease() {
		return cancelOnOutOfBoundsRelease;
	}

	public void setCancelOnOutOfBoundsRelease(boolean cancelOnOutOfBoundsRelease) {
		this.cancelOnOutOfBoundsRelease = cancelOnOutOfBoundsRelease;
	}

	public void setEnabled(boolean enabled) {
		if (enabled != isEnabled()) {
			if (enabled) {
				addMouseListener(mouse);
				addMouseMotionListener(mouse);
			} else {
				removeMouseListener(mouse);
				removeMouseMotionListener(mouse);
			}
			super.setEnabled(enabled);
		}
	}

	public int getValue() {
		return value;
	}

	public void setValue(int newValue) {
		int old = value;
		value = Math.max(0, Math.min(max, newValue));
		if (old != value) {
			repaint();
			firePropertyChange(VALUE, old, value);
		}
	}

	public void setMax(int max) {
		this.max = Math.max(1, max);
		repaint();
	}

	public int getMax() {
		return max;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(max, 16); // Scales horizontally with `max`, 16px high
	}

	@Override
	protected void paintComponent(Graphics g) {
		var g2 = (Graphics2D) g.create();
		try {
			var w = getWidth();
			var h = getHeight();

			if (isOpaque()) {
				g2.setColor(getBackground());
				g2.fillRect(0, 0, w, h);
			}

			var barWidth = calcBarWidth(w);
			var fg = getForeground();
			if(!isEnabled()) {
				fg = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 25);
			}
			g2.setColor(fg);
			g2.fillRect(0, 0, barWidth, h);
		} finally {
			g2.dispose();
		}
	}

	private void updateValueFromMouse(int x) {
		int newValue = calcNewValue(x);
		if (newValue != value) {
			value = newValue;
			repaint();
			firePropertyChange(VALUE, null, value);
		}
	}

	private int calcBarWidth(int w) {
	    if (isLogarithmic()) {
	        double logMin = Math.log(1);
	        double logMax = Math.log(max);
	        double logValue = Math.log(Math.max(1, value)); // prevent log(0)
	        return (int) ((logValue - logMin) / (logMax - logMin) * w);
	    } else {
	        return (int) ((value / (double) max) * w);
	    }
	}

	public int calcNewValue(int x) {
		return calcNewValue(x, isLogarithmic(), max, getWidth());
	}

	public static int calcNewValue(int x, boolean logarithmic, int max, int width) {
	    int newValue;
	    if (logarithmic) {
	        double logMin = Math.log(1);
	        double logMax = Math.log(max);
	        double ratio = Math.max(0.0, Math.min(1.0, x / (double) width));
	        double logValue = logMin + ratio * (logMax - logMin);
	        newValue = (int) Math.round(Math.exp(logValue));
	    } else {
	        newValue = (int) ((x / (double) width) * max);
	    }
	    newValue = Math.max(1, Math.min(max, newValue)); // clamp to avoid 0
	    return newValue;
	}

}
