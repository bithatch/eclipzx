package uk.co.bithatch.eclipz80.scoping;

import java.util.LinkedHashSet;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.inject.Inject;

import uk.co.bithatch.eclipz80.AsmIncludeSource;

public class AsmGlobalScopeProvider extends ImportUriGlobalScopeProvider {

	@Inject(optional = true)
	private AsmIncludeSource includeSource;

	@Inject
	private IResourceScopeCache cache;

	@Override
	public LinkedHashSet<URI> getImportedUris(Resource resource) {
		return cache.get(AsmGlobalScopeProvider.class.getSimpleName(), resource, () -> {
			return doGetImportUris(resource);
		});

		// TEMP
//		return doGetImportUris(resource);
		
	}

	public LinkedHashSet<URI> doGetImportUris(Resource resource) {
		LinkedHashSet<URI> impUris = new LinkedHashSet<>(super.getImportedUris(resource));
		if (includeSource != null) {
			impUris.addAll(includeSource.importUris(resource));
		}
		System.out.println("ZZZZZ All imports ...");
		impUris.forEach(uri -> {
			System.out.println("     Imp uri: " +uri);
		});
		
		return impUris;
	}
}