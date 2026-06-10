package uk.co.bithatch.eclipz80;

import java.nio.file.Path;
import java.util.Set;

import org.eclipse.emf.ecore.resource.Resource;

public interface IAsmIncludeSource {

	Path find(Resource resource, String importURI);

	Set<String> find(Resource resource); 

}
