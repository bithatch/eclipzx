package uk.co.bithatch.emuzx.api;

import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

public class EmulatorDescriptor {
	
	private final Pattern pattern;
	private final String icon;
	private final String[] locations;
	private final String name;
	private final String website;
	private final String author;
	private final IConfigurationElement configElement;
	private final String id;
	private final String pluginId;
	private final String executable;

	public EmulatorDescriptor(IConfigurationElement configElement, IExtensionPoint ext) {
		this.configElement = configElement;

		id = configElement.getAttribute("id");
		var name = configElement.getAttribute("name");
		var icon = configElement.getAttribute("icon");
		var pluginId = Optional.ofNullable(configElement.getAttribute("plugin")).orElse(null);
		if (pluginId == null) {
			var ctrb = configElement.getContributor();
			pluginId = ctrb.getName();
		}

		if (icon == null) {
			icon = "icons/preparation16.png";
		}
		var executable = configElement.getAttribute("executable");
		if(executable == null) {
			executable = name;
		}
		var pattern = configElement.getAttribute("pattern");
		if(pattern == null) {
			pattern = "(.*)/" + name.toLowerCase() + "(\\.?.*)";
		}
		var website = configElement.getAttribute("website");
		if(website == null) {
			website = "";
		}
		var author = configElement.getAttribute("author");
		if(author == null) {
			author = "";
		}
		var locations = configElement.getAttribute("locations");
		
		this.name = name == null || name.equals("") ? id : name;
		this.icon = icon;
		this.executable = executable;
		this.pluginId = pluginId;
		this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		this.website = website;
		this.author = author;
		this.locations = locations == null ? new String[0] : locations.split(",");
	}

	public Optional<IEmulator> createEmulator() throws CoreException {
		String className = configElement.getAttribute("class");
		if (className == null)
			return Optional.empty();
		return Optional.of((IEmulator) configElement.createExecutableExtension("class"));
	}

	public String home(String path) {
		var mtchr = pattern.matcher(path);
		if(mtchr.find())
			return mtchr.group(1);
		else
			return null;
	}

	public String executable(String path) {
		var mtchr = pattern.matcher(path);
		if(mtchr.find())
		return mtchr.group(2);
		else
			return null;
	}

	public String extension(String path) {
		var mtchr = pattern.matcher(path);
		if(mtchr.find())
			return mtchr.group(3);
		else
			return null;
	}
	
	public boolean matches(String path) {
		return pattern.matcher(path).matches();
	}
	
	public String[] getLocations() {
		return locations;
	}
	
	public String getPluginId() {
		return pluginId;
	}
	
	public String getIcon() {
		return icon;
	}
	
	public String getName() {
		return name;
	}
	
	public String getWebsite() {
		return website;
	}
	
	public String getIdOrDefault(String activatorId) {
		return id;
	}
	
	public String getAuthor() {
		return author;
	}

	public String getExecutable() {
		return executable;
	}
}
