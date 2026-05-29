package uk.co.bithatch.eclipz80.ui.library;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.eclipz80.IAsmIncludeSource;

public class AsmLibraryIncludeSource implements IAsmIncludeSource {

	@Override
	public Set<URI> find(Resource resource) {
		var resUri = resource.getURI().toString();
		if(resUri.startsWith("platform:/resource/")) {
			var file = ResourcesPlugin.getWorkspace().getRoot().findMember(resUri.substring(19));
			if(file instanceof IFile ifile) {
				return LanguageSystem.languageSystem(file).findIncludeSourcePaths(ifile).
						stream().
						map(p -> URI.createURI(p.toString(), true)).
						collect(Collectors.toSet());
			}
		}
		return Collections.emptySet();
	}

	
}
