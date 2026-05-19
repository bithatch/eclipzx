package uk.co.bithatch.eclipzx.product;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.splash.BasicSplashHandler;

public class SplashHandler extends BasicSplashHandler {

	@Override
	public void init(Shell splash) {
		var progressRect = Platform.getProduct().getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
		var messageRect = Platform.getProduct().getProperty(IProductConstants.STARTUP_MESSAGE_RECT);
		var foregroundColor = Platform.getProduct().getProperty(IProductConstants.STARTUP_FOREGROUND_COLOR);

		if (progressRect != null)
			setProgressRect(parseRect(progressRect));
		if (messageRect != null)
			setMessageRect(parseRect(messageRect));

		super.init(splash);
		if (foregroundColor != null)
			setForeground(parseColor(foregroundColor, splash));
		
		// Force the progress bar to show something immediately for testing
		IProgressMonitor mon = getBundleProgressMonitor();
		if (mon != null) {
			mon.beginTask("Starting EclipZX...", 100);
			mon.worked(10);
		}
		// Force repaint
		splash.layout(true, true);
		splash.update();
	}

	private Rectangle parseRect(String rect) {
		String[] parts = rect.split(",");
		return new Rectangle(
			Integer.parseInt(parts[0].trim()),
			Integer.parseInt(parts[1].trim()),
			Integer.parseInt(parts[2].trim()),
			Integer.parseInt(parts[3].trim()));
	}

	private RGB parseColor(String hex, Shell splash) {
		int r = Integer.parseInt(hex.substring(0, 2), 16);
		int g = Integer.parseInt(hex.substring(2, 4), 16);
		int b = Integer.parseInt(hex.substring(4, 6), 16);
		return new RGB(r, g, b);
	}
}