package uk.co.bithatch.emuzx.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Platform;

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

}
