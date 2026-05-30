package uk.co.bithatch.emuzx;

import java.util.Optional;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

import uk.co.bithatch.emuzx.api.ILaunchable;

public abstract class AbstractLaunchableDescriptor<L extends ILaunchable> {

	private final String id;
	private final String name;
	private final String icon;
	private final String pluginId;
	private final Set<String> extensions;

	protected final IConfigurationElement configElement;

	AbstractLaunchableDescriptor(IConfigurationElement configElement, IExtensionPoint ext) {
		this.configElement = configElement;

		id = configElement.getAttribute("id");
		var name = configElement.getAttribute("name");
		var icon = configElement.getAttribute("icon");
		var pluginId = Optional.ofNullable(configElement.getAttribute("plugin")).orElse(ext.getContributor().getName());
		if (pluginId == null) {
			pluginId = ext.getContributor().getName();
			if (pluginId == null) {
				pluginId = Activator.PLUGIN_ID;
			}
		}

		if (icon == null) {
			icon = "icons/chip16.png";
		}
		this.name = name == null || name.equals("") ? id : name;
		this.icon = icon;
		this.pluginId = pluginId;
		this.extensions = Set.of(configElement.getAttribute("extensions").split(","));
	}

	@SuppressWarnings("unchecked")
	public L createLaunchable() throws CoreException {
		return (L) configElement.createExecutableExtension("class");
	}
	
	public Set<String> extensions() {
		return extensions;
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

	public String icon() {
		return icon;
	}
}
