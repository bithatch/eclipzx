package uk.co.bithatch.zxbasic.ui.api;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import uk.co.bithatch.zxbasic.ui.builder.WorkspaceFileProgramBuildOptionsFactory;

public interface IProgramBuildOptionsFactory {

	Optional<IProgramBuildOptions> buildOptionsFor(IFile file);

	public static IProgramBuildOptions accumulate(IFile file) {
		var bldr = new IProgramBuildOptions.Builder();

		for (var factory : factories()) {
			factory.buildOptionsFor(file).ifPresent(opts -> {
				opts.org().ifPresent(bldr::withOrgAddress);
				opts.heap().ifPresent(bldr::withHeapAddress);
				if(!opts.build())
					bldr.withoutBuild();
			});
			;
		}

		return bldr.build();
	}

	
	public static List<IProgramBuildOptionsFactory> factories() {
		
        var registry = Platform.getExtensionRegistry();
        var point = registry.getExtensionPoint("uk.co.bithatch.zxbasic.programBuildsOptionsFactory");
        
        return Stream.concat(Stream.of(new WorkspaceFileProgramBuildOptionsFactory()), Arrays.asList(point.getConfigurationElements()).stream().map(cfg -> {
        	try {
				return (IProgramBuildOptionsFactory) cfg.createExecutableExtension("class");
			} catch (CoreException e) {
				throw new IllegalStateException(e);
			}
        })).toList();
	}
	
}
