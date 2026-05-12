package uk.co.bithatch.emuzx;

import java.util.Optional;

import org.eclipse.core.resources.IFile;

import uk.co.bithatch.bitzx.IProgramBuildOptions;
import uk.co.bithatch.emuzx.api.IProgramBuildOptionsFactory;
import uk.co.bithatch.emuzx.api.IResourceProperties;

public class WorkspaceFileProgramBuildOptionsFactory implements IProgramBuildOptionsFactory {

	@Override
	public Optional<IProgramBuildOptions> buildOptionsFor(IFile file) {
		return Optional.of(new IProgramBuildOptions.Builder().
				withBuild(IResourceProperties.getProperty(file, IResourceProperties.BUILD, true)).
				withOrgAddress(IResourceProperties.getProperty(file, IResourceProperties.ORG_ADDRESS, 0)).
				withHeapAddress(IResourceProperties.getProperty(file, IResourceProperties.HEAP_ADDRESS, 0)).
				withHeapSize(IResourceProperties.getProperty(file, IResourceProperties.HEAP_SIZE, 0)).
				build());
	}

}
