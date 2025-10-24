package uk.co.bithatch.zxbasic.ui.api;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import uk.co.bithatch.zxbasic.tools.NexConverter;
import uk.co.bithatch.zxbasic.ui.builder.WorkspaceNEXConfigurer;

public interface INEXConfigurer {

	void configure(IFile file, NexConverter.NexConfiguration nexConfiguration) throws CoreException;

	public static void accumulate(IFile file, NexConverter.NexConfiguration nexConfiguration) throws CoreException {
		for (var configurer : configurers()) {
			configurer.configure(file, nexConfiguration);
		}
	}
	
	public static List<INEXConfigurer> configurers() {
        var registry = Platform.getExtensionRegistry();
        var point = registry.getExtensionPoint("uk.co.bithatch.zxbasic.nexConfigurer");
        return Stream.concat(Stream.of(new WorkspaceNEXConfigurer()), Arrays.asList(point.getConfigurationElements()).stream().map(cfg -> {
        	try {
				return (INEXConfigurer) cfg.createExecutableExtension("class");
			} catch (CoreException e) {
				throw new IllegalStateException(e);
			}
        })).toList();
	}

}
