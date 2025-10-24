package uk.co.bithatch.emuzx;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Platform;

import uk.co.bithatch.emuzx.api.EmulatorDescriptor;

public class EmulatorRegistry {

	public static final String EXTENSION_POINT_ID = "uk.co.bithatch.emuzx.emulator";

	public static List<EmulatorDescriptor> descriptors() {
		var registry = Platform.getExtensionRegistry();
		var point = registry.getExtensionPoint(EXTENSION_POINT_ID);
		if (point == null)
			return Collections.emptyList();
		else
			return Arrays.asList(point.getConfigurationElements()).stream()
					.filter(c -> c.getName().equals("emulator") && c.getAttribute("id") != null)
					.map(c -> new EmulatorDescriptor(c, point)).toList();
	}

}
