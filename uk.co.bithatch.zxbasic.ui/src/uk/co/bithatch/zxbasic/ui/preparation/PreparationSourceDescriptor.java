package uk.co.bithatch.zxbasic.ui.preparation;

import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;
import uk.co.bithatch.zxbasic.ui.api.IPreparationSource;
import uk.co.bithatch.zxbasic.ui.api.IPreparationSourceUI;

public class PreparationSourceDescriptor implements Comparable<PreparationSourceDescriptor> {

	private final String id;
	private final String name;
	private final String pluginId;
	private final IConfigurationElement configElement;
	private final int order;
	private final boolean selected;
	private final String description;

	PreparationSourceDescriptor(IConfigurationElement configElement, IExtensionPoint ext) {
		this.configElement = configElement;

		id = configElement.getAttribute("id");
		var name = configElement.getAttribute("name");
		var icon = configElement.getAttribute("icon");
		var description = configElement.getAttribute("description");
		var pluginId = Optional.ofNullable(configElement.getAttribute("plugin")).orElse(ext.getContributor().getName());
		if (pluginId == null) {
			pluginId = ext.getContributor().getName();
			if (pluginId == null) {
				pluginId = ZXBasicUiActivator.PLUGIN_ID;
			}
		}

		var orderStr  = configElement.getAttribute("order");
		order = orderStr == null || orderStr.equals("") ? 10000 : Integer.parseInt(orderStr); 

		if (icon == null) {
			icon = "icons/preparation16.png";
		}
		
		this.description= description == null ? "" : description;
		this.name = name == null || name.equals("") ? id : name;
		this.pluginId = pluginId;
		this.selected = "true".equalsIgnoreCase(configElement.getAttribute("selected"));
	}
	
	public boolean selected() {
		return selected;
	}

	public Optional<IPreparationSourceUI> createSourceUI() throws CoreException {
		String className = configElement.getAttribute("uiClass");
		if (className == null)
			return Optional.empty();
		return Optional.of((IPreparationSourceUI) configElement.createExecutableExtension("uiClass"));
	}

	public IPreparationSource createSource() throws CoreException {
		return (IPreparationSource) configElement.createExecutableExtension("class");
	}

	public int order() {
		return order;
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

	public String description() {
		return description;
	}

	@Override
	public int compareTo(PreparationSourceDescriptor o) {
		return Integer.valueOf(order).compareTo(o.order);
	}
}
