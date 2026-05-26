package uk.co.bithatch.emuzx.ui;

import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

public class ExternalEmulatorDebugTargetDescriptor {

	private final String id;
	private final String name;
	private final String pluginId;
	private final IConfigurationElement configElement;
	private final boolean selected;

	ExternalEmulatorDebugTargetDescriptor(IConfigurationElement configElement, IExtensionPoint ext) {
		this.configElement = configElement;

		id = configElement.getAttribute("id");
		var name = configElement.getAttribute("name");
		var icon = configElement.getAttribute("icon");
		var pluginId = Optional.ofNullable(configElement.getAttribute("plugin")).orElse(ext.getContributor().getName());
		if (pluginId == null) {
			pluginId = ext.getContributor().getName();
			if (pluginId == null) {
				pluginId = EmuZXUIActivator.PLUGIN_ID;
			}
		}

		if (icon == null) {
			icon = "icons/preparation16.png";
		}

		this.name = name == null || name.equals("") ? id : name;
		this.pluginId = pluginId;
		this.selected = "true".equalsIgnoreCase(configElement.getAttribute("selected"));
	}

	public boolean selected() {
		return selected;
	}

	public IExternalEmulatorDebugTargetFactory createDebugTargetFactory() throws CoreException {
		return (IExternalEmulatorDebugTargetFactory) configElement.createExecutableExtension("class");
	}

	public String id() {
		return id;
	}

	public String pluginId() {
		return pluginId;
	}

	public String name() {
		return name;
	}

}
