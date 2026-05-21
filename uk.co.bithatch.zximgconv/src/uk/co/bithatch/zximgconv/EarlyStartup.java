package uk.co.bithatch.zximgconv;

import org.eclipse.ui.IStartup;

/**
 * Forces early activation of this plugin so that the resource change listener
 * in {@link Activator} is installed before any project wizards run.
 * <p>
 * Without this, the plugin would only activate lazily (when a class is first
 * referenced), meaning the nature auto-add listener would not be in place
 * when Z88DK or ZX Basic projects are created.
 */
public class EarlyStartup implements IStartup {

	@Override
	public void earlyStartup() {
		// The Activator.start() method installs the resource change listener.
		// Simply loading this class triggers bundle activation due to
		// Bundle-ActivationPolicy: lazy.
	}
}
