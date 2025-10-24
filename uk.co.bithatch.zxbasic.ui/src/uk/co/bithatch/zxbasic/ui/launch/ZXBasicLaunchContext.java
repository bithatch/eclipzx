package uk.co.bithatch.zxbasic.ui.launch;

import org.eclipse.debug.core.ILaunchConfiguration;

public class ZXBasicLaunchContext {
	private static final ThreadLocal<ILaunchConfiguration> currentConfig = new ThreadLocal<>();

	public static void set(ILaunchConfiguration config) {
		currentConfig.set(config);
	}

	public static ILaunchConfiguration get() {
		return currentConfig.get();
	}

	public static void clear() {
		currentConfig.remove();
	}
}
