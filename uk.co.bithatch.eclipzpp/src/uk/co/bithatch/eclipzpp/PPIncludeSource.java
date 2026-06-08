package uk.co.bithatch.eclipzpp;

import java.nio.file.Path;
import java.util.Set;

import org.eclipse.emf.ecore.resource.Resource;

public interface PPIncludeSource {

	Path find(Resource resource, String importURI);

	Set<String> find(Resource resource);

}
