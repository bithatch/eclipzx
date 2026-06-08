package uk.co.bithatch.eclipzpp.scoping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;

import com.google.inject.Inject;

import uk.co.bithatch.eclipzpp.PPIncludeSource;

public class PPGlobalScopeProvider extends ImportUriGlobalScopeProvider {

	@Inject(optional = true)
	private PPIncludeSource includeSource;

	@Override
	public LinkedHashSet<URI> getImportedUris(Resource resource) {
		LinkedHashSet<URI> impUris = new LinkedHashSet<>(super.getImportedUris(resource));
		if (includeSource != null) {
			for (String dirUriStr : includeSource.find(resource)) {
				/* The include source returns directory URIs (include paths).
				 * Xtext needs individual file URIs with a registered service
				 * provider, so we enumerate files within each directory and
				 * only add those that have a registered provider (any extension
				 * is fine — .asm, .inc, .z80, etc.). */
				try {
					Path dir = toPath(dirUriStr);
					if (dir != null && Files.isDirectory(dir)) {
						try (Stream<Path> files = Files.list(dir)) {
							files.filter(Files::isRegularFile)
								.forEach(f -> {
									URI fileUri = URI.createFileURI(f.toAbsolutePath().toString());
									if (IResourceServiceProvider.Registry.INSTANCE
											.getResourceServiceProvider(fileUri) != null) {
										impUris.add(fileUri);
									}
								});
						}
					}
				} catch (IOException e) {
					// skip directories that cannot be listed
				}
			}
		}
		return impUris;
	}

	private static Path toPath(String uriStr) {
		try {
			if (uriStr.startsWith("file:")) {
				return Paths.get(java.net.URI.create(uriStr));
			}
			return Paths.get(uriStr);
		} catch (Exception e) {
			return null;
		}
	}
}