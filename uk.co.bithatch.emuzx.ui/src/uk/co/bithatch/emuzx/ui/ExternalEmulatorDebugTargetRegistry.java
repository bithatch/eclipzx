package uk.co.bithatch.emuzx.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Platform;

public class ExternalEmulatorDebugTargetRegistry {

	public static final String EXTENSION_POINT_ID = "uk.co.bithatch.emuzx.ui.externalEmulatorDebugTarget";


	public static ExternalEmulatorDebugTargetDescriptor get(String id)  {
		return descriptors().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("No such external emulator debug target: " + id));
		
	}
	
	public static List<ExternalEmulatorDebugTargetDescriptor> descriptors() {
		var registry = Platform.getExtensionRegistry();
		var point = registry.getExtensionPoint(EXTENSION_POINT_ID);
		if (point == null)
			return Collections.emptyList();
		else
			return Arrays.asList(point.getConfigurationElements()).stream()
					.filter(c -> c.getName().equals("externalEmulatorDebugTarget") && c.getAttribute("id") != null)
					.map(c -> new ExternalEmulatorDebugTargetDescriptor(c, point))
					.toList();
	}

}
