package uk.co.bithatch.eclipz80;

import java.nio.file.Path;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

public interface IAsmIncludeSource {

	Path find(Resource resource, String importURI);

	@Deprecated
	Set<String> find(Resource resource);
	
	Set<URI> importUris(Resource resource);

}
