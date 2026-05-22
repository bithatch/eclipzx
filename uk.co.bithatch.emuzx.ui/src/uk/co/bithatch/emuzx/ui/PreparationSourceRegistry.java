package uk.co.bithatch.emuzx.ui;

import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_SOURCE_IDS;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;

public class PreparationSourceRegistry {

	public static final String EXTENSION_POINT_ID = "uk.co.bithatch.emuzx.preparationSource";

	public static List<PreparationSourceDescriptor> descriptors() {
		var registry = Platform.getExtensionRegistry();
		var point = registry.getExtensionPoint(EXTENSION_POINT_ID);
		if (point == null)
			return Collections.emptyList();
		else
			return Arrays.asList(point.getConfigurationElements()).stream()
					.filter(c -> c.getName().equals("preparationSource") && c.getAttribute("id") != null)
					.map(c -> new PreparationSourceDescriptor(c, point))
					.sorted()
					.toList();
	}

	public static List<String> getSourceIds(ILaunchConfiguration configuration) throws CoreException {
		var sourceIds = Arrays.asList(configuration.getAttribute(PREPARATION_SOURCE_IDS, defaultDescriptors()).split(";")).
				stream().
				filter(s -> !s.equals("")).
				toList();
		return sourceIds;
	}

	public static String defaultDescriptors() {
		return String.join(";", PreparationSourceRegistry.descriptors().
				stream().
				filter(d -> d.selected()).
				map(d -> d.id()).
				toList());
	}

}
