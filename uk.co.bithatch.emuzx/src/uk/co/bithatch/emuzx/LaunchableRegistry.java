package uk.co.bithatch.emuzx;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import uk.co.bithatch.emuzx.api.IExternallyLaunchable;
import uk.co.bithatch.emuzx.api.IInternallyLaunchable;
import uk.co.bithatch.emuzx.api.ILaunchable;

public class LaunchableRegistry {

	public static final String EXTERNAL_XTENSION_POINT_ID = "uk.co.bithatch.emuzx.ui.externallyLaunchable";
	public static final String INTERNAL_XTENSION_POINT_ID = "uk.co.bithatch.emuzx.ui.internallyLaunchable";

	public static <L extends ILaunchable> List<AbstractLaunchableDescriptor<L>> descriptors(Class<L> clazz) {
		var registry = Platform.getExtensionRegistry();
		var point = registry.getExtensionPoint(idForClass(clazz));
		if (point == null)
			return Collections.emptyList();
		else {
			var tagForClass = tagForClass(clazz);
			var els = Arrays.asList(point.getConfigurationElements());
			return els.stream()
					.filter(c -> c.getName().equals(tagForClass) && c.getAttribute("id") != null)
					.map(c -> (AbstractLaunchableDescriptor<L>)descriptorForClass(clazz, point, c))
					.toList();
		}
	}

	@SuppressWarnings("unchecked")
	public static <D  extends AbstractLaunchableDescriptor<L>, L extends ILaunchable> D descriptorForClass(Class<L> clazz, IExtensionPoint point, IConfigurationElement c) {
		if(clazz.equals(IExternallyLaunchable.class))
			return (D)new ExternallyLaunchableDescriptor(c, point);
		else if(clazz.equals(IInternallyLaunchable.class))
			return (D)new InternallyLaunchableDescriptor(c, point);
		else
			throw new IllegalArgumentException("Unsupported launchable type.");
	}
	
	private static String tagForClass(Class<? extends ILaunchable> clazz) {
		if(IExternallyLaunchable.class.isAssignableFrom(clazz))
			return "externallyLaunchable";
		else if(IInternallyLaunchable.class.isAssignableFrom(clazz))
			return "internallyLaunchable";
		else
			throw new IllegalArgumentException("Unsupported launchable type.");
	}
	
	private static String idForClass(Class<? extends ILaunchable> clazz) {
		if(IExternallyLaunchable.class.isAssignableFrom(clazz))
			return EXTERNAL_XTENSION_POINT_ID;
		else if(IInternallyLaunchable.class.isAssignableFrom(clazz))
			return INTERNAL_XTENSION_POINT_ID;
		else
			throw new IllegalArgumentException("Unsupported launchable type.");
	}

	public static <L extends ILaunchable> L launchableFor(Class<L> clazz, IFile file) throws CoreException {
		return descriptorFor(clazz, file).createLaunchable();
	}
	
	public static <L extends ILaunchable> AbstractLaunchableDescriptor<L> descriptorFor(Class<L> clazz, IFile file) throws CoreException {
		return descriptors(clazz).stream().filter(d -> {
			var ext = file.getFileExtension();
			return ext != null && d.extensions().contains(ext);
		}).findFirst().orElseThrow(() -> new IllegalArgumentException("No launchable language found for file " + file.getFullPath() + " (extension " + file.getFileExtension() + ")"));
	}

}
