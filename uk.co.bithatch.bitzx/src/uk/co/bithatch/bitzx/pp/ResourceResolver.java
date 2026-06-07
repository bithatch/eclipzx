package uk.co.bithatch.bitzx.pp;

public interface ResourceResolver<CONTEXT> {
	IncludeContext<CONTEXT> resolve(ResolveType type, CONTEXT context, String name);
}