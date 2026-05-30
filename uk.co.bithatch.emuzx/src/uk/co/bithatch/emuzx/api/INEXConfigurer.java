package uk.co.bithatch.emuzx.api;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import uk.co.bithatch.bitzx.INEXConfiguration;

public interface INEXConfigurer {

	void configure(IFile file, INEXConfiguration nexConfiguration) throws CoreException;

	public static void accumulate(IFile file, INEXConfiguration nexConfiguration) throws CoreException {
		for (var configurer : configurers()) {
			configurer.configure(file, nexConfiguration);
		}
	}
	
	public static List<INEXConfigurer> configurers() {
        var registry = Platform.getExtensionRegistry();
        var point = registry.getExtensionPoint("uk.co.bithatch.emuzx.nexConfigurer");
        return Arrays.asList(point.getConfigurationElements()).stream().map(cfg -> {
        	try {
				return (INEXConfigurer) cfg.createExecutableExtension("class");
			} catch (CoreException e) {
				throw new IllegalStateException(e);
			}
        }).toList();
	}

}
