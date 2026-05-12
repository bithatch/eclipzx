package uk.co.bithatch.emuzx;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import uk.co.bithatch.emuzx.api.IExternallyLaunchable;

public class ExternallyLaunchableRegistry {

	public static final String EXTENSION_POINT_ID = "uk.co.bithatch.emuzx.ui.externallyLaunchable";

	public static List<ExternallyLaunchableDescriptor> descriptors() {
		var registry = Platform.getExtensionRegistry();
		var point = registry.getExtensionPoint(EXTENSION_POINT_ID);
		if (point == null)
			return Collections.emptyList();
		else
			return Arrays.asList(point.getConfigurationElements()).stream()
					.filter(c -> c.getName().equals("externallyLaunchable") && c.getAttribute("id") != null)
					.map(c -> new ExternallyLaunchableDescriptor(c, point))
					.toList();
	}

	public static IExternallyLaunchable externallyLaunchableFor(IFile file) throws CoreException {
		return descriptorFor(file).createExternallyLaunchable();
	}
	
	public static ExternallyLaunchableDescriptor descriptorFor(IFile file) throws CoreException {
		return descriptors().stream().filter(d -> {
			var ext = file.getFileExtension();
			return ext != null && d.extensions().contains(ext);
		}).findFirst().orElseThrow(() -> new IllegalArgumentException("No externally launchable language found for file " + file.getFullPath() + " (extension " + file.getFileExtension() + ")"));
	}

}
