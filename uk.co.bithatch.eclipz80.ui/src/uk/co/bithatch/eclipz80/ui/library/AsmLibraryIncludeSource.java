package uk.co.bithatch.eclipz80.ui.library;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IResourceServiceProvider;

import uk.co.bithatch.bitzx.ILanguageSystemProvider;
import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.eclipz80.IAsmIncludeSource;
import uk.co.bithatch.eclipz80.ui.preferences.AsmPreferencesAccess;

public class AsmLibraryIncludeSource implements IAsmIncludeSource {

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

	@Override
	public Set<URI> importUris(Resource resource) {
		var resUri = resource.getURI().toString();
		if (resUri.startsWith("platform:/resource/")) {
			var file = ResourcesPlugin.getWorkspace().getRoot().findMember(resUri.substring(19));
			if (file instanceof IFile ifile) {
				ILanguageSystemProvider lang = LanguageSystem.languageSystem(file);
				/* The include source returns directory URIs (include paths).
				 * Xtext needs individual file URIs with a registered service
				 * provider, so we enumerate files within each directory and
				 * only add those that have a registered provider (any extension
				 * is fine — .asm, .inc, .z80, etc.). */
				return lang.findImportUris(ifile, IResource.DEPTH_INFINITE).stream().map(f -> {
					URI fileUri = URI.createFileURI(f.toAbsolutePath().toString());
					if (IResourceServiceProvider.Registry.INSTANCE
							.getResourceServiceProvider(fileUri) != null) {
						return fileUri;
					}
					else {
						return null;
					}
				}).filter(f -> f != null).collect(Collectors.toSet());
			}
		}
		return Collections.emptySet();
	}
}
