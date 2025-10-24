package uk.co.bithatch.zxbasic.preprocessor;

public interface ResourceResolver<CONTEXT> {
	IncludeContext<CONTEXT> resolve(ResolveType type, CONTEXT context, String name);
}