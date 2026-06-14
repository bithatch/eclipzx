package uk.co.bithatch.eclipz80.ui.library;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.ecore.resource.Resource;

import uk.co.bithatch.bitzx.ILanguageSystemProvider;
import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.eclipz80.AsmIncludeSource;
import uk.co.bithatch.eclipz80.ui.preferences.AsmPreferencesAccess;
import uk.co.bithatch.eclipzpp.ui.AbstractLibraryIncludeSource;

public class AsmLibraryIncludeSource extends AbstractLibraryIncludeSource implements AsmIncludeSource {

	@Override
	public Set<String> find(Resource resource) {
		var resUri = resource.getURI().toString();
		if (resUri.startsWith("platform:/resource/")) {
			var file = ResourcesPlugin.getWorkspace().getRoot().findMember(resUri.substring(19));
			if (file instanceof IFile ifile) {
				ILanguageSystemProvider lang = LanguageSystem.languageSystem(file);
				return lang.findIncludeSourcePaths(ifile, IResource.DEPTH_ONE).stream().map(p -> {
					return AsmPreferencesAccess.resolveWorkspaceRelative(ifile.getProject(), p).toUri().toString();
				}).collect(Collectors.toSet());
			}
		}
		return Collections.emptySet();
	}

	@Override
	@Deprecated
	public Path find(Resource resource, String importURI) {
		var resUri = resource.getURI().toString();
		if (resUri.startsWith("platform:/resource/")) {
			var file = ResourcesPlugin.getWorkspace().getRoot().findMember(resUri.substring(19));
			if (file instanceof IFile ifile) {
				ILanguageSystemProvider lang = LanguageSystem.languageSystem(file);
				for (var ipath : lang.findIncludeSourcePaths(ifile, IResource.DEPTH_ONE)) {
					var path = AsmPreferencesAccess.resolveWorkspaceRelative(ifile.getProject(), ipath);
					var fpath = path.resolve(importURI);
					if (Files.exists(fpath)) {
						return fpath;
					}
				}
			}
		}
		return null;
	}
}
