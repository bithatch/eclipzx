package uk.co.bithatch.eclipzpp;

import java.nio.file.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.IResourceServiceProvider;

public interface ResourceResolver<CONTEXT> {
	IncludeContext<CONTEXT> resolve(ResolveType type, CONTEXT context, String name);

	public static URI makePlatformURI(IFile baseFile, String f) {
		return makePlatformURI(baseFile, java.net.URI.create(f));
	}

	public static URI makePlatformURI(IFile baseFile, Path file) {
		return makePlatformURI(baseFile, file.toUri());
	}

	public static URI makePlatformURI(IFile baseFile, java.net.URI nativeUri) {
		var fileUri = URI.createURI(nativeUri.toString());
		var incfile = baseFile.getWorkspace().getRoot().findFilesForLocationURI(nativeUri);
		if(incfile == null || incfile.length == 0) {
			if (IResourceServiceProvider.Registry.INSTANCE
					.getResourceServiceProvider(fileUri) != null) {
				return fileUri;
			}
			else {
				return null;
			}
		}
		else {
			return URI.createPlatformResourceURI(incfile[0].getFullPath().toString(), true);
		}
	}
}