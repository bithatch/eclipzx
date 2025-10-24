package uk.co.bithatch.jspeccy.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.part.ViewPart;

import jakarta.inject.Inject;
import uk.co.bithatch.jspeccy.Activator;

public class KeyboardView extends ViewPart {

	private Image originalImage;
	private Image scaledImage;
	private Canvas canvas;

	public static final String ID = "uk.co.bithatch.zxbasic.ui.views.KeyboardView";

	@Inject
	IWorkbench workbench;

	@Override
	public void createPartControl(Composite parent) {
		canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		canvas.addPaintListener(e -> {
			e.gc.setBackground(new Color(27,27,27));
			int height = canvas.getClientArea().height;
			e.gc.fillRectangle(0, 0, canvas.getClientArea().width, height);
			if (scaledImage != null) {
				e.gc.drawImage(scaledImage, 0, ( height - (scaledImage.getBounds().height)) / 2);
			}
		});

		originalImage = Activator.getDefault().getImageRegistry().getDescriptor(Activator.KEYBOARD_PATH)
				.createImage();

		canvas.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				resizeImage(canvas.getClientArea());
				canvas.redraw();
			}
		});
	}

	@Override
	public void dispose() {
		super.dispose();
		if (originalImage != null && !originalImage.isDisposed())
			originalImage.dispose();
		if (scaledImage != null && !scaledImage.isDisposed())
			scaledImage.dispose();
	}

	@Override
	public void setFocus() {
		canvas.setFocus();
	}

	private void resizeImage(Rectangle area) {
		if (originalImage == null || originalImage.isDisposed())
			return;

		if (scaledImage != null && !scaledImage.isDisposed()) {
			scaledImage.dispose();
		}

		var imgBounds = originalImage.getBounds();

		var originalAspect = (float) imgBounds.width / imgBounds.height;
		var targetAspect = (float) area.width / area.height;

		int newWidth, newHeight;
		if (originalAspect > targetAspect) {
			newWidth = area.width;
			newHeight = (int) (newWidth / originalAspect);
		} else {
			newHeight = area.height;
			newWidth = (int) (newHeight * originalAspect);
		}

		scaledImage = new Image(canvas.getDisplay(), originalImage.getImageData().scaledTo(newWidth, newHeight));
	}
}
