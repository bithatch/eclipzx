package uk.co.bithatch.bitzx;

import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

public class LanguageSystemProviderDescriptor {

	private final String name;
	private final IConfigurationElement configElement;
	private final String id;
	private final String pluginId;

	public LanguageSystemProviderDescriptor(IConfigurationElement configElement, IExtensionPoint ext) {
		this.configElement = configElement;

		id = configElement.getAttribute("id");
		var name = configElement.getAttribute("name");
		var pluginId = Optional.ofNullable(configElement.getAttribute("plugin")).orElse(null);
		if (pluginId == null) {
			var ctrb = configElement.getContributor();
			pluginId = ctrb.getName();
		}
		this.name = name == null || name.equals("") ? id : name;
		this.pluginId = pluginId;
	}

	public ILanguageSystemProvider createHandler() throws CoreException {
		return (ILanguageSystemProvider) configElement.createExecutableExtension("class");
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

	public String getIdOrDefault(String activatorId) {
		return id;
	}

}
