package uk.co.bithatch.zxbasic.ui.preparation;

import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;
import uk.co.bithatch.zxbasic.ui.api.IPreparationTarget;
import uk.co.bithatch.zxbasic.ui.api.IPreparationTargetUI;

public class PreparationTargetDescriptor {

	private final String id;
	private final String name;
	private final String icon;
	private final String pluginId;
	private final IConfigurationElement configElement;

	PreparationTargetDescriptor(IConfigurationElement configElement, IExtensionPoint ext) {
		this.configElement = configElement;

		id = configElement.getAttribute("id");
		var name = configElement.getAttribute("name");
		var icon = configElement.getAttribute("icon");
		var pluginId = Optional.ofNullable(configElement.getAttribute("plugin")).orElse(ext.getContributor().getName());
		if (pluginId == null) {
			pluginId = ext.getContributor().getName();
			if (pluginId == null) {
				pluginId = ZXBasicUiActivator.PLUGIN_ID;
			}
		}

		if (icon == null) {
			icon = "icons/preparation16.png";
		}
		this.name = name == null || name.equals("") ? id : name;
		this.icon = icon;
		this.pluginId = pluginId;
	}

	public Optional<IPreparationTargetUI> createTargetUI() throws CoreException {
		String className = configElement.getAttribute("uiClass");
		if (className == null)
			return Optional.empty();
		return Optional.of((IPreparationTargetUI) configElement.createExecutableExtension("uiClass"));
	}

	public IPreparationTarget createTarget() throws CoreException {
		return (IPreparationTarget) configElement.createExecutableExtension("class");
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
