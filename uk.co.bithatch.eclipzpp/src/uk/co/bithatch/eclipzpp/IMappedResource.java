package uk.co.bithatch.eclipzpp;

import org.eclipse.emf.ecore.resource.Resource;

public interface IMappedResource extends Resource {

	SourceMap map();
}
