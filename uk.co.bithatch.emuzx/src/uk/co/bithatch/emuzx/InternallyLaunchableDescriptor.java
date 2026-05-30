package uk.co.bithatch.emuzx;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

import uk.co.bithatch.emuzx.api.IInternallyLaunchable;

public class InternallyLaunchableDescriptor extends AbstractLaunchableDescriptor<IInternallyLaunchable> {

	InternallyLaunchableDescriptor(IConfigurationElement configElement, IExtensionPoint ext) {
		super(configElement, ext);
	}
}
