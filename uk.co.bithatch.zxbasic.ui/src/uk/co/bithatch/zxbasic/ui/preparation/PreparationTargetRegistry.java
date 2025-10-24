package uk.co.bithatch.zxbasic.ui.preparation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;

import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.zxbasic.ui.api.IPreparationTarget;

public class PreparationTargetRegistry {

	public static final String EXTENSION_POINT_ID = "uk.co.bithatch.zxbasic.preparationTarget";

	public static List<PreparationTargetDescriptor> descriptors() {
		var registry = Platform.getExtensionRegistry();
		var point = registry.getExtensionPoint(EXTENSION_POINT_ID);
		if (point == null)
			return Collections.emptyList();
		else
			return Arrays.asList(point.getConfigurationElements()).stream()
					.filter(c -> c.getName().equals("preparationTarget") && c.getAttribute("id") != null)
					.map(c -> new PreparationTargetDescriptor(c, point)).toList();
	}

	public static Optional<IPreparationTarget> targetFor(ILaunchConfiguration configuration) throws CoreException {
		try {
			return descriptorFor(configuration).map(t -> {
				try {
					return t.createTarget();
				} catch (CoreException e) {
					throw new IllegalStateException(e);
				}
			});
		} catch (IllegalStateException ise) {
			if (ise.getCause() instanceof CoreException ce)
				throw ce;
			else
				throw ise;
		}
	}

	public static Optional<PreparationTargetDescriptor> descriptorFor(ILaunchConfiguration configuration)
			throws CoreException {
		var id = configuration.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_TARGET, "");
		if (id.equals(""))
			return Optional.empty();
		else
			return descriptors().stream().filter(s -> s.id().equals(id)).findFirst();
	}

}
