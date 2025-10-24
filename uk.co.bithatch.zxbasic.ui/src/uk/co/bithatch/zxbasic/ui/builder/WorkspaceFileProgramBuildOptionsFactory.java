package uk.co.bithatch.zxbasic.ui.builder;

import java.util.Optional;

import org.eclipse.core.resources.IFile;

import uk.co.bithatch.zxbasic.ui.api.IProgramBuildOptions;
import uk.co.bithatch.zxbasic.ui.api.IProgramBuildOptionsFactory;

public class WorkspaceFileProgramBuildOptionsFactory implements IProgramBuildOptionsFactory {

	@Override
	public Optional<IProgramBuildOptions> buildOptionsFor(IFile file) {
		return Optional.of(new IProgramBuildOptions.Builder().
				withBuild(ResourceProperties.getProperty(file, ResourceProperties.BUILD, true)).
				withOrgAddress(ResourceProperties.getProperty(file, ResourceProperties.ORG_ADDRESS, 0)).
				withHeapAddress(ResourceProperties.getProperty(file, ResourceProperties.HEAP_ADDRESS, 0)).
				withHeapSize(ResourceProperties.getProperty(file, ResourceProperties.HEAP_SIZE, 0)).
				build());
	}

}
