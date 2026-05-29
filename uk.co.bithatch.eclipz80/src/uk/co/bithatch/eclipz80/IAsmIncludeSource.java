package uk.co.bithatch.eclipz80;

import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

public interface IAsmIncludeSource {

	Set<URI> find(Resource resource);

}
