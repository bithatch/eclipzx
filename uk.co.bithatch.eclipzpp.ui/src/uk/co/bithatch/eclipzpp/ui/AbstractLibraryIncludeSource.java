package uk.co.bithatch.eclipzpp.ui;

import static uk.co.bithatch.eclipzpp.ResourceResolver.makePlatformURI;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.eclipzpp.PPIncludeSource;

public abstract class AbstractLibraryIncludeSource implements PPIncludeSource {

	@Override
	public Set<URI> importUris(Resource resource) {
		var resUri = resource.getURI().toString();
		if (resUri.startsWith("platform:/resource/")) {
			var file = ResourcesPlugin.getWorkspace().getRoot().findMember(resUri.substring(19));
			if (file instanceof IFile ifile) {
				return LanguageSystem.languageSystem(file).findImportUris(ifile, IResource.DEPTH_INFINITE).stream()
						.map(f -> makePlatformURI(ifile, f)).filter(f -> f != null)
						.collect(Collectors.toSet());
			}
		}
		return Collections.emptySet();
	}

}
