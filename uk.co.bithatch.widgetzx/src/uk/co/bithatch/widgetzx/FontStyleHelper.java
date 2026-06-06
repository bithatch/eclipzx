package uk.co.bithatch.widgetzx;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;

public class FontStyleHelper {
	private Font boldFont;
	private final Control base;

	public FontStyleHelper(Control base) {
		this.base = base;
	}
	
	public <C extends Control> C bold(C control) {
		resolveBold();
		control.setFont(boldFont);
		return control;
	}

	public void dispose() {
		if (boldFont != null && !boldFont.isDisposed()) {
			boldFont.dispose();
		}
	}
	
	private void resolveBold() {
		if(boldFont != null)
			return;
		
		// Create bold font
		var normalFont = base.getFont();
		var fd = normalFont.getFontData();
		for (var f : fd) {
			f.setStyle(f.getStyle() | SWT.BOLD);
		}
		boldFont = new Font(base.getDisplay(), fd);
	}
}
