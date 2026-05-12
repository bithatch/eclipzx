package uk.co.bithatch.emuzx.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

public class ExternalEmulatorLaunchTabFactoryDescriptor {

	private final IConfigurationElement configElement;

	ExternalEmulatorLaunchTabFactoryDescriptor(IConfigurationElement configElement, IExtensionPoint ext) {
		this.configElement = configElement;
	}

	public IExternalEmulatorLaunchTabFactory createTabFactory() throws CoreException {
		return (IExternalEmulatorLaunchTabFactory) configElement.createExecutableExtension("class");
	}
}
