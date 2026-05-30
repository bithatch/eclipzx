package uk.co.bithatch.emuzx;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

import uk.co.bithatch.emuzx.api.IExternallyLaunchable;

public class ExternallyLaunchableDescriptor extends AbstractLaunchableDescriptor<IExternallyLaunchable> {

	ExternallyLaunchableDescriptor(IConfigurationElement configElement, IExtensionPoint ext) {
		super(configElement, ext);
	}
}
