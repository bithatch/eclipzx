package uk.co.bithatch.eclipzpp;

public interface ResourceResolver<CONTEXT> {
	IncludeContext<CONTEXT> resolve(ResolveType type, CONTEXT context, String name);
}