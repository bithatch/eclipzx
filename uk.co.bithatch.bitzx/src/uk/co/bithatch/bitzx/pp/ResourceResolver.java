package uk.co.bithatch.bitzx.pp;

import uk.co.bithatch.bitzx.pp.GenericPreprocessor.PreProcessorConfiguration;

public interface ResourceResolver<CONTEXT> {
	IncludeContext<CONTEXT> resolve(ResolveType type, CONTEXT context, String name, PreProcessorConfiguration config);
}