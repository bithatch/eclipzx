package uk.co.bithatch.eclipzpp;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.resource.Resource;

public interface IMappedResource extends Resource {

	SourceMap map();

	Optional<IFile> getFile();
}
