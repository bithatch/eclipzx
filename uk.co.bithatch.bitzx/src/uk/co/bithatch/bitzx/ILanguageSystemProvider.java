package uk.co.bithatch.bitzx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

public interface ILanguageSystemProvider {

	public static final String ASM_EDITOR_ID = "uk.co.bithatch.eclipz80.Asm";
	
	String getEditorId(IFile file);
	
	ISourceAdressMap createSourceAddressMap(Path file);

	List<? extends IArchitecture> architectures(IResource resource);

	default List<IOutputFormat> outputFormats(IResource resource) {
		var l = new LinkedHashSet<IOutputFormat>();
		for(var arch : architectures(resource)) {
			l.addAll(arch.supportedFormats());
		}
		return l.stream().toList();
	}

	default IArchitecture architectureOrDefault(IProject project, String archName) {
		var archs = architectures(project);
		for (var a : archs) {
			if (a.name().equalsIgnoreCase(archName))
				return a;
		}
		if (!archs.isEmpty()) {
			return archs.get(0);
		}
		throw new IllegalStateException("Could not get any architectures for this project!");
	}
	
	LanguageSystemPreferencesAccess preferenceAccess();

	boolean isCompatible(IResource resource);

	Path prepareForInternalLaunch(IOutputFormat fmt, IFile file, ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException;

	boolean isLaunchable(IResource res);
	
	String[] sourceFileExtensions();

	List<? extends IArchitecture> architectures(IProject project, String sdkName);
	
	Optional<String> findRuntimeDir(IResource baseFile);
	
	Set<String> findIncludeSourcePaths(IResource baseFile, int depth);
	
	Map<String, String> findDefines(IResource baseFile);
	
	default Set<Path> findImportUris(IResource baseFile, int depth) {
		
		var set = new LinkedHashSet<Path>();
		for(var path : findIncludeSourcePaths(baseFile, depth).stream().map(p -> {
			return LanguageSystemPreferencesAccess.resolveWorkspaceRelative(baseFile.getProject(), p).toUri().toString();
		}).collect(Collectors.toSet())) {
			var dir = URIS.toPath(path);
			try {
				set.addAll(Files.list(dir).filter(Files::isRegularFile).collect(Collectors.toSet()));
			} catch (IOException e) {
//					// skip directories that cannot be listed
			}
		}
		return set;
	}
}
