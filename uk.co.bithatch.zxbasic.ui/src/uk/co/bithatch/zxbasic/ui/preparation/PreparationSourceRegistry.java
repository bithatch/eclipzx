package uk.co.bithatch.zxbasic.ui.preparation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Platform;

public class PreparationSourceRegistry {

	public static final String EXTENSION_POINT_ID = "uk.co.bithatch.zxbasic.preparationSource";

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

//	public static Optional<IPreparationTarget> targetFor(ILaunchConfiguration configuration) throws CoreException {
//		try {
//			return descriptorFor(configuration).map(t -> {
//				try {
//					return t.createTarget();
//				} catch (CoreException e) {
//					throw new IllegalStateException(e);
//				}
//			});
//		} catch (IllegalStateException ise) {
//			if (ise.getCause() instanceof CoreException ce)
//				throw ce;
//			else
//				throw ise;
//		}
//	}
//
//	public static Optional<PreparationTargetDescriptor> descriptorFor(ILaunchConfiguration configuration)
//			throws CoreException {
//		var id = configuration.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_TARGET, "");
//		if (id.equals(""))
//			return Optional.empty();
//		else
//			return descriptors().stream().filter(s -> s.id().equals(id)).findFirst();
//	}

}
