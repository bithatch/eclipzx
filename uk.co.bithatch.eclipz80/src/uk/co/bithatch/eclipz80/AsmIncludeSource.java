package uk.co.bithatch.eclipz80;

import java.util.Set;

import org.eclipse.emf.ecore.resource.Resource;

import uk.co.bithatch.eclipzpp.PPIncludeSource;

public interface AsmIncludeSource extends PPIncludeSource {
	Set<String> find(Resource resource);
}
