package uk.co.bithatch.eclipz80.scoping;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;

import com.google.inject.Inject;

import uk.co.bithatch.eclipz80.IAsmIncludeSource;

public class AsmGlobalScopeProvider extends ImportUriGlobalScopeProvider {

	@Inject(optional = true)
	private IAsmIncludeSource includeSource;

	@Override
	public LinkedHashSet<URI> getImportedUris(Resource resource) {
		LinkedHashSet<URI> impUris = new LinkedHashSet<>(super.getImportedUris(resource));
		if (includeSource != null) {
			impUris.addAll(includeSource.importUris(resource));
		}
		
		System.out.println("ZZZZZ Imported URIS are : " + String.join(", ", impUris.stream().map(URI::toString).toList()));
		
		return impUris;
	}
}